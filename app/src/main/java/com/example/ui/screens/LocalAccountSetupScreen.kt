package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AlgeriaCommuneMatch
import com.example.data.AlgeriaLocationsCatalog
import com.example.data.AlgerianCommune
import com.example.data.AlgerianWilaya
import com.example.ui.components.VaultixLogoContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SetupCreamBg = Color(0xFFFCF4E8)
private val SetupPastelBlob = Color(0xFFF9DFDC)
private val SetupRoyalIndigo = Color(0xFF334194)
private val SetupFieldGray = Color(0xFFD6D7DA)
private val SetupPlaceholderGray = Color(0xFF6B7280)
private val SetupInkDark = Color(0xFF0F172A)
private val SetupErrorRed = Color(0xFFDC2626)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalAccountSetupScreen(
    isArabic: Boolean,
    initialUsername: String = "",
    initialWilayaCode: String = "",
    initialCommuneName: String = "",
    onToggleLanguage: (String) -> Unit,
    onBack: () -> Unit,
    onCreateAccount: (
        username: String,
        wilayaCode: String,
        wilayaName: String,
        communeName: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    BackHandler(onBack = onBack)

    var allWilayas by remember { mutableStateOf<List<AlgerianWilaya>>(emptyList()) }
    var username by remember { mutableStateOf(initialUsername) }
    var selectedWilaya by remember { mutableStateOf<AlgerianWilaya?>(null) }
    var selectedCommune by remember { mutableStateOf<AlgerianCommune?>(null) }

    var showWilayaSheet by remember { mutableStateOf(false) }
    var showCommuneSheet by remember { mutableStateOf(false) }
    var showValidationErrors by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            AlgeriaLocationsCatalog.loadFromAssets(context)
        }
        allWilayas = loaded
        if (selectedWilaya == null && initialWilayaCode.isNotBlank()) {
            val matchedW = loaded.firstOrNull { it.code == initialWilayaCode }
            selectedWilaya = matchedW
            if (matchedW != null && initialCommuneName.isNotBlank() && selectedCommune == null) {
                selectedCommune = matchedW.communes.firstOrNull {
                    it.nameAr == initialCommuneName || it.nameEn == initialCommuneName
                }
            }
        }
    }

    val totalCommunesCount = remember(allWilayas) {
        allWilayas.sumOf { it.communes.size }
    }

    val usernameError = showValidationErrors && username.trim().isEmpty()
    val wilayaError = showValidationErrors && selectedWilaya == null
    val communeError = showValidationErrors && selectedCommune == null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SetupCreamBg)
    ) {
        // Matching soft pastel curved background blobs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val topBlob = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.68f, 0f)
                cubicTo(
                    w * 0.78f, h * 0.14f,
                    w * 0.62f, h * 0.25f,
                    w * 0.44f, h * 0.27f
                )
                cubicTo(
                    w * 0.26f, h * 0.29f,
                    w * 0.20f, h * 0.40f,
                    0f, h * 0.44f
                )
                close()
            }
            drawPath(path = topBlob, color = SetupPastelBlob)

            val bottomBlob = Path().apply {
                moveTo(w, h)
                lineTo(w * 0.34f, h)
                cubicTo(
                    w * 0.28f, h * 0.86f,
                    w * 0.42f, h * 0.76f,
                    w * 0.62f, h * 0.72f
                )
                cubicTo(
                    w * 0.80f, h * 0.68f,
                    w * 0.88f, h * 0.58f,
                    w, h * 0.54f
                )
                close()
            }
            drawPath(path = bottomBlob, color = SetupPastelBlob)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header: Back Button + Language Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.82f),
                    border = BorderStroke(1.dp, SetupRoyalIndigo.copy(alpha = 0.22f)),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("local_account_back_button"),
                    onClick = onBack
                ) {
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back",
                            tint = SetupInkDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.78f))
                        .border(1.dp, SetupRoyalIndigo.copy(alpha = 0.22f), RoundedCornerShape(50))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LocalSetupLangChip(
                        label = "العربية",
                        selected = isArabic,
                        onClick = { onToggleLanguage("ar") },
                        tag = "local_setup_lang_ar"
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    LocalSetupLangChip(
                        label = "EN",
                        selected = !isArabic,
                        onClick = { onToggleLanguage("en") },
                        tag = "local_setup_lang_en"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VaultixLogoContainer(
                    size = 76.dp,
                    cornerRadius = 22.dp,
                    padding = 9.dp,
                    forceObsidianFrame = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isArabic) "إنشاء حساب محلي" else "Create Local Account",
                    color = SetupInkDark,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isArabic) {
                        "أدخل معلوماتك المحلية • ${allWilayas.size.takeIf { it > 0 } ?: 69} ولاية و ${totalCommunesCount.takeIf { it > 0 } ?: 1541} بلدية جزائرية"
                    } else {
                        "Local on-device profile • ${allWilayas.size.takeIf { it > 0 } ?: 69} Wilayas & ${totalCommunesCount.takeIf { it > 0 } ?: 1541} Communes"
                    },
                    color = SetupPlaceholderGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 1. اسم المستخدم (Username)
                FieldLabel(
                    text = if (isArabic) "اسم المستخدم" else "Username",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SetupFieldGray)
                        .border(
                            width = if (usernameError) 1.5.dp else 1.dp,
                            color = if (usernameError) SetupErrorRed else SetupRoyalIndigo.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SetupRoyalIndigo),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = if (isArabic) "اسم المستخدم" else "Username",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (username.isEmpty()) {
                            Text(
                                text = if (isArabic) "أدخل اسم المستخدم" else "Enter your username",
                                color = SetupPlaceholderGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        BasicTextField(
                            value = username,
                            onValueChange = { username = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = SetupInkDark,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            cursorBrush = SolidColor(SetupRoyalIndigo),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("local_username_input")
                        )
                    }
                    if (username.isNotEmpty()) {
                        IconButton(
                            onClick = { username = "" },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isArabic) "مسح" else "Clear",
                                tint = SetupPlaceholderGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                if (usernameError) {
                    ErrorCaption(
                        text = if (isArabic) "يرجى إدخال اسم المستخدم" else "Please enter a username"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. ولاية الإقامة (Wilaya of Residence - All 69 Wilayas)
                FieldLabel(
                    text = if (isArabic) "ولاية الإقامة" else "Wilaya of Residence",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                SelectorInputBar(
                    icon = Icons.Outlined.LocationCity,
                    valueText = selectedWilaya?.displayName(isArabic),
                    placeholder = if (isArabic) {
                        "اختر ولاية الإقامة (كل الولايات - 69)"
                    } else {
                        "Select Wilaya of residence (all 69)"
                    },
                    isError = wilayaError,
                    testTag = "local_wilaya_selector",
                    onClick = {
                        focusManager.clearFocus()
                        showWilayaSheet = true
                    }
                )
                if (wilayaError) {
                    ErrorCaption(
                        text = if (isArabic) "يرجى اختيار ولاية الإقامة" else "Please select your Wilaya"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. بلدية الإقامة (Commune of Residence - All 1,541 Communes)
                FieldLabel(
                    text = if (isArabic) "بلدية الإقامة" else "Commune of Residence",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                SelectorInputBar(
                    icon = Icons.Outlined.Place,
                    valueText = selectedCommune?.displayName(isArabic),
                    placeholder = if (selectedWilaya != null) {
                        if (isArabic) "اختر بلدية الإقامة (${selectedWilaya!!.communes.size} بلدية)"
                        else "Select Commune (${selectedWilaya!!.communes.size} communes)"
                    } else {
                        if (isArabic) "اختر بلدية الإقامة (كل البلديات)"
                        else "Select Commune of residence"
                    },
                    isError = communeError,
                    testTag = "local_commune_selector",
                    onClick = {
                        focusManager.clearFocus()
                        showCommuneSheet = true
                    }
                )
                if (communeError) {
                    ErrorCaption(
                        text = if (isArabic) "يرجى اختيار بلدية الإقامة" else "Please select your Commune"
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Bottom Submit Button: إنشاء حساب
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val w = selectedWilaya
                        val c = selectedCommune
                        val cleanName = username.trim()
                        if (cleanName.isEmpty() || w == null || c == null) {
                            showValidationErrors = true
                        } else {
                            onCreateAccount(
                                cleanName,
                                w.code,
                                if (isArabic) w.nameAr else w.nameEn,
                                if (isArabic) c.nameAr else c.nameEn
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(
                            elevation = 5.dp,
                            shape = RoundedCornerShape(14.dp),
                            spotColor = SetupRoyalIndigo.copy(alpha = 0.30f)
                        )
                        .testTag("local_account_submit_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SetupRoyalIndigo,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isArabic) "إنشاء حساب" else "Create Account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Searchable ModalBottomSheet for All 69 Algerian Wilayas
    if (showWilayaSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var query by remember { mutableStateOf("") }
        val filteredWilayas by remember(allWilayas, query) {
            derivedStateOf {
                AlgeriaLocationsCatalog.filterWilayas(allWilayas, query)
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showWilayaSheet = false },
            sheetState = sheetState,
            containerColor = SetupCreamBg,
            contentColor = SetupInkDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .heightIn(max = 560.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "ولاية الإقامة (${allWilayas.size} ولاية)" else "Select Wilaya (${allWilayas.size})",
                            color = SetupInkDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isArabic) "ابحث بالاسم أو رقم الولاية (01 - 69)" else "Search by name or code (01 - 69)",
                            color = SetupPlaceholderGray,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { showWilayaSheet = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isArabic) "إغلاق" else "Close",
                            tint = SetupInkDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                PickerSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = if (isArabic) "ابحث عن الولاية..." else "Search Wilaya...",
                    testTag = "wilaya_search_input"
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredWilayas.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "لا توجد نتائج مطابقة" else "No matching Wilaya found",
                            color = SetupPlaceholderGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(filteredWilayas, key = { it.code }) { wilaya ->
                            val isSelected = selectedWilaya?.code == wilaya.code
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) SetupRoyalIndigo.copy(alpha = 0.12f) else Color.White,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) SetupRoyalIndigo else SetupInkDark.copy(alpha = 0.08f)
                                ),
                                onClick = {
                                    if (selectedWilaya?.code != wilaya.code) {
                                        selectedWilaya = wilaya
                                        selectedCommune = null
                                    }
                                    showWilayaSheet = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("wilaya_item_${wilaya.code}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) SetupRoyalIndigo else SetupRoyalIndigo.copy(alpha = 0.12f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = wilaya.code,
                                            color = if (isSelected) Color.White else SetupRoyalIndigo,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isArabic) wilaya.nameAr else wilaya.nameEn,
                                            color = SetupInkDark,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isArabic) {
                                                "${wilaya.nameEn} • ${wilaya.communes.size} بلدية"
                                            } else {
                                                "${wilaya.nameAr} • ${wilaya.communes.size} communes"
                                            },
                                            color = SetupPlaceholderGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = SetupRoyalIndigo,
                                            modifier = Modifier.size(20.dp)
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

    // Searchable ModalBottomSheet for All Algerian Communes (scoped to Wilaya or All 1,541)
    if (showCommuneSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var query by remember { mutableStateOf("") }
        var searchAllCommunes by remember(selectedWilaya) { mutableStateOf(selectedWilaya == null) }

        val currentMatches by remember(allWilayas, selectedWilaya, searchAllCommunes, query) {
            derivedStateOf {
                val activeW = selectedWilaya
                if (!searchAllCommunes && activeW != null) {
                    AlgeriaLocationsCatalog.filterCommunes(activeW.communes, query)
                        .map { AlgeriaCommuneMatch(activeW, it) }
                } else {
                    AlgeriaLocationsCatalog.searchAllCommunes(allWilayas, query)
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showCommuneSheet = false },
            sheetState = sheetState,
            containerColor = SetupCreamBg,
            contentColor = SetupInkDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .heightIn(max = 580.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (!searchAllCommunes && selectedWilaya != null) {
                                if (isArabic) {
                                    "بلديات ${selectedWilaya!!.nameAr} (${selectedWilaya!!.communes.size})"
                                } else {
                                    "${selectedWilaya!!.nameEn} Communes (${selectedWilaya!!.communes.size})"
                                }
                            } else {
                                if (isArabic) {
                                    "كل البلديات الجزائرية ($totalCommunesCount)"
                                } else {
                                    "All Algerian Communes ($totalCommunesCount)"
                                }
                            },
                            color = SetupInkDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isArabic) {
                                "ابحث بالعربية أو الفرنسية واختر بلديتك"
                            } else {
                                "Search in Arabic or English & select commune"
                            },
                            color = SetupPlaceholderGray,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { showCommuneSheet = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isArabic) "إغلاق" else "Close",
                            tint = SetupInkDark
                        )
                    }
                }

                if (selectedWilaya != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterScopeChip(
                            label = if (isArabic) "بلديات ${selectedWilaya!!.nameAr}" else selectedWilaya!!.nameEn,
                            selected = !searchAllCommunes,
                            onClick = { searchAllCommunes = false },
                            modifier = Modifier.weight(1f)
                        )
                        FilterScopeChip(
                            label = if (isArabic) "كل البلديات ($totalCommunesCount)" else "All Communes ($totalCommunesCount)",
                            selected = searchAllCommunes,
                            onClick = { searchAllCommunes = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                PickerSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = if (isArabic) "ابحث عن البلدية..." else "Search Commune...",
                    testTag = "commune_search_input"
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (currentMatches.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "لا توجد بلدية مطابقة" else "No matching Commune found",
                            color = SetupPlaceholderGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(
                            items = currentMatches,
                            key = { "${it.wilaya.code}_${it.commune.nameEn}" }
                        ) { match ->
                            val isSelected = selectedWilaya?.code == match.wilaya.code &&
                                selectedCommune?.nameAr == match.commune.nameAr
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) SetupRoyalIndigo.copy(alpha = 0.12f) else Color.White,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) SetupRoyalIndigo else SetupInkDark.copy(alpha = 0.08f)
                                ),
                                onClick = {
                                    selectedWilaya = match.wilaya
                                    selectedCommune = match.commune
                                    showCommuneSheet = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("commune_item_${match.wilaya.code}_${match.commune.nameEn}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isArabic) match.commune.nameAr else match.commune.nameEn,
                                            color = SetupInkDark,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isArabic) {
                                                if (searchAllCommunes) {
                                                    "${match.commune.nameEn} • ${match.wilaya.code} - ${match.wilaya.nameAr}"
                                                } else {
                                                    match.commune.nameEn
                                                }
                                            } else {
                                                if (searchAllCommunes) {
                                                    "${match.commune.nameAr} • ${match.wilaya.code} - ${match.wilaya.nameEn}"
                                                } else {
                                                    match.commune.nameAr
                                                }
                                            },
                                            color = SetupPlaceholderGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = SetupRoyalIndigo,
                                            modifier = Modifier.size(20.dp)
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
}

@Composable
private fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = SetupInkDark,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 2.dp)
    )
}

@Composable
private fun ErrorCaption(text: String) {
    Text(
        text = text,
        color = SetupErrorRed,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, start = 4.dp, end = 4.dp)
    )
}

@Composable
private fun SelectorInputBar(
    icon: ImageVector,
    valueText: String?,
    placeholder: String,
    isError: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SetupFieldGray)
            .border(
                width = if (isError) 1.5.dp else 1.dp,
                color = if (isError) SetupErrorRed else SetupRoyalIndigo.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SetupRoyalIndigo),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = placeholder,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = valueText ?: placeholder,
            color = if (valueText != null) SetupInkDark else SetupPlaceholderGray,
            fontSize = if (valueText != null) 15.sp else 14.sp,
            fontWeight = if (valueText != null) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = SetupRoyalIndigo,
            modifier = Modifier
                .padding(end = 14.dp)
                .size(22.dp)
        )
    }
}

@Composable
private fun PickerSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    testTag: String
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = {
            Text(
                text = placeholder,
                color = SetupPlaceholderGray,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = SetupRoyalIndigo
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = SetupPlaceholderGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = SetupRoyalIndigo,
            unfocusedBorderColor = SetupInkDark.copy(alpha = 0.16f),
            focusedTextColor = SetupInkDark,
            unfocusedTextColor = SetupInkDark,
            cursorColor = SetupRoyalIndigo
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
private fun FilterScopeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) SetupRoyalIndigo else Color.White)
            .border(
                width = 1.dp,
                color = if (selected) SetupRoyalIndigo else SetupInkDark.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else SetupInkDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LocalSetupLangChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) SetupRoyalIndigo else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else SetupInkDark.copy(alpha = 0.72f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
