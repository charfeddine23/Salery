package com.example.cloud

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object GoogleAuthHelper {
    private const val TAG = "Auth"

    private fun resolveAuth(context: Context): FirebaseAuth? = runCatching {
        val appCtx = context.applicationContext
        if (FirebaseApp.getApps(appCtx).isEmpty()) {
            FirebaseApp.initializeApp(appCtx)
        }
        Firebase.auth
    }.getOrNull()

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager = CredentialManager.create(context),
        scope: CoroutineScope,
        onAuthSuccess: (FirebaseUser) -> Unit,
        onUnauthenticated: () -> Unit
    ) {
        val auth = resolveAuth(context)
        if (auth == null) {
            onUnauthenticated()
            return
        }
        val existing = auth.currentUser
        if (existing != null) {
            onAuthSuccess(existing)
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onUnauthenticated()
            return
        }
        if (clientId.isBlank()) {
            onUnauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val activityOrContext = context.findActivityOrNull() ?: context
                val credential = credentialManager.getCredential(activityOrContext, request).credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val result = auth.signInWithCredential(
                        GoogleAuthProvider.getCredential(token, null)
                    ).await()
                    val user = result.user ?: auth.currentUser
                    if (user != null) onAuthSuccess(user) else onUnauthenticated()
                } else {
                    onUnauthenticated()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager = CredentialManager.create(context),
        scope: CoroutineScope,
        onAuthSuccess: (FirebaseUser) -> Unit,
        onAuthError: (String) -> Unit,
        onAuthCancelled: () -> Unit = {}
    ) {
        val auth = resolveAuth(context)
        if (auth == null) {
            onAuthError("Firebase Authentication is not initialized on this device.")
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }
        if (clientId.isBlank()) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id is empty")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val hostActivity = context.findActivityOrNull()
                val credentialContext: Context = hostActivity ?: context
                val credential = credentialManager.getCredential(credentialContext, request).credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val result = auth.signInWithCredential(
                        GoogleAuthProvider.getCredential(token, null)
                    ).await()
                    val user = result.user ?: auth.currentUser
                    if (user != null) {
                        onAuthSuccess(user)
                    } else {
                        onAuthError("Firebase sign-in returned no authenticated user.")
                    }
                } else {
                    onAuthError("Unexpected credential type returned by Credential Manager.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Google Sign-In failed")
            }
        }
    }

    /**
     * Authenticates an existing Firebase account via Email & Password.
     * Password is passed directly to Firebase Auth and NEVER stored locally or in Firestore.
     */
    suspend fun signInWithEmailPassword(
        context: Context,
        email: String,
        password: String,
        isArabic: Boolean
    ): EmailAuthResult {
        val auth = resolveAuth(context) ?: return EmailAuthResult.Error(
            message = if (isArabic) "تعذر تهيئة خدمة المصادقة السحابية" else "Firebase Authentication is unavailable",
            accountNotFound = false
        )
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: auth.currentUser
            if (user != null) {
                EmailAuthResult.Success(user)
            } else {
                EmailAuthResult.Error(
                    message = if (isArabic) "لم يتم العثور على جلسة مستخدم صالحة" else "No authenticated user returned",
                    accountNotFound = false
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val errorCode = (e as? FirebaseAuthException)?.errorCode.orEmpty()
            val isMissingUser = e is FirebaseAuthInvalidUserException ||
                errorCode == "ERROR_USER_NOT_FOUND"
            val isInvalidCred = errorCode == "ERROR_INVALID_CREDENTIAL" ||
                errorCode == "ERROR_WRONG_PASSWORD"
            EmailAuthResult.Error(
                message = mapFirebaseAuthError(e, isArabic),
                accountNotFound = isMissingUser || isInvalidCred
            )
        }
    }

    /**
     * Creates a new Firebase Auth account with Email & Password and attaches full display name.
     * Password is handled exclusively by Firebase Auth and NEVER stored anywhere else.
     */
    suspend fun signUpWithEmailPassword(
        context: Context,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        isArabic: Boolean
    ): EmailAuthResult {
        val auth = resolveAuth(context) ?: return EmailAuthResult.Error(
            message = if (isArabic) "تعذر تهيئة خدمة المصادقة السحابية" else "Firebase Authentication is unavailable",
            accountNotFound = false
        )
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: auth.currentUser
                ?: return EmailAuthResult.Error(
                    message = if (isArabic) "تعذر إنشاء الحساب السحابي" else "Failed to create account",
                    accountNotFound = false
                )
            val fullName = listOf(firstName.trim(), lastName.trim())
                .filter { it.isNotEmpty() }
                .joinToString(" ")
            if (fullName.isNotEmpty()) {
                runCatching {
                    user.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()
                    ).await()
                }
            }
            EmailAuthResult.Success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            EmailAuthResult.Error(
                message = mapFirebaseAuthError(e, isArabic),
                accountNotFound = false
            )
        }
    }

    private fun mapFirebaseAuthError(e: Exception, isArabic: Boolean): String {
        val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()
        return when (code) {
            "ERROR_USER_NOT_FOUND" -> if (isArabic) {
                "لا يوجد حساب مسجّل بهذا البريد الإلكتروني — يمكنك إنشاء حساب جديد الآن"
            } else {
                "No account found for this email — create a new account now"
            }
            "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD" -> if (isArabic) {
                "بيانات الدخول غير صحيحة أو لا يوجد حساب مسجّل بهذا البريد"
            } else {
                "Invalid email/password or account does not exist yet"
            }
            "ERROR_INVALID_EMAIL" -> if (isArabic) {
                "صيغة البريد الإلكتروني غير صالحة"
            } else {
                "Invalid email address format"
            }
            "ERROR_EMAIL_ALREADY_IN_USE" -> if (isArabic) {
                "هذا البريد الإلكتروني مسجّل مسبقاً — جرّب تسجيل الدخول"
            } else {
                "This email is already registered — try signing in"
            }
            "ERROR_WEAK_PASSWORD" -> if (isArabic) {
                "كلمة السر ضعيفة — استخدم 6 أحرف على الأقل"
            } else {
                "Password is too weak — use at least 6 characters"
            }
            "ERROR_OPERATION_NOT_ALLOWED" -> if (isArabic) {
                "يرجى تفعيل تسجيل الدخول بالبريد وكلمة السر في Firebase Console"
            } else {
                "Please enable Email/Password sign-in provider in Firebase Console"
            }
            "ERROR_TOO_MANY_REQUESTS" -> if (isArabic) {
                "محاولات كثيرة متتالية، انتظر قليلاً ثم أعد المحاولة"
            } else {
                "Too many attempts. Please wait a moment and try again"
            }
            "ERROR_NETWORK_REQUEST_FAILED" -> if (isArabic) {
                "لا يوجد اتصال بالإنترنت حالياً"
            } else {
                "Network connection unavailable"
            }
            else -> e.localizedMessage?.takeIf { it.isNotBlank() }
                ?: if (isArabic) "تعذرت المصادقة السحابية" else "Authentication failed"
        }
    }

    fun signOut(
        context: Context,
        credentialManager: CredentialManager = CredentialManager.create(context),
        scope: CoroutineScope,
        beforeFirebaseSignOut: suspend () -> Unit = {},
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    beforeFirebaseSignOut()
                } catch (e: Exception) {
                    Log.w(TAG, "Pre-sign-out cleanup warning: ${e.message}")
                }
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    resolveAuth(context)?.signOut()
                    onSignOutComplete()
                }
            }
        }
    }

    private tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivityOrNull()
        else -> null
    }
}

sealed interface EmailAuthResult {
    data class Success(val user: FirebaseUser) : EmailAuthResult
    data class Error(
        val message: String,
        val accountNotFound: Boolean
    ) : EmailAuthResult
}
