package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.ArabicStrings
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.LiquidGlassTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceContactItem(
    val id: String,
    val displayName: String,
    val phoneNumber: String,
    val contactUri: String
)

@Stable
class ContactPickerFlowState internal constructor(
    hasPermissionInitial: Boolean,
    private val onLaunchPicker: () -> Unit
) {
    var hasPermission by mutableStateOf(hasPermissionInitial)
        internal set
    var permissionDeniedHint by mutableStateOf(false)
        internal set

    fun openPicker() = onLaunchPicker()
}

@Composable
fun rememberContactPickerFlow(
    secureScreenshots: Boolean = false,
    onContactResolved: (name: String, phone: String, uri: String) -> Unit
): ContactPickerFlowState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showInAppContactsPicker by remember { mutableStateOf(false) }
    var launchAction by remember { mutableStateOf({}) }
    val state = remember {
        ContactPickerFlowState(
            hasPermissionInitial = hasReadContactsPermission(context),
            onLaunchPicker = { launchAction() }
        )
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = hasReadContactsPermission(context)
                state.hasPermission = granted
                if (granted) {
                    state.permissionDeniedHint = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { pickedUri: Uri? ->
        pickedUri?.let { uri ->
            val (resolvedName, resolvedPhone, resolvedUriStr) = resolvePickedContactSafely(context, uri)
            if (resolvedName.isBlank() && resolvedPhone.isBlank()) {
                if (hasReadContactsPermission(context)) {
                    showInAppContactsPicker = true
                } else {
                    state.permissionDeniedHint = true
                }
            } else {
                state.permissionDeniedHint = false
                onContactResolved(resolvedName, resolvedPhone, resolvedUriStr)
            }
        }
    }

    fun launchSystemContactPickerWithFallback() {
        try {
            contactPickerLauncher.launch(null)
        } catch (_: Throwable) {
            if (hasReadContactsPermission(context)) {
                showInAppContactsPicker = true
            } else {
                state.permissionDeniedHint = true
            }
        }
    }

    val readContactsPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val granted = isGranted || hasReadContactsPermission(context)
        state.hasPermission = granted
        if (granted) {
            state.permissionDeniedHint = false
            showInAppContactsPicker = true
        } else {
            state.permissionDeniedHint = true
            launchSystemContactPickerWithFallback()
        }
    }

    launchAction = {
        if (hasReadContactsPermission(context)) {
            state.hasPermission = true
            state.permissionDeniedHint = false
            showInAppContactsPicker = true
        } else {
            readContactsPermLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    if (showInAppContactsPicker) {
        InAppContactsPickerDialog(
            secureScreenshots = secureScreenshots,
            onDismiss = { showInAppContactsPicker = false },
            onSelectContact = { item ->
                showInAppContactsPicker = false
                state.permissionDeniedHint = false
                onContactResolved(item.displayName, item.phoneNumber, item.contactUri)
            },
            onOpenSystemPicker = {
                showInAppContactsPicker = false
                launchSystemContactPickerWithFallback()
            }
        )
    }

    return state
}

@Composable
fun InAppContactsPickerDialog(
    secureScreenshots: Boolean = false,
    onDismiss: () -> Unit,
    onSelectContact: (DeviceContactItem) -> Unit,
    onOpenSystemPicker: () -> Unit
) {
    val strings = AppStrings.current
    val colors = LiquidGlassTheme.colors
    val isArabic = strings is ArabicStrings
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var contacts by remember { mutableStateOf<List<DeviceContactItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        isLoading = true
        contacts = withContext(Dispatchers.IO) {
            loadDeviceContactsList(context)
        }
        isLoading = false
    }

    val filtered by remember(contacts, searchQuery) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) contacts
            else contacts.filter {
                it.displayName.lowercase().contains(q) ||
                    it.phoneNumber.lowercase().contains(q)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            securePolicy = if (secureScreenshots) SecureFlagPolicy.SecureOn else SecureFlagPolicy.SecureOff
        )
    ) {
        GlassCard(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .height(500.dp)
                .testTag("in_app_contacts_picker_dialog"),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = colors.dialogBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "اختيار جهة اتصال" else "Select Contact",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.cancel,
                            tint = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.glassCard)
                        .border(1.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (isArabic) "ابحث بالاسم أو رقم الهاتف..." else "Search by name or phone...",
                                color = colors.textMuted,
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 13.5.sp
                            ),
                            cursorBrush = SolidColor(CyanNeon),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("in_app_contacts_search_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                color = CyanNeon,
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center)
                            )
                        }
                        filtered.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isArabic) "لا توجد جهات اتصال مطابقة" else "No matching contacts found",
                                    color = colors.textSecondary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        else -> {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filtered, key = { "${it.id}_${it.phoneNumber}" }) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(colors.glassCard)
                                            .border(0.8.dp, colors.glassBorderSubtle, RoundedCornerShape(14.dp))
                                            .clickable { onSelectContact(item) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(CyanNeon.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item.displayName.firstOrNull()?.uppercase() ?: "#",
                                                color = CyanNeon,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.displayName,
                                                color = colors.textPrimary,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (item.phoneNumber.isNotBlank()) {
                                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                    Text(
                                                        text = item.phoneNumber,
                                                        color = colors.textMuted,
                                                        fontSize = 11.5.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                GlassButton(
                    text = if (isArabic) "الفتح عبر تطبيق جهات الاتصال" else "Open System Contacts App",
                    containerColor = CyanNeon.copy(alpha = 0.18f),
                    contentColor = CyanNeon,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_system_contacts_app_button"),
                    onClick = onOpenSystemPicker
                )
            }
        }
    }
}

