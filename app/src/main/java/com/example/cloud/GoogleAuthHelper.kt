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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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