fun hasReadContactsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
}

fun openAppContactsPermissionSettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
        if (context.findActivityOrNull() == null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Throwable) {
    }
}

private tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
}

fun resolvePickedContactSafely(context: Context, uri: Uri): Triple<String, String, String> {
    val resolver = context.contentResolver
    val lookupUri = try {
        ContactsContract.Contacts.lookupContact(resolver, uri) ?: uri
    } catch (_: Throwable) {
        uri
    }

    var resolvedName = ""
    var contactId = ""
    var resolvedPhone = ""

    fun readCursor(targetUri: Uri) {
        try {
            resolver.query(targetUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    if (resolvedName.isBlank()) {
                        val nameCols = listOf(
                            ContactsContract.Contacts.DISPLAY_NAME,
                            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                            ContactsContract.Contacts.DISPLAY_NAME_ALTERNATIVE
                        )
                        for (col in nameCols) {
                            val idx = cursor.getColumnIndex(col)
                            if (idx != -1) {
                                val v = cursor.getString(idx)?.trim().orEmpty()
                                if (v.isNotBlank()) {
                                    resolvedName = v
                                    break
                                }
                            }
                        }
                    }
                    if (contactId.isBlank()) {
                        val idCols = listOf(
                            ContactsContract.Contacts._ID,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                        )
                        for (col in idCols) {
                            val idx = cursor.getColumnIndex(col)
                            if (idx != -1) {
                                val v = cursor.getString(idx)?.trim().orEmpty()
                                if (v.isNotBlank()) {
                                    contactId = v
                                    break
                                }
                            }
                        }
                    }
                    if (resolvedPhone.isBlank()) {
                        val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numIdx != -1) {
                            resolvedPhone = cursor.getString(numIdx)?.trim().orEmpty()
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    readCursor(lookupUri)
    if ((resolvedName.isBlank() || resolvedPhone.isBlank()) && lookupUri != uri) {
        readCursor(uri)
    }

    if (hasReadContactsPermission(context) && contactId.isNotBlank()) {
        if (resolvedPhone.isBlank()) {
            try {
                resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { phoneCursor ->
                    while (phoneCursor.moveToNext()) {
                        val numIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numIdx != -1) {
                            val candidate = phoneCursor.getString(numIdx)?.trim().orEmpty()
                            if (candidate.isNotBlank()) {
                                resolvedPhone = candidate
                                break
                            }
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }
        if (resolvedName.isBlank()) {
            try {
                resolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
                    "${ContactsContract.Contacts._ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        if (idx != -1) {
                            resolvedName = c.getString(idx)?.trim().orEmpty()
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    return Triple(resolvedName, resolvedPhone, lookupUri.toString())
}

fun loadDeviceContactsList(context: Context): List<DeviceContactItem> {
    if (!hasReadContactsPermission(context)) return emptyList()
    val resolver = context.contentResolver
    val itemsMap = linkedMapOf<String, DeviceContactItem>()

    try {
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val keyIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)

            while (cursor.moveToNext()) {
                val id = if (idIdx != -1) cursor.getString(idIdx)?.trim().orEmpty() else ""
                val name = if (nameIdx != -1) cursor.getString(nameIdx)?.trim().orEmpty() else ""
                val phone = if (numIdx != -1) cursor.getString(numIdx)?.trim().orEmpty() else ""
                val lookupKey = if (keyIdx != -1) cursor.getString(keyIdx)?.trim().orEmpty() else ""
                if (name.isBlank() && phone.isBlank()) continue

                val uri = if (id.isNotBlank() && lookupKey.isNotBlank()) {
                    ContactsContract.Contacts.getLookupUri(id.toLongOrNull() ?: 0L, lookupKey)?.toString().orEmpty()
                } else if (id.isNotBlank()) {
                    Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id).toString()
                } else {
                    ""
                }

                val mapKey = "${id}_${phone.filter { it.isDigit() }}"
                if (!itemsMap.containsKey(mapKey)) {
                    itemsMap[mapKey] = DeviceContactItem(
                        id = id.ifBlank { mapKey },
                        displayName = name.ifBlank { phone },
                        phoneNumber = phone,
                        contactUri = uri
                    )
                }
            }
        }
    } catch (_: Throwable) {
    }

    val seenContactIds = itemsMap.values.map { it.id }.toSet()
    try {
        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.LOOKUP_KEY
            ),
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val keyIdx = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)

            while (cursor.moveToNext()) {
                val id = if (idIdx != -1) cursor.getString(idIdx)?.trim().orEmpty() else ""
                val name = if (nameIdx != -1) cursor.getString(nameIdx)?.trim().orEmpty() else ""
                val lookupKey = if (keyIdx != -1) cursor.getString(keyIdx)?.trim().orEmpty() else ""
                if (id.isBlank() || name.isBlank() || seenContactIds.contains(id)) continue

                val uri = if (lookupKey.isNotBlank()) {
                    ContactsContract.Contacts.getLookupUri(id.toLongOrNull() ?: 0L, lookupKey)?.toString().orEmpty()
                } else {
                    Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id).toString()
                }

                itemsMap["contact_$id"] = DeviceContactItem(
                    id = id,
                    displayName = name,
                    phoneNumber = "",
                    contactUri = uri
                )
            }
        }
    } catch (_: Throwable) {
    }

    return itemsMap.values.sortedBy { it.displayName.lowercase() }
}
