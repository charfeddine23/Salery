package com.example

import com.example.data.ExpenseCategories
import com.example.data.DebtType
import com.example.data.TransactionType
import com.example.data.UserSettings
import com.example.ui.components.toPatternHash
import com.example.ui.i18n.APP_NAME
import com.example.ui.i18n.APP_VERSION
import com.example.ui.i18n.ArabicStrings
import com.example.ui.i18n.DEVELOPER_NAME
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.formatDate
import com.example.ui.i18n.formatDzdAmount
import com.example.ui.i18n.formatPercent
import com.example.ui.i18n.isolateLtr
import com.example.update.hasVisibleTopIndicator
import com.example.update.isMandatoryBlockActive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun appIdentity_andDeveloper_areCorrect() {
    assertEquals("Vaultix", APP_NAME)
    assertEquals("Charfeddine Ka", DEVELOPER_NAME)
    assertTrue(APP_VERSION.isNotBlank())
  }

  @Test
  fun currencyFormatting_usesDzd() {
    val formatted = formatDzdAmount(85000.0, "DZD")
    assertTrue(formatted.contains("85,000") && formatted.contains("DZD"))

    val stealthFormatted = formatDzdAmount(85000.0, "DZD", isStealth = true)
    assertTrue(stealthFormatted.contains("••••••") && stealthFormatted.contains("DZD"))
  }

  @Test
  fun patternHash_isFormattedCorrectly() {
    val pattern = listOf(0, 1, 2, 5, 8)
    val hash = pattern.toPatternHash()
    assertEquals("0-1-2-5-8", hash)
  }

  @Test
  fun enumTypes_areConsistent() {
    assertEquals(TransactionType.INCOME.name, "INCOME")
    assertEquals(TransactionType.EXPENSE.name, "EXPENSE")
    assertEquals(DebtType.OWED_BY_ME.name, "OWED_BY_ME")
    assertEquals(DebtType.OWED_TO_ME.name, "OWED_TO_ME")
  }

  @Test
  fun savingsWallet_balanceCalculation_andSpending_isMathematicallyConsistent() {
    // 1. Initial State: Income = 100,000 DZD, Expenses = 0, Savings = 0
    val totalIncome = 100000.0
    var totalExpenses = 0.0
    var savingsWallet = 0.0

    var availableBalance = totalIncome - totalExpenses - savingsWallet
    var grossWealth = totalIncome - totalExpenses
    assertEquals(100000.0, availableBalance, 0.01)
    assertEquals(100000.0, grossWealth, 0.01)

    // 2. Transfer 25,000 DZD to Savings Wallet (محفظة الادخار)
    val depositAmount = 25000.0
    savingsWallet += depositAmount
    availableBalance = totalIncome - totalExpenses - savingsWallet
    grossWealth = totalIncome - totalExpenses

    // Available spendable balance decreases by deposit amount
    assertEquals(75000.0, availableBalance, 0.01)
    assertEquals(25000.0, savingsWallet, 0.01)
    // Gross wealth remains 100,000
    assertEquals(100000.0, grossWealth, 0.01)

    // 3. Withdraw 5,000 DZD from Savings back to Available
    val withdrawAmount = 5000.0
    savingsWallet -= withdrawAmount
    availableBalance = totalIncome - totalExpenses - savingsWallet
    grossWealth = totalIncome - totalExpenses

    assertEquals(80000.0, availableBalance, 0.01)
    assertEquals(20000.0, savingsWallet, 0.01)
    assertEquals(100000.0, grossWealth, 0.01)

    // 4. Spend 4,000 DZD directly from Savings Wallet
    // The amount is deducted from savings, and recorded as an expense in expenses page
    val spendAmount = 4000.0
    savingsWallet -= spendAmount
    totalExpenses += spendAmount

    availableBalance = totalIncome - totalExpenses - savingsWallet
    grossWealth = totalIncome - totalExpenses

    // Savings decreases to 16,000
    assertEquals(16000.0, savingsWallet, 0.01)
    // Available spendable balance remains 80,000 (was not double-deducted)
    assertEquals(80000.0, availableBalance, 0.01)
    // Gross wealth accurately drops by 4,000 to 96,000
    assertEquals(96000.0, grossWealth, 0.01)
  }

  @Test
  fun predefinedCategories_containsFoodTransportUtilities_andResolvesAccurately() {
    val expenseCategories = ExpenseCategories.getExpenseCategories()
    assertTrue(expenseCategories.isNotEmpty())

    val categoryIds = expenseCategories.map { it.id }
    assertTrue("Should contain food category", categoryIds.contains("food"))
    assertTrue("Should contain transport category", categoryIds.contains("transport"))
    assertTrue("Should contain utilities category", categoryIds.contains("utilities"))
    assertTrue("Should contain shopping category", categoryIds.contains("shopping"))
    assertTrue("Should contain health category", categoryIds.contains("health"))

    // Test resolution by id and keywords
    val foodCat = ExpenseCategories.findCategory("food")
    assertEquals("food", foodCat.id)
    assertNotNull(foodCat.icon)

    val transportCat = ExpenseCategories.findCategory("transport")
    assertEquals("transport", transportCat.id)
    assertNotNull(transportCat.icon)

    val utilitiesCat = ExpenseCategories.findCategory("utilities")
    assertEquals("utilities", utilitiesCat.id)
    assertNotNull(utilitiesCat.icon)

    // Keyword fallback checks
    assertEquals("food", ExpenseCategories.findCategory("طعام").id)
    assertEquals("transport", ExpenseCategories.findCategory("نقل").id)
    assertEquals("utilities", ExpenseCategories.findCategory("bills").id)
  }

  @Test
  fun categoryTranslations_matchEnglishAndArabic() {
    assertEquals("Food & Dining", EnglishStrings.categoryName("food"))
    assertEquals("Transport", EnglishStrings.categoryName("transport"))
    assertEquals("Utilities", EnglishStrings.categoryName("utilities"))

    assertEquals("طعام ومطاعم", ArabicStrings.categoryName("food"))
    assertEquals("نقل ومواصلات", ArabicStrings.categoryName("transport"))
    assertEquals("فواتير وخدمات", ArabicStrings.categoryName("utilities"))
  }

  @Test
  fun classicCardFormatting_handlesDefaultCustomAndStealth() {
    assertEquals("6280  26••  ••••  9842", com.example.ui.components.formatCardDigitsDisplay("", isStealth = false))
    assertEquals("••••  ••••  ••••  ••••", com.example.ui.components.formatCardDigitsDisplay("", isStealth = true))
    assertEquals("6280  2612  3456  9842", com.example.ui.components.formatCardDigitsDisplay("6280261234569842", isStealth = false))
    assertEquals("••••  ••••  ••••  9842", com.example.ui.components.formatCardDigitsDisplay("6280261234569842", isStealth = true))
  }

  @Test
  fun appUpdate_versionComparison_andSizeFormatting_areAccurate() {
    assertTrue(com.example.update.compareVersionTags("v1.0.14", "1.0.3") > 0)
    assertTrue(com.example.update.compareVersionTags("v2.0.0", "1.9.99") > 0)
    assertEquals(0, com.example.update.compareVersionTags("v1.0.3", "1.0.3"))
    assertTrue(com.example.update.compareVersionTags("v1.0.2", "1.0.3") < 0)
    assertEquals("12.0 MB", com.example.update.formatBytesToMb(12L * 1024L * 1024L))
  }

  @Test
  fun appUpdate_validation_rejectsMissingAndCorruptApkFiles() {
    val missing = java.io.File("non_existent_salery_update.apk")
    val emptyOrSmall = java.io.File.createTempFile("corrupt_update", ".apk")
    val invalidLargeNonZip = java.io.File.createTempFile("invalid_non_zip", ".apk")
    try {
      emptyOrSmall.writeText("<html>404 Not Found</html>")
      invalidLargeNonZip.writeBytes(ByteArray(120_000) { 0x41 })
      org.junit.Assert.assertFalse(
        com.example.update.AppUpdateManager.hasValidApkHeaderAndSize(missing)
      )
      org.junit.Assert.assertFalse(
        com.example.update.AppUpdateManager.hasValidApkHeaderAndSize(emptyOrSmall)
      )
      org.junit.Assert.assertFalse(
        com.example.update.AppUpdateManager.hasValidApkHeaderAndSize(invalidLargeNonZip)
      )
    } finally {
      emptyOrSmall.delete()
      invalidLargeNonZip.delete()
    }
  }

  @Test
  fun hubVaultCodec_formatsManualRip_sortsPinned_andRoundTripsJson() {
    val rip = "00799999001234567897"
    val grouped = com.example.data.HubVaultCodec.formatRipGrouped(rip)
    assertEquals("007 99999 0012345678 97", grouped)

    val keys = listOf(
      com.example.data.VaultKeyEntry(
        id = "k1",
        label = "Edahabia",
        username = "charfeddine@mail.com",
        secretValue = "S@lery#2026!Dz",
        category = "BANK",
        note = "Main card"
      )
    )
    val parsedKeys = com.example.data.HubVaultCodec.parseVaultKeys(
      com.example.data.HubVaultCodec.encodeVaultKeys(keys)
    )
    assertEquals(1, parsedKeys.size)
    assertEquals("Edahabia", parsedKeys[0].label)
    assertEquals("charfeddine@mail.com", parsedKeys[0].username)
    assertEquals("S@lery#2026!Dz", parsedKeys[0].secretValue)

    val unpinned = com.example.data.CcpAccountEntry(
      id = "c1",
      firstName = "Amine",
      lastName = "Benali",
      holderName = "Amine Benali",
      ccpNumber = "12345678",
      ccpKey = "42",
      ripFull = "",
      agencyNote = "Algiers",
      isPinned = false,
      createdAt = 2000L
    )
    val pinned = com.example.data.CcpAccountEntry(
      id = "c2",
      firstName = "Charfeddine",
      lastName = "Ka",
      holderName = "Charfeddine Ka",
      ccpNumber = "87654321",
      ccpKey = "11",
      ripFull = rip,
      contactName = "Charfeddine",
      contactPhone = "0550112233",
      isPinned = true,
      pinnedAt = 1000L,
      createdAt = 1000L
    )
    val parsedCcps = com.example.data.HubVaultCodec.parseCcpAccounts(
      com.example.data.HubVaultCodec.encodeCcpAccounts(listOf(unpinned, pinned))
    )
    assertEquals(2, parsedCcps.size)
    assertEquals("c2", parsedCcps[0].id)
    assertTrue(parsedCcps[0].isPinned)
    assertEquals("Charfeddine Ka", parsedCcps[0].displayFullName)
    assertEquals("0550112233", parsedCcps[0].contactPhone)
    assertEquals("", parsedCcps[1].ripFull)
  }

  @Test
  fun passwordStrengthAnalyzer_evaluatesWeakStrongAndReusedPasswords() {
    val weak = com.example.data.PasswordStrengthAnalyzer.analyze(
      password = "123456",
      username = "amine",
      label = "BaridiMob"
    )
    assertEquals(com.example.data.PasswordStrengthLevel.WEAK, weak.level)
    assertTrue(weak.score < 40)

    val strong = com.example.data.PasswordStrengthAnalyzer.analyze(
      password = "K9!vX2@mPq#84Lw",
      username = "amine",
      label = "BaridiMob"
    )
    assertEquals(com.example.data.PasswordStrengthLevel.STRONG, strong.level)
    assertTrue(strong.score >= 82)

    val reused = com.example.data.PasswordStrengthAnalyzer.analyze(
      password = "K9!vX2@mPq#84Lw",
      duplicateCount = 3
    )
    assertTrue(reused.isReused)
    assertTrue(reused.score < strong.score)

    val generated = com.example.data.PasswordStrengthAnalyzer.generateStrongPassword(14)
    assertEquals(14, generated.length)
    assertEquals(
      com.example.data.PasswordStrengthLevel.STRONG,
      com.example.data.PasswordStrengthAnalyzer.analyze(generated).level
    )
  }

  @Test
  fun algPosteSmsAnalyzer_parsesExpenseIncomeAndIgnoresBalanceOrOtp() {
    // 1. Negative withdrawal amount "-300.00 DA" with trailing Solde -> EXPENSE 300.0
    val debitSms = com.example.sms.AlgPosteSmsAnalyzer.parseSms(
      sender = "ALG Poste",
      body = "Retrait GAB -300.00 DA le 14/11/2026. Solde: 24500.00 DA",
      timestampMillis = 1_763_120_000_000L
    )
    assertNotNull(debitSms)
    assertEquals(true, debitSms!!.isExpense)
    assertEquals(TransactionType.EXPENSE, debitSms.transactionType)
    assertEquals(300.0, debitSms.amount, 0.01)

    // 2. Positive credit / incoming transfer -> INCOME 15000.0
    val creditSms = com.example.sms.AlgPosteSmsAnalyzer.parseSms(
      sender = "BaridiMob",
      body = "Virement reçu +15 000,00 DA. Nouveau solde: 39500.00 DA",
      timestampMillis = 1_763_120_120_000L
    )
    assertNotNull(creditSms)
    assertEquals(false, creditSms!!.isExpense)
    assertEquals(TransactionType.INCOME, creditSms.transactionType)
    assertEquals(15000.0, creditSms.amount, 0.01)

    // 3. Arabic numerals & minus sign -> EXPENSE 300.0
    val arabicDebitSms = com.example.sms.AlgPosteSmsAnalyzer.parseSms(
      sender = "ALG Poste",
      body = "تم سحب -٣٠٠.٠٠ دج من حسابك",
      timestampMillis = 1_763_120_240_000L
    )
    assertNotNull(arabicDebitSms)
    assertTrue(arabicDebitSms!!.isExpense)
    assertEquals(300.0, arabicDebitSms.amount, 0.01)

    // 4. OTP message must be ignored
    val otpSms = com.example.sms.AlgPosteSmsAnalyzer.parseSms(
      sender = "ALG Poste",
      body = "Votre code OTP de confirmation est 482910.",
      timestampMillis = 1_763_120_360_000L
    )
    org.junit.Assert.assertNull(otpSms)

    // 5. Unrelated sender ignored when allowAnySenderForManualTest = false
    val nonPosteSms = com.example.sms.AlgPosteSmsAnalyzer.parseSms(
      sender = "Mobilis",
      body = "Promo -300.00 DA",
      timestampMillis = 1_763_120_480_000L,
      allowAnySenderForManualTest = false
    )
    org.junit.Assert.assertNull(nonPosteSms)

    // 6. Moshi Codec round-trip
    val encoded = com.example.sms.AlgPosteSmsCodec.encodeSuggestions(listOf(debitSms, creditSms))
    val decoded = com.example.sms.AlgPosteSmsCodec.parseSuggestions(encoded)
    assertEquals(2, decoded.size)
  }

  @Test
  fun vaultAppPresets_detectsAndResolvesAppThemesAndRoundTripsPresetId() {
    val insta = com.example.data.VaultAppPresets.detectFromLabel("My Instagram Account")
    assertNotNull(insta)
    assertEquals("instagram", insta!!.id)
    assertEquals(com.example.data.VaultAppMotif.CAMERA_LENS_RINGS, insta.motif)

    val bybit = com.example.data.VaultAppPresets.findById("bybit")
    assertEquals(com.example.data.VaultAppMotif.CRYPTO_HEX_GRID, bybit.motif)

    val entry = com.example.data.VaultKeyEntry(
      id = "v_app_1",
      label = "TikTok Creator",
      username = "@charfeddine",
      secretValue = "Strong#Pass2026!",
      category = "APP",
      appPresetId = "tiktok"
    )
    val encoded = com.example.data.HubVaultCodec.encodeVaultKeys(listOf(entry))
    val decoded = com.example.data.HubVaultCodec.parseVaultKeys(encoded)
    assertEquals(1, decoded.size)
    assertEquals("tiktok", decoded[0].appPresetId)
    assertEquals(
      com.example.data.VaultAppMotif.SOUND_WAVES,
      com.example.data.VaultAppPresets.resolveForEntry(decoded[0]).motif
    )
  }

  @Test
  fun themeEngineCatalog_containsClassicAndAll30NewThemes_andResolvesDarkAndLight() {
    val themes = com.example.ui.theme.ThemeEngineCatalog.allThemes
    assertEquals(32, themes.size)
    assertEquals(32, themes.map { it.id }.toSet().size)

    val expectedNames = listOf(
      "Liquid Glass",
      "Neumorphism",
      "Dark Luxury",
      "Neo-Glassmorphism",
      "Cyberpunk Neon",
      "Minimalist Monochrome",
      "Emerald Opulence",
      "Champagne Gold",
      "OLED Midnight",
      "Royal Velvet",
      "Titanium Metal",
      "Rose Quartz & Slate",
      "Cyber Organic",
      "Sunset Gradient",
      "Deep Amethyst",
      "Neo-Brutalist Luxury",
      "Liquid Metal",
      "Frosted Glass",
      "Platinum Minimal",
      "Cosmic Nebula",
      "Retro Noir",
      "Warm Earthy Tones",
      "Cyber Metallic",
      "Deep Burgundy",
      "Neon Blueprint",
      "Soft Pastels",
      "Industrial Steel",
      "Holographic Hologram",
      "Obsidian Black",
      "Dual-Tone Contrast",
      "Bio-Luminescent",
      "Modern Swiss"
    )
    val actualNames = themes.map { it.nameEn }.toSet()
    expectedNames.forEach { expected ->
      assertTrue("Missing theme: $expected", actualNames.contains(expected))
    }

    themes.forEach { spec ->
      assertTrue(spec.nameEn.isNotBlank())
      assertTrue(spec.nameAr.isNotBlank())
      val dark = com.example.ui.theme.ThemeEngineCatalog.resolveColors(spec.id, isDark = true)
      val light = com.example.ui.theme.ThemeEngineCatalog.resolveColors(spec.id, isDark = false)
      assertTrue(dark.isDark)
      org.junit.Assert.assertFalse(light.isDark)
      assertEquals(spec.id, dark.designStyle)
      assertEquals(spec.id, light.designStyle)
    }

    val fallback = com.example.ui.theme.ThemeEngineCatalog.findById("UNKNOWN_THEME_ID")
    assertEquals("LIQUID_GLASS", fallback.id)
    assertTrue(com.example.ui.theme.ThemeEngineCatalog.resolveColors("NEUMORPHISM", isDark = true).isNeumorphic)
  }

  @Test
  fun algPosteSmsAnalyzer_extractsBalance_huntsHiddenTaxes_andRoundTripsSnapshots() {
    // 1. French & Arabic balance extraction
    val frBal = com.example.sms.AlgPosteSmsAnalyzer.extractBalanceFromBody(
      "Retrait GAB -1000.00 DA le 15/11/2026. Nouveau solde: 23 950,00 DA"
    )
    assertNotNull(frBal)
    assertEquals(23950.0, frBal!!, 0.01)

    val arBal = com.example.sms.AlgPosteSmsAnalyzer.extractBalanceFromBody(
      "تم خصم -500.00 دج. الرصيد الحالي: 19400.00 دج"
    )
    assertNotNull(arBal)
    assertEquals(19400.0, arBal!!, 0.01)

    // 2. Balance discrepancy tax hunting:
    // Previous SMS balance = 25,000.00 DA
    // Debit SMS = -1,000.00 DA -> Expected balance = 24,000.00 DA
    // Actual balance = 23,950.00 DA -> Caught hidden tax/fee = 50.00 DA!
    val snap1 = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Virement reçu +1000.00 DA. Nouveau solde: 25000.00 DA",
      timestampMillis = 1_000_000L
    )!!
    val snap2 = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Paiement TPE -1000.00 DA. Nouveau solde: 23950.00 DA",
      timestampMillis = 2_000_000L
    )!!
    // 3. Explicit standalone tax/fee SMS
    val snap3 = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Frais tenue de compte -250.00 DA. Solde: 23700.00 DA",
      timestampMillis = 3_000_000L
    )!!

    val existingRecorded = listOf(
      com.example.sms.SmsTaxAnomalyEntry(
        id = "tax_fee_${snap3.id}",
        smsId = snap3.id,
        taxAmount = 250.0,
        detectionKind = "EXPLICIT_TAX_FEE",
        isRecordedAsExpense = true
      )
    )

    val anomalies = com.example.sms.AlgPosteSmsAnalyzer.detectTaxAnomalies(
      snapshots = listOf(snap1, snap2, snap3),
      existingAnomalies = existingRecorded
    )

    val discrepancy = anomalies.firstOrNull { it.smsId == snap2.id }
    assertNotNull("Expected 50 DA discrepancy anomaly to be caught", discrepancy)
    assertEquals(25000.0, discrepancy!!.previousBalance, 0.01)
    assertEquals(24000.0, discrepancy.expectedBalance, 0.01)
    assertEquals(23950.0, discrepancy.actualBalance, 0.01)
    assertEquals(50.0, discrepancy.taxAmount, 0.01)

    val explicitFee = anomalies.firstOrNull { it.smsId == snap3.id }
    assertNotNull("Expected explicit fee anomaly", explicitFee)
    assertEquals(250.0, explicitFee!!.taxAmount, 0.01)
    assertTrue("Should preserve recorded state", explicitFee.isRecordedAsExpense)

    // 4. Snapshot & Tax Anomaly Codec round-trips
    val encodedSnaps = com.example.sms.AlgPosteSmsCodec.encodeBalanceHistory(listOf(snap1, snap2, snap3))
    val decodedSnaps = com.example.sms.AlgPosteSmsCodec.parseBalanceHistory(encodedSnaps)
    assertEquals(3, decodedSnaps.size)
    assertEquals(23950.0, decodedSnaps[1].balance, 0.01)

    val encodedTaxes = com.example.sms.AlgPosteSmsCodec.encodeTaxAnomalies(anomalies)
    val decodedTaxes = com.example.sms.AlgPosteSmsCodec.parseTaxAnomalies(encodedTaxes)
    assertEquals(anomalies.size, decodedTaxes.size)
  }

  @Test
  fun ccpAccountEntry_appPresetResolution_andLegacyJsonBackwardCompat() {
    val styledEntry = com.example.data.CcpAccountEntry(
      id = "c_styled_1",
      firstName = "Charfeddine",
      lastName = "Ka",
      holderName = "Charfeddine Ka",
      ccpNumber = "12345678",
      ccpKey = "99",
      appPresetId = "redotpay"
    )
    val encoded = com.example.data.HubVaultCodec.encodeCcpAccounts(listOf(styledEntry))
    val decoded = com.example.data.HubVaultCodec.parseCcpAccounts(encoded)
    assertEquals(1, decoded.size)
    assertEquals("redotpay", decoded[0].appPresetId)
    assertEquals(
      "redotpay",
      com.example.data.VaultAppPresets.resolveForCcpEntry(decoded[0]).id
    )

    // Legacy JSON saved before appPresetId was added must parse cleanly with "algerie_poste"
    val legacyJson = """[{"id":"c_legacy","holderName":"Amine","ccpNumber":"87654321","ccpKey":"12"}]"""
    val parsedLegacy = com.example.data.HubVaultCodec.parseCcpAccounts(legacyJson)
    assertEquals(1, parsedLegacy.size)
    assertEquals("algerie_poste", parsedLegacy[0].appPresetId)
    assertEquals(
      "algerie_poste",
      com.example.data.VaultAppPresets.resolveForCcpEntry(parsedLegacy[0]).id
    )
  }

  @Test
  fun userSettings_defaultsToScreenshotProtectionDisabled() {
    val defaults = UserSettings()
    org.junit.Assert.assertFalse(defaults.screenshotProtectionEnabled)
  }

  @Test
  fun appUpdateUiState_updatesAreOptionalAndNeverBlockApp() {
    val dummyRelease = com.example.update.GitHubReleaseInfo(
      tagName = "v9.9.9",
      versionName = "9.9.9",
      title = "Optional Release",
      releaseNotes = "Feature update",
      publishedAt = "2026-11-15T00:00:00Z",
      htmlUrl = "https://github.com/charfeddine23/Salery/releases",
      apkName = "SaleryPulse-9.9.9.apk",
      apkDownloadUrl = "https://example.com/SaleryPulse.apk",
      apkSizeBytes = 12_000_000L
    )

    org.junit.Assert.assertFalse(com.example.update.AppUpdateUiState.Idle.isMandatoryBlockActive)
    org.junit.Assert.assertFalse(com.example.update.AppUpdateUiState.Checking.isMandatoryBlockActive)
    org.junit.Assert.assertFalse(com.example.update.AppUpdateUiState.UpToDate("1.0.0").isMandatoryBlockActive)
    org.junit.Assert.assertFalse(com.example.update.AppUpdateUiState.Error("Offline", null).isMandatoryBlockActive)
    org.junit.Assert.assertFalse(com.example.update.AppUpdateUiState.Available(dummyRelease).isMandatoryBlockActive)
    org.junit.Assert.assertFalse(com.example.update.AppUpdateUiState.Downloading(dummyRelease, 500L, 1000L).isMandatoryBlockActive)
    org.junit.Assert.assertFalse(
      com.example.update.AppUpdateUiState.ReadyToInstall(
        dummyRelease,
        java.io.File("dummy.apk")
      ).isMandatoryBlockActive
    )
    org.junit.Assert.assertFalse(com.example.update.AppUpdateUiState.Error("Failed download", dummyRelease).isMandatoryBlockActive)
  }

  @Test
  fun algPosteSmsAnalyzer_handlesRealWorldSenders_frenchArabicFormats_andBalanceLog() {
    // 1. Real-world carrier sender variants & hybrid body recognition
    val algPostTruncated = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Post",
      body = "Votre nouveau solde est de : 24 500,00 DA",
      timestampMillis = 1_000_000L
    )
    assertNotNull("Truncated 'ALG Post' sender must match", algPostTruncated)
    assertEquals(24500.0, algPostTruncated!!.balance, 0.01)

    val edahabiaSender = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "EDAHABIA",
      body = "Opération de paiement TPE de 1 200,00 DA effectuée. Solde le 15/11/2026 : 19.800,50 DZD",
      timestampMillis = 2_000_000L
    )
    assertNotNull("EDAHABIA sender and dated Solde format must match", edahabiaSender)
    assertEquals(19800.50, edahabiaSender!!.balance, 0.01)
    assertTrue(edahabiaSender.hasTransaction)
    assertTrue(edahabiaSender.isExpense)
    assertEquals(1200.0, edahabiaSender.transactionAmount, 0.01)

    val carrierShortCode = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "777",
      body = "BaridiMob: Nouveau solde de votre compte 0024567890 est de 32 100.00 DA",
      timestampMillis = 3_000_000L
    )
    assertNotNull("Short carrier code with BaridiMob body must match", carrierShortCode)
    assertEquals(32100.0, carrierShortCode!!.balance, 0.01)

    // 2. Arabic digits & "د ج" / "الرصيد البريدي" / "الرصيد المتاح هو :"
    val arAvailableBal = com.example.sms.AlgPosteSmsAnalyzer.extractBalanceFromBody(
      "الرصيد المتاح هو : ٣٢ ١٠٠ د ج"
    )
    assertNotNull(arAvailableBal)
    assertEquals(32100.0, arAvailableBal!!, 0.01)

    val arDebitAndPostalBal = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "بريدي موب",
      body = "تم خصم 1500.00 دج من حسابكم البريدي. الرصيد البريدي: 18500.00 دج",
      timestampMillis = 4_000_000L
    )
    assertNotNull(arDebitAndPostalBal)
    assertEquals(18500.0, arDebitAndPostalBal!!.balance, 0.01)
    assertTrue(arDebitAndPostalBal.hasTransaction)
    assertTrue(arDebitAndPostalBal.isExpense)
    assertEquals(1500.0, arDebitAndPostalBal.transactionAmount, 0.01)

    // 3. Ensure non-Poste carrier promos stay excluded
    val mobilisSolde = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "Mobilis",
      body = "Solde internet: 500.00 DA",
      timestampMillis = 5_000_000L,
      allowAnySenderForManualTest = false
    )
    org.junit.Assert.assertNull(mobilisSolde)

    // 4. Chronological Balance Log round-trip preserves all converted snapshots in order
    val encoded = com.example.sms.AlgPosteSmsCodec.encodeBalanceHistory(
      listOf(arDebitAndPostalBal, algPostTruncated, carrierShortCode, edahabiaSender)
    )
    val decoded = com.example.sms.AlgPosteSmsCodec.parseBalanceHistory(encoded)
    assertEquals(4, decoded.size)
    assertEquals(1_000_000L, decoded[0].timestampMillis)
    assertEquals(2_000_000L, decoded[1].timestampMillis)
    assertEquals(3_000_000L, decoded[2].timestampMillis)
    assertEquals(4_000_000L, decoded[3].timestampMillis)
  }

  @Test
  fun algPosteSmsAnalyzer_improvedTaxDetection_comparesWithPreviousBalance_andClearsStaleAnomalies() {
    // 1. Unsigned neutral operation inferred against previous SMS balance:
    // Prev = 25,000 DA, Withdrawn (unsigned) = 2,000 DA -> Expected = 23,000 DA
    // Current = 22,950 DA -> Difference (Tax) = 50 DA (NOT 2,050 DA!)
    val prevSnap = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Virement reçu +5 000,00 DA. Nouveau solde: 25 000,00 DA",
      timestampMillis = 10_000_000L
    )!!
    val neutralOpSnap = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "ALG Poste: Opération 2 000,00 DA. Nouveau solde: 22 950,00 DA",
      timestampMillis = 10_100_000L
    )!!

    val detected = com.example.sms.AlgPosteSmsAnalyzer.detectTaxAnomalies(
      snapshots = listOf(prevSnap, neutralOpSnap)
    )
    val caught = detected.firstOrNull { it.smsId == neutralOpSnap.id }
    assertNotNull("Expected unsigned operation discrepancy of 50 DA to be caught", caught)
    assertEquals(25000.0, caught!!.previousBalance, 0.01)
    assertEquals(2000.0, caught.transactionAmount, 0.01)
    assertTrue(caught.isExpense)
    assertEquals(23000.0, caught.expectedBalance, 0.01)
    assertEquals(22950.0, caught.actualBalance, 0.01)
    assertEquals(50.0, caught.taxAmount, 0.01)
    assertTrue(caught.explanationEn.contains("25,000"))
    assertTrue(caught.explanationEn.contains("2,000"))
    assertTrue(caught.explanationEn.contains("22,950"))
    assertTrue(caught.explanationEn.contains("50"))

    // 2. Inline Ancien solde extraction on a standalone first SMS
    val inlinePrevSnap = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Ancien solde: 10 000,00 DA. Retrait 1 000,00 DA. Nouveau solde: 8 970,00 DA",
      timestampMillis = 20_000_000L
    )!!
    assertEquals(8970.0, inlinePrevSnap.balance, 0.01)

    val standaloneAnomalies = com.example.sms.AlgPosteSmsAnalyzer.detectTaxAnomalies(
      snapshots = listOf(inlinePrevSnap)
    )
    val inlineAnomaly = standaloneAnomalies.firstOrNull { it.smsId == inlinePrevSnap.id }
    assertNotNull("Expected inline Ancien solde discrepancy to be caught", inlineAnomaly)
    assertEquals(10000.0, inlineAnomaly!!.previousBalance, 0.01)
    assertEquals(1000.0, inlineAnomaly.transactionAmount, 0.01)
    assertEquals(9000.0, inlineAnomaly.expectedBalance, 0.01)
    assertEquals(8970.0, inlineAnomaly.actualBalance, 0.01)
    assertEquals(30.0, inlineAnomaly.taxAmount, 0.01)

    // 3. Stale unrecorded anomaly cleanup while preserving recorded expense anomalies
    val cleanOpSnap = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Retrait GAB -2 000,00 DA. Nouveau solde: 23 000,00 DA",
      timestampMillis = 10_100_000L
    )!!.copy(id = neutralOpSnap.id)

    val staleUnrecorded = caught.copy(isRecordedAsExpense = false)
    val externalRecorded = com.example.sms.SmsTaxAnomalyEntry(
      id = "tax_diff_old_recorded",
      smsId = "old_recorded_sms",
      taxAmount = 75.0,
      detectionKind = "BALANCE_DISCREPANCY",
      detectedAtMillis = 5_000_000L,
      isRecordedAsExpense = true
    )
    val refreshed = com.example.sms.AlgPosteSmsAnalyzer.detectTaxAnomalies(
      snapshots = listOf(prevSnap, cleanOpSnap),
      existingAnomalies = listOf(staleUnrecorded, externalRecorded)
    )
    org.junit.Assert.assertFalse(
      "Stale unrecorded anomaly should be cleared once balance matches",
      refreshed.any { it.id == staleUnrecorded.id }
    )
    assertTrue(
      "Already recorded historical anomaly must be preserved",
      refreshed.any { it.id == externalRecorded.id && it.isRecordedAsExpense }
    )

    // 4. Explicit fee + balance discrepancy coexistence on the same SMS
    val baseSnap = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Nouveau solde: 20 000,00 DA",
      timestampMillis = 30_000_000L
    )!!
    val feePlusDiscrepancySnap = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Frais de gestion -200,00 DA. Nouveau solde: 19 750,00 DA",
      timestampMillis = 30_100_000L
    )!!
    val bothAnomalies = com.example.sms.AlgPosteSmsAnalyzer.detectTaxAnomalies(
      snapshots = listOf(baseSnap, feePlusDiscrepancySnap)
    )
    val diffPart = bothAnomalies.firstOrNull { it.id == "tax_diff_${feePlusDiscrepancySnap.id}" }
    val feePart = bothAnomalies.firstOrNull { it.id == "tax_fee_${feePlusDiscrepancySnap.id}" }
    assertNotNull("Discrepancy of 50 DA should be reported", diffPart)
    assertNotNull("Explicit fee of 200 DA should also be reported", feePart)
    assertEquals(50.0, diffPart!!.taxAmount, 0.01)
    assertEquals(200.0, feePart!!.taxAmount, 0.01)
  }

  @Test
  fun algPosteSmsAnalyzer_caughtTax_ignoresAmountsExceeding1000Dzd() {
    val snap0 = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Nouveau solde: 50 000,00 DA",
      timestampMillis = 1_000_000L
    )!!
    // Discrepancy = 1,500 DA (> 1,000 DA) -> MUST be ignored by Caught Tax
    val snapLargeDiff = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Retrait GAB -1 000,00 DA. Nouveau solde: 47 500,00 DA",
      timestampMillis = 2_000_000L
    )!!
    // Discrepancy = 1,000 DA exact (<= 1,000 DA) -> MUST be kept
    val snapBoundaryDiff = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Retrait GAB -2 000,00 DA. Nouveau solde: 44 500,00 DA",
      timestampMillis = 3_000_000L
    )!!
    // Discrepancy = 1,000.01 DA (> 1,000 DA) -> MUST be ignored
    val snapJustOver = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Retrait GAB -1 000,00 DA. Nouveau solde: 42 499,99 DA",
      timestampMillis = 4_000_000L
    )!!
    // Explicit fee > 1,000 DA -> MUST also be ignored by Caught Tax
    val snapLargeExplicitFee = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Frais annuels -1 250,00 DA. Nouveau solde: 41 249,99 DA",
      timestampMillis = 5_000_000L
    )!!
    // Normal small fee = 30 DA -> MUST be kept
    val snapSmallTax = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Paiement TPE -500,00 DA. Nouveau solde: 40 719,99 DA",
      timestampMillis = 6_000_000L
    )!!

    val caught = com.example.sms.AlgPosteSmsAnalyzer.detectTaxAnomalies(
      snapshots = listOf(
        snap0,
        snapLargeDiff,
        snapBoundaryDiff,
        snapJustOver,
        snapLargeExplicitFee,
        snapSmallTax
      )
    )

    org.junit.Assert.assertFalse(
      "Discrepancy of 1,500 DA (> 1,000 DA) must be ignored",
      caught.any { it.smsId == snapLargeDiff.id }
    )
    assertTrue(
      "Discrepancy of exactly 1,000 DA must be kept",
      caught.any { it.smsId == snapBoundaryDiff.id && kotlin.math.abs(it.taxAmount - 1000.0) < 0.01 }
    )
    org.junit.Assert.assertFalse(
      "Discrepancy of 1,000.01 DA (> 1,000 DA) must be ignored",
      caught.any { it.smsId == snapJustOver.id }
    )
    org.junit.Assert.assertFalse(
      "Explicit fee of 1,250 DA (> 1,000 DA) must be ignored",
      caught.any { it.smsId == snapLargeExplicitFee.id }
    )
    assertTrue(
      "Small 30 DA tax must be caught",
      caught.any { it.smsId == snapSmallTax.id && kotlin.math.abs(it.taxAmount - 30.0) < 0.01 }
    )

    // Codec parseTaxAnomalies must also filter stale unrecorded anomalies > 1000 DZD
    val staleOver = com.example.sms.SmsTaxAnomalyEntry(
      id = "tax_diff_stale_over",
      smsId = "s_over",
      taxAmount = 2500.0,
      isRecordedAsExpense = false
    )
    val validTax = caught.first { it.smsId == snapSmallTax.id }
    val rawJson = com.example.sms.AlgPosteSmsCodec.encodeTaxAnomalies(listOf(staleOver, validTax))
    val filteredParsed = com.example.sms.AlgPosteSmsCodec.parseTaxAnomalies(rawJson)
    org.junit.Assert.assertFalse(filteredParsed.any { it.id == staleOver.id })
    assertTrue(filteredParsed.any { it.id == validTax.id })
  }

  @Test
  fun permanentAlgPosteArchive_reconstructsDeletedHistory_studiesPatterns_andRoundTripsBackup() {
    val msg1 = com.example.sms.AlgPosteSmsAnalyzer.toArchivedEntity(
      sender = "ALG Poste",
      body = "Virement reçu +10 000,00 DA. Nouveau solde: 30 000,00 DA",
      timestampMillis = 10_000_000L,
      captureSource = "RECEIVER"
    )!!
    val msg2 = com.example.sms.AlgPosteSmsAnalyzer.toArchivedEntity(
      sender = "ALG Poste",
      body = "Retrait GAB -2 000,00 DA. Nouveau solde: 27 970,00 DA",
      timestampMillis = 20_000_000L,
      captureSource = "RECEIVER"
    )!!
    val msgOtp = com.example.sms.AlgPosteSmsAnalyzer.toArchivedEntity(
      sender = "ALG Poste",
      body = "Votre code OTP de confirmation est 123456.",
      timestampMillis = 25_000_000L,
      captureSource = "RECEIVER"
    )!!
    val msgUnknown = com.example.sms.AlgPosteSmsAnalyzer.toArchivedEntity(
      sender = "ALG Poste",
      body = "Notification BaridiMob: mise a jour de votre profil effectuee.",
      timestampMillis = 30_000_000L,
      captureSource = "INBOX"
    )!!

    assertEquals("FULL_MATCH", msg1.studyStatus)
    assertEquals(30000.0, msg1.extractedBalance!!, 0.01)
    assertEquals("OTP_IGNORED", msgOtp.studyStatus)
    assertEquals("NEEDS_STUDY", msgUnknown.studyStatus)

    // Reconstruct chronological balance timeline purely from permanent archive (even after phone SMS deletion)
    val reconstructed = com.example.sms.AlgPosteSmsAnalyzer.reconstructSnapshotsFromArchive(
      listOf(msg2, msgOtp, msgUnknown, msg1)
    )
    assertEquals(2, reconstructed.size)
    assertEquals(30000.0, reconstructed[0].balance, 0.01)
    assertEquals(27970.0, reconstructed[1].balance, 0.01)

    val taxes = com.example.sms.AlgPosteSmsAnalyzer.detectTaxAnomalies(reconstructed)
    assertEquals(1, taxes.size)
    assertEquals(30.0, taxes[0].taxAmount, 0.01)

    val report = com.example.sms.AlgPosteSmsAnalyzer.buildStudyReport(
      archived = listOf(msg1, msg2, msgOtp, msgUnknown),
      caughtTaxes = taxes
    )
    assertEquals(4, report.totalArchived)
    assertEquals(2, report.fullMatchCount)
    assertEquals(1, report.otpIgnoredCount)
    assertEquals(1, report.needsStudyCount)
    assertEquals(67, report.recognitionAccuracyPercent) // 2 of 3 non-OTP matched
    assertEquals(1, report.learnedFeePatterns.size)
    assertEquals(30.0, report.learnedFeePatterns[0].amountDzd, 0.01)
    assertEquals(1, report.learnedFeePatterns[0].occurrences)
  }

  @Test
  fun binanceCryptoPortfolio_includesXrpFirst_parsesMarketPrices_andEnforcesHourlySync() {
    val catalog = com.example.crypto.BinanceCryptoCatalog.majorAssets
    assertTrue(catalog.size >= 15)
    assertEquals("XRP", catalog.first().symbol)
    assertNotNull(com.example.crypto.BinanceCryptoCatalog.findBySymbol("xrp"))
    assertNotNull(com.example.crypto.BinanceCryptoCatalog.findBySymbol("BTC"))
    assertNotNull(com.example.crypto.BinanceCryptoCatalog.findBySymbol("ETH"))
    assertNotNull(com.example.crypto.BinanceCryptoCatalog.findBySymbol("BNB"))
    assertNotNull(com.example.crypto.BinanceCryptoCatalog.findBySymbol("SOL"))

    // Hourly refresh interval check (3,600,000 ms)
    val now = 10_000_000L
    assertTrue(com.example.crypto.BinanceCryptoCatalog.shouldRefreshMarketPrices(0L, now))
    org.junit.Assert.assertFalse(
      com.example.crypto.BinanceCryptoCatalog.shouldRefreshMarketPrices(now - 1_800_000L, now)
    )
    assertTrue(
      com.example.crypto.BinanceCryptoCatalog.shouldRefreshMarketPrices(now - 3_600_000L, now)
    )

    // Parse Binance 24hr payload
    val binanceJson = """
      [
        {"symbol":"XRPUSDT","lastPrice":"2.15","priceChangePercent":"4.50"},
        {"symbol":"BTCUSDT","lastPrice":"90000.00","priceChangePercent":"1.25"}
      ]
    """.trimIndent()
    val binanceParsed = com.example.crypto.BinanceCryptoCatalog.parseBinance24hrResponse(binanceJson, now)
    assertEquals(2.15, binanceParsed["XRP"]!!.priceUsd, 0.001)
    assertEquals(90000.0, binanceParsed["BTC"]!!.priceUsd, 0.01)
    assertEquals(1.0, binanceParsed["USDT"]!!.priceUsd, 0.001)

    // Parse CoinGecko fallback payload
    val cgJson = """
      {
        "ripple":{"usd":2.18,"usd_24h_change":3.2},
        "bitcoin":{"usd":91000.0,"usd_24h_change":-0.5}
      }
    """.trimIndent()
    val cgParsed = com.example.crypto.BinanceCryptoCatalog.parseCoinGeckoResponse(cgJson, now)
    assertEquals(2.18, cgParsed["XRP"]!!.priceUsd, 0.001)
    assertEquals(91000.0, cgParsed["BTC"]!!.priceUsd, 0.01)

    // Portfolio valuation: 500 XRP * 2.15 + 0.1 BTC * 90000 = 1075 + 9000 = 10075.0 USD
    val holdings = listOf(
      com.example.crypto.CryptoHoldingEntry(symbol = "XRP", quantity = 500.0),
      com.example.crypto.CryptoHoldingEntry(symbol = "BTC", quantity = 0.1)
    )
    val totalUsd = com.example.crypto.BinanceCryptoCatalog.calculatePortfolioTotalUsd(holdings, binanceParsed)
    assertEquals(10075.0, totalUsd, 0.01)

    // Codec round-trips
    val encodedHoldings = com.example.crypto.BinanceCryptoCodec.encodeHoldings(holdings)
    val decodedHoldings = com.example.crypto.BinanceCryptoCodec.parseHoldings(encodedHoldings)
    assertEquals(2, decodedHoldings.size)
    assertEquals(500.0, decodedHoldings.first { it.symbol == "XRP" }.quantity, 0.001)

    val encodedPrices = com.example.crypto.BinanceCryptoCodec.encodePrices(binanceParsed)
    val decodedPrices = com.example.crypto.BinanceCryptoCodec.parsePrices(encodedPrices)
    assertEquals(2.15, decodedPrices["XRP"]!!.priceUsd, 0.001)
  }

  @Test
  fun algerianBankCardCatalog_andThirdCardSettings_areComplete() {
    val banks = com.example.data.AlgerianBankCardCatalog.all
    assertEquals(17, banks.size)
    val expectedIds = listOf(
      "EDAHABIA", "BNA", "CPA", "BEA", "BADR", "BDL", "CNEP",
      "AGB", "SGA", "BNP", "NATIXIS", "HOUSING", "AL_BARAKA",
      "AL_SALAM", "TRUST", "ABC", "FRANSABANK"
    )
    expectedIds.forEach { id ->
      assertEquals(id, com.example.data.AlgerianBankCardCatalog.findById(id).id)
    }
    assertEquals("EDAHABIA", com.example.data.AlgerianBankCardCatalog.findById("UNKNOWN").id)

    val defaults = UserSettings()
    assertEquals("EDAHABIA", defaults.classicCardBankStyle)
    org.junit.Assert.assertFalse(defaults.thirdCardEnabled)
    assertEquals("BINANCE", defaults.thirdCardProvider)
  }

  @Test
  fun vaultKeys_pinSorting_andRealAppPackagePresets() {
    val olderUnpinned = com.example.data.VaultKeyEntry(
      id = "k_old",
      label = "GitHub",
      username = "dev",
      secretValue = "Secret#1234",
      isPinned = false,
      createdAt = 1000L,
      appPresetId = "github"
    )
    val newerUnpinned = com.example.data.VaultKeyEntry(
      id = "k_new",
      label = "Instagram",
      username = "@insta",
      secretValue = "Secret#5678",
      isPinned = false,
      createdAt = 3000L,
      appPresetId = "instagram"
    )
    val pinnedKey = com.example.data.VaultKeyEntry(
      id = "k_pin",
      label = "BaridiMob",
      username = "user",
      secretValue = "Secret#9999",
      isPinned = true,
      pinnedAt = 2000L,
      createdAt = 500L,
      appPresetId = "baridimob"
    )

    val sorted = com.example.data.HubVaultCodec.parseVaultKeys(
      com.example.data.HubVaultCodec.encodeVaultKeys(listOf(olderUnpinned, newerUnpinned, pinnedKey))
    )
    assertEquals(3, sorted.size)
    assertEquals("k_pin", sorted[0].id)
    assertTrue(sorted[0].isPinned)
    assertEquals("k_new", sorted[1].id)
    assertEquals("k_old", sorted[2].id)

    assertEquals(listOf("com.instagram.android"), com.example.data.VaultAppPresets.findById("instagram").packageNames)
    assertEquals(listOf("com.google.android.youtube"), com.example.data.VaultAppPresets.findById("youtube").packageNames)
    assertEquals(listOf("com.transferwise.android"), com.example.data.VaultAppPresets.findById("wise").packageNames)
    assertEquals(listOf("com.linkedin.android"), com.example.data.VaultAppPresets.findById("linkedin").packageNames)
    assertTrue(com.example.data.VaultAppPresets.findById("baridimob").packageNames.contains("dz.poste.baridimob"))
  }

  @Test
  fun squareMarket_goldAllKarats_bankVsParallelCurrencies_hourlySync_andCodec() {
    // 1. Hourly refresh check (1h = 3,600,000 ms)
    val now = 20_000_000L
    assertTrue(com.example.market.SquareMarketCatalog.shouldRefresh(0L, now))
    org.junit.Assert.assertFalse(
      com.example.market.SquareMarketCatalog.shouldRefresh(now - 1_800_000L, now)
    )
    assertTrue(
      com.example.market.SquareMarketCatalog.shouldRefresh(now - 3_600_000L, now)
    )

    // 2. Gold all karats (24K, 22K, 21K, 18K, 14K, 9K) in USD & DZD (Parallel + Bank)
    val goldOzUsd = 3110.34768 // exactly $100/g for 24K
    val karats = com.example.market.SquareMarketCatalog.buildGoldKarats(
      goldOzUsd = goldOzUsd,
      usdBankDzd = 135.0,
      usdParallelDzd = 240.0
    )
    assertEquals(listOf(24, 22, 21, 18, 14, 9), karats.map { it.karat })

    val k24 = karats.first { it.karat == 24 }
    assertEquals(100.0, k24.pricePerGramUsd, 0.01)
    assertEquals(24000.0, k24.pricePerGramDzdParallel, 0.1)
    assertEquals(13500.0, k24.pricePerGramDzdBank, 0.1)

    val k18 = karats.first { it.karat == 18 }
    assertEquals(75.0, k18.pricePerGramUsd, 0.01)
    assertEquals(18000.0, k18.pricePerGramDzdParallel, 0.1)
    assertEquals(10125.0, k18.pricePerGramDzdBank, 0.1)

    // 3. Spot Gold & FX parsers
    val goldQuote = com.example.market.SquareMarketCatalog.parseGoldApiResponse(
      """{"price":2650.5,"chp":1.25}"""
    )
    assertNotNull(goldQuote)
    assertEquals(2650.5, goldQuote!!.priceOzUsd, 0.01)

    val paxgQuote = com.example.market.SquareMarketCatalog.parseBinancePaxgResponse(
      """{"lastPrice":"2660.00","priceChangePercent":"-0.45"}"""
    )
    assertNotNull(paxgQuote)
    assertEquals(2660.0, paxgQuote!!.priceOzUsd, 0.01)

    val bankRates = com.example.market.SquareMarketCatalog.parseOpenErRatesResponse(
      """{"result":"success","rates":{"DZD":135.0,"EUR":0.90,"GBP":0.75,"CAD":1.35}}"""
    )
    assertEquals(135.0, bankRates["USD"]!!, 0.01)
    assertEquals(150.0, bankRates["EUR"]!!, 0.01)

    val fawazRates = com.example.market.SquareMarketCatalog.parseFawazCurrencyResponse(
      """{"usd":{"dzd":134.0,"eur":0.92}}"""
    )
    assertEquals(134.0, fawazRates["USD"]!!, 0.01)

    val floatRates = com.example.market.SquareMarketCatalog.parseFloatRatesResponse(
      """{"dzd":{"rate":134.5},"eur":{"rate":0.91}}"""
    )
    assertEquals(134.5, floatRates["USD"]!!, 0.01)

    // Multi-site official bank consensus
    val bankConsensus = com.example.market.SquareMarketCatalog.computeBankRatesConsensus(
      listOf(
        "Open ER-API" to bankRates,
        "Currency-API" to fawazRates,
        "FloatRates" to floatRates
      )
    )
    assertNotNull(bankConsensus)
    assertEquals(134.5, bankConsensus!!.first["USD"]!!, 0.01)
    assertEquals(3, bankConsensus.third)

    // Multi-site gold consensus rejects rogue outlier (4999.0)
    val yahooGold = com.example.market.SquareMarketCatalog.parseYahooGoldChartResponse(
      """{"chart":{"result":[{"meta":{"regularMarketPrice":2655.0,"chartPreviousClose":2640.0}}]}}"""
    )
    assertNotNull(yahooGold)
    val xauFawaz = com.example.market.SquareMarketCatalog.parseFawazXauResponse(
      """{"xau":{"usd":2652.0}}"""
    )
    assertNotNull(xauFawaz)
    val rogueGold = com.example.market.GoldSpotQuote(4999.0, 20.0, "RogueSite")
    val goldConsensus = com.example.market.SquareMarketCatalog.computeGoldConsensus(
      listOf(goldQuote, paxgQuote, yahooGold!!, xauFawaz!!, rogueGold)
    )
    assertNotNull(goldConsensus)
    assertEquals(2653.5, goldConsensus!!.first.priceOzUsd, 3.0)
    assertEquals(4, goldConsensus.second)

    val p2pMedian = com.example.market.SquareMarketCatalog.parseBinanceP2pMedianDzd(
      """{"data":[{"adv":{"price":"238.0","tradableQuantity":"100","minSingleTransAmount":"5000"}},{"adv":{"price":"240.0","tradableQuantity":"200","minSingleTransAmount":"5000"}},{"adv":{"price":"242.0","tradableQuantity":"150","minSingleTransAmount":"5000"}}]}"""
    )
    assertEquals(240.0, p2pMedian!!, 0.01)

    // Row-scoped HTML parser ignores French prose traps ("heure", "meilleur", "vendeur", "country", "cadre")
    // and parses 100-unit spaced quotes ("25 400" -> 254.0) accurately.
    val trickyHtml = """
      <header><div>Mise à jour il y a 1 heure - meilleur taux vendeur country cadre 2026 - 199.00</div></header>
      <table>
        <tr><th>Devise</th><th>EUR</th><th>USD</th></tr>
        <tr><td>EUR Euro (100 EUR)</td><td>25 400</td><td>25 700</td></tr>
        <tr><td>USD Dollar US</td><td>236.50</td><td>239.00</td></tr>
      </table>
    """.trimIndent()
    val htmlQuotes = com.example.market.SquareMarketCatalog.parseSquareHtmlQuotes(
      trickyHtml,
      bankRates,
      sourceLabel = "DeviseSquare"
    )
    assertEquals(254.0, htmlQuotes["EUR"]!!.buyDzd, 0.01)
    assertEquals(257.0, htmlQuotes["EUR"]!!.sellDzd, 0.01)
    assertEquals(236.5, htmlQuotes["USD"]!!.buyDzd, 0.01)
    assertEquals(239.0, htmlQuotes["USD"]!!.sellDzd, 0.01)
    org.junit.Assert.assertFalse(htmlQuotes.containsKey("TRY"))
    org.junit.Assert.assertFalse(htmlQuotes.containsKey("CAD"))

    // 4. Moshi Snapshot round-trip
    val snapshot = com.example.market.SquareMarketSnapshot(
      version = com.example.market.SQUARE_SNAPSHOT_VERSION,
      goldOzUsd = goldOzUsd,
      gold24hChangePercent = 1.15,
      goldSource = goldConsensus.first.source,
      bankSource = bankConsensus.second,
      parallelSource = "2 sites (DeviseSquare, Binance P2P)",
      goldSourceCount = goldConsensus.second,
      bankSourceCount = bankConsensus.third,
      parallelSourceCount = 2,
      goldKarats = karats,
      currencies = listOf(
        com.example.market.CurrencySquareRate(
          code = "EUR",
          nameEn = "Euro",
          nameAr = "اليورو الأوروبي",
          flag = "🇪🇺",
          symbol = "€",
          bankRateDzd = 150.0,
          parallelBuyDzd = 254.0,
          parallelSellDzd = 257.0,
          parallelSource = "DeviseSquare",
          sourceCount = 2
        )
      ),
      updatedAtMillis = now
    )
    val decoded = com.example.market.SquareMarketCodec.parseSnapshot(
      com.example.market.SquareMarketCodec.encodeSnapshot(snapshot)
    )
    assertNotNull(decoded)
    assertEquals(com.example.market.SQUARE_SNAPSHOT_VERSION, decoded!!.version)
    assertEquals(6, decoded.goldKarats.size)
    assertEquals("EUR", decoded.currencies[0].code)
    assertEquals(257.0, decoded.currencies[0].parallelSellDzd, 0.01)
  }

  @Test
  fun algPosteBalanceOperations_suggestsIncomeAndOutcome_andSupportsDismiss() {
    assertTrue(UserSettings().baridiMobSmsSyncEnabled)

    val snapBase = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Nouveau solde: 20 000,00 DA",
      timestampMillis = 1_000_000L
    )!!
    // Balance-only SMS showing balance INCREASE (+8,500 DA) -> Income operation
    val snapIncrease = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Votre solde disponible est de 28 500,00 DA",
      timestampMillis = 2_000_000L
    )!!
    // Balance-only SMS showing balance DECREASE (-3,300 DA) -> Outcome / Expense operation
    val snapDecrease = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Votre nouveau solde est de 25 200,00 DA",
      timestampMillis = 3_000_000L
    )!!
    // Explicit SMS operation with previous balance attached
    val snapExplicitDebit = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Paiement TPE -1 200,00 DA. Nouveau solde: 24 000,00 DA",
      timestampMillis = 4_000_000L
    )!!

    val ops = com.example.sms.AlgPosteSmsAnalyzer.detectBalanceOperationSuggestions(
      snapshots = listOf(snapBase, snapIncrease, snapDecrease, snapExplicitDebit)
    )

    assertEquals(3, ops.size)

    val explicitOp = ops[0]
    assertEquals(1200.0, explicitOp.amount, 0.01)
    assertTrue(explicitOp.isExpense)
    assertEquals(25200.0, explicitOp.previousBalance!!, 0.01)
    assertEquals(24000.0, explicitOp.extractedBalance!!, 0.01)

    val decreaseOp = ops[1]
    assertEquals("bal_op_${snapDecrease.id}", decreaseOp.id)
    assertEquals(3300.0, decreaseOp.amount, 0.01)
    assertTrue("Balance decrease must suggest Outcome/Expense", decreaseOp.isExpense)
    assertEquals(28500.0, decreaseOp.previousBalance!!, 0.01)
    assertEquals(25200.0, decreaseOp.extractedBalance!!, 0.01)

    val increaseOp = ops[2]
    assertEquals("bal_op_${snapIncrease.id}", increaseOp.id)
    assertEquals(8500.0, increaseOp.amount, 0.01)
    org.junit.Assert.assertFalse("Balance increase must suggest Income", increaseOp.isExpense)
    assertEquals(20000.0, increaseOp.previousBalance!!, 0.01)
    assertEquals(28500.0, increaseOp.extractedBalance!!, 0.01)

    // Codec round-trip preserves previousBalance
    val encoded = com.example.sms.AlgPosteSmsCodec.encodeSuggestions(ops)
    val decoded = com.example.sms.AlgPosteSmsCodec.parseSuggestions(encoded)
    assertEquals(3, decoded.size)
    assertEquals(28500.0, decoded[1].previousBalance!!, 0.01)
  }

  @Test
  fun squareTicker_filteringSortingAndPinning_workAccurately() {
    val defaultPins = com.example.market.SquareMarketCodec.parsePinnedIds("")
    assertTrue(defaultPins.contains("XAU_SPOT"))
    assertTrue(defaultPins.contains("EUR"))

    val customPins = setOf("XAU_18K", "USD")
    val encodedPins = com.example.market.SquareMarketCodec.encodePinnedIds(customPins)
    assertEquals(customPins, com.example.market.SquareMarketCodec.parsePinnedIds(encodedPins))

    val karats = com.example.market.SquareMarketCatalog.buildGoldKarats(
      goldOzUsd = 3869.91,
      usdBankDzd = 135.0,
      usdParallelDzd = 240.0
    )
    val currencies = listOf(
      com.example.market.CurrencySquareRate("EUR", "Euro", "اليورو", "🇪🇺", "€", 145.0, 260.0, 263.0, "DeviseSquare", 3),
      com.example.market.CurrencySquareRate("USD", "US Dollar", "الدولار", "🇺🇸", "$", 135.0, 238.0, 240.0, "Binance P2P", 2)
    )
    val snapshot = com.example.market.SquareMarketSnapshot(
      goldOzUsd = 3869.91,
      gold24hChangePercent = 0.14,
      goldSource = "GoldPrice",
      goldSourceCount = 3,
      bankSourceCount = 2,
      parallelSourceCount = 3,
      goldKarats = karats,
      currencies = currencies
    )

    val allRows = com.example.ui.components.buildSquareTickerRows(
      snapshot = snapshot,
      primaryTab = com.example.ui.components.SquarePrimaryTab.ALL,
      venueFilter = com.example.ui.components.SquareVenueFilter.ALL,
      pinnedIds = customPins,
      searchQuery = ""
    )
    assertEquals(1 + karats.size + currencies.size, allRows.size)

    val favRows = com.example.ui.components.buildSquareTickerRows(
      snapshot = snapshot,
      primaryTab = com.example.ui.components.SquarePrimaryTab.FAVORITES,
      venueFilter = com.example.ui.components.SquareVenueFilter.ALL,
      pinnedIds = customPins,
      searchQuery = ""
    )
    assertEquals(2, favRows.size)

    val sortedDesc = com.example.ui.components.sortSquareTickerRows(
      rows = allRows,
      sortColumn = com.example.ui.components.SquareSortColumn.PRICE,
      sortDirection = com.example.ui.components.SquareSortDirection.DESC,
      pinnedIds = customPins
    )
    assertTrue(sortedDesc.first().primaryNumericPrice >= sortedDesc.last().primaryNumericPrice)
  }

  @Test
  fun algPosteNegativeBalance_extractsStoresAndPreservesSign() {
    val frNeg = com.example.sms.AlgPosteSmsAnalyzer.extractBalanceFromBody(
      "Retrait GAB -5 000,00 DA. Nouveau solde: -1 500,00 DA"
    )
    assertNotNull(frNeg)
    assertEquals(-1500.0, frNeg!!, 0.01)

    val frGlued = com.example.sms.AlgPosteSmsAnalyzer.extractBalanceFromBody(
      "Solde:-250.50 DA"
    )
    assertNotNull(frGlued)
    assertEquals(-250.50, frGlued!!, 0.01)

    val arBidiNeg = com.example.sms.AlgPosteSmsAnalyzer.extractBalanceFromBody(
      "الرصيد الجديد: \u200E-\u200F500.00 دج"
    )
    assertNotNull(arBidiNeg)
    assertEquals(-500.0, arBidiNeg!!, 0.01)

    // Balance-only negative SMS must NOT be misparsed as an expense transaction
    val negBalanceOnly = com.example.sms.AlgPosteSmsAnalyzer.parseSms(
      sender = "ALG Poste",
      body = "Votre nouveau solde est de -1 500,00 DA",
      timestampMillis = 1_000_000L
    )
    org.junit.Assert.assertNull(negBalanceOnly)

    val snapPos = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Nouveau solde: 500,00 DA",
      timestampMillis = 1_000_000L
    )!!
    val snapNeg = com.example.sms.AlgPosteSmsAnalyzer.parseBalanceSnapshot(
      sender = "ALG Poste",
      body = "Retrait GAB -2 000,00 DA. Nouveau solde: -1 520,00 DA",
      timestampMillis = 2_000_000L
    )!!
    assertEquals(-1520.0, snapNeg.balance, 0.01)

    // Codec must preserve negative balances in history
    val encoded = com.example.sms.AlgPosteSmsCodec.encodeBalanceHistory(listOf(snapPos, snapNeg))
    val decoded = com.example.sms.AlgPosteSmsCodec.parseBalanceHistory(encoded)
    assertEquals(2, decoded.size)
    assertEquals(-1520.0, decoded[1].balance, 0.01)

    // Tax anomaly detection still catches 20 DA discrepancy across positive -> negative transition
    val taxes = com.example.sms.AlgPosteSmsAnalyzer.detectTaxAnomalies(decoded)
    assertEquals(1, taxes.size)
    assertEquals(20.0, taxes[0].taxAmount, 0.01)
  }

  @Test
  fun square24hChange_storesPreviousDayCloseAndComputesRiseDropPercent() {
    val utc = java.util.TimeZone.getTimeZone("UTC")
    val cal = java.util.Calendar.getInstance(utc).apply {
      set(2026, java.util.Calendar.NOVEMBER, 17, 10, 0, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }
    val day1Morning = cal.timeInMillis
    val day1Evening = day1Morning + 11 * 3_600_000L // 21:00 Nov 17
    val day2Morning = day1Morning + 24 * 3_600_000L // 10:00 Nov 18
    val day2Afternoon = day2Morning + 4 * 3_600_000L // 14:00 Nov 18
    val day3Morning = day1Morning + 48 * 3_600_000L // 10:00 Nov 19

    fun makeSnapshot(goldOz: Double, eurSell: Double, eurBank: Double, usdSell: Double, usdBank: Double, ts: Long): com.example.market.SquareMarketSnapshot {
      return com.example.market.SquareMarketSnapshot(
        goldOzUsd = goldOz,
        gold24hChangePercent = 0.0,
        goldKarats = com.example.market.SquareMarketCatalog.buildGoldKarats(goldOz, usdBank, usdSell),
        currencies = listOf(
          com.example.market.CurrencySquareRate("EUR", "Euro", "اليورو", "🇪🇺", "€", eurBank, eurSell - 2.0, eurSell, "Test", 1),
          com.example.market.CurrencySquareRate("USD", "US Dollar", "الدولار", "🇺🇸", "$", usdBank, usdSell - 2.0, usdSell, "Test", 1)
        ),
        updatedAtMillis = ts
      )
    }

    // Day 1 Morning: EUR=250.0, GoldOz=3000.0
    val (s1m, base1m) = com.example.market.SquareMarketCatalog.apply24hPriceComparison(
      fetched = makeSnapshot(3000.0, 250.0, 140.0, 230.0, 134.0, day1Morning),
      storedBaselineJson = "",
      nowMillis = day1Morning,
      timeZone = utc
    )
    assertEquals(0.0, s1m.currencies[0].change24hParallelPercent, 0.001)

    // Day 1 Evening closing price: EUR exits Day 1 at 252.0, GoldOz exits at 3060.0
    val (_, base1e) = com.example.market.SquareMarketCatalog.apply24hPriceComparison(
      fetched = makeSnapshot(3060.0, 252.0, 142.0, 232.0, 134.0, day1Evening),
      storedBaselineJson = base1m,
      nowMillis = day1Evening,
      timeZone = utc
    )

    // Day 2 Morning: EUR rises to 258.3 vs Day 1 closing price 252.0 => +2.50%
    // Bank EUR drops to 139.16 vs Day 1 closing 142.0 => -2.00%
    val (s2m, base2m) = com.example.market.SquareMarketCatalog.apply24hPriceComparison(
      fetched = makeSnapshot(3090.6, 258.3, 139.16, 234.0, 134.0, day2Morning),
      storedBaselineJson = base1e,
      nowMillis = day2Morning,
      timeZone = utc
    )
    assertEquals("2026-11-17", s2m.baselinePreviousDateKey)
    assertEquals(252.0, s2m.currencies.first { it.code == "EUR" }.previousParallelSellDzd, 0.01)
    assertEquals(2.50, s2m.currencies.first { it.code == "EUR" }.change24hParallelPercent, 0.01)
    assertEquals(-2.00, s2m.currencies.first { it.code == "EUR" }.change24hBankPercent, 0.01)
    assertEquals(1.00, s2m.gold24hChangePercent, 0.01)
    assertTrue(s2m.goldKarats.first { it.karat == 18 }.change24hParallelPercent > 0.0)

    // Day 2 Afternoon (same day): EUR drops to 249.48 -> MUST still compare vs Day 1 close (252.0) => -1.00%
    val (s2a, base2a) = com.example.market.SquareMarketCatalog.apply24hPriceComparison(
      fetched = makeSnapshot(3029.4, 249.48, 140.0, 230.0, 134.0, day2Afternoon),
      storedBaselineJson = base2m,
      nowMillis = day2Afternoon,
      timeZone = utc
    )
    assertEquals(-1.00, s2a.currencies.first { it.code == "EUR" }.change24hParallelPercent, 0.01)
    assertEquals(-1.00, s2a.gold24hChangePercent, 0.01)

    // Check UI ticker rows reflect true 24h change (NOT spread) for both Parallel and Bank venues
    val parallelRows = com.example.ui.components.buildSquareTickerRows(
      snapshot = s2a,
      primaryTab = com.example.ui.components.SquarePrimaryTab.CURRENCIES,
      venueFilter = com.example.ui.components.SquareVenueFilter.SQUARE,
      pinnedIds = emptySet(),
      searchQuery = ""
    )
    assertEquals(-1.00, parallelRows.first { it.id == "EUR" }.changeOrSpreadValue, 0.01)

    // Day 3 Morning: compares against Day 2's final exit price (EUR=249.48)
    val (s3m, _) = com.example.market.SquareMarketCatalog.apply24hPriceComparison(
      fetched = makeSnapshot(3059.694, 254.4696, 140.0, 230.0, 134.0, day3Morning),
      storedBaselineJson = base2a,
      nowMillis = day3Morning,
      timeZone = utc
    )
    assertEquals("2026-11-18", s3m.baselinePreviousDateKey)
    assertEquals(249.48, s3m.currencies.first { it.code == "EUR" }.previousParallelSellDzd, 0.01)
    assertEquals(2.00, s3m.currencies.first { it.code == "EUR" }.change24hParallelPercent, 0.02)
  }

  @Test
  fun squareSyncInterval_respectsCustomMinutesAndManualOnlyMode() {
    val now = 1_763_200_000_000L
    // Default UserSettings is 60 minutes (1h)
    assertEquals(60, UserSettings().squareSyncIntervalMinutes)

    // Initial sync (lastSync == 0) always triggers refresh regardless of interval
    assertTrue(com.example.market.SquareMarketCatalog.shouldRefresh(0L, now, intervalMinutes = 0))
    assertTrue(com.example.market.SquareMarketCatalog.shouldRefresh(0L, now, intervalMinutes = 15))

    // 5-minute interval
    val fourMinsAgo = now - 4 * 60_000L
    val fiveMinsAgo = now - 5 * 60_000L
    org.junit.Assert.assertFalse(
      com.example.market.SquareMarketCatalog.shouldRefresh(fourMinsAgo, now, intervalMinutes = 5)
    )
    assertTrue(
      com.example.market.SquareMarketCatalog.shouldRefresh(fiveMinsAgo, now, intervalMinutes = 5)
    )

    // Manual-only (0 minutes) never auto-refreshes once synced once
    val tenHoursAgo = now - 10 * 3_600_000L
    org.junit.Assert.assertFalse(
      com.example.market.SquareMarketCatalog.shouldRefresh(tenHoursAgo, now, intervalMinutes = 0)
    )
  }

  @Test
  fun algPosteSmsAnalyzer_computesCurrentYearTaxesSinceJanFirst() {
    val utc = java.util.TimeZone.getTimeZone("UTC")
    fun utcMillis(year: Int, month: Int, day: Int): Long {
      return java.util.Calendar.getInstance(utc).apply {
        clear()
        set(year, month, day, 12, 0, 0)
        set(java.util.Calendar.MILLISECOND, 0)
      }.timeInMillis
    }

    val decLastYear = utcMillis(2025, java.util.Calendar.DECEMBER, 31)
    val jan1Current = utcMillis(2026, java.util.Calendar.JANUARY, 1)
    val novCurrent = utcMillis(2026, java.util.Calendar.NOVEMBER, 19)

    val taxes = listOf(
      com.example.sms.SmsTaxAnomalyEntry(
        id = "t_old",
        taxAmount = 500.0,
        detectedAtMillis = decLastYear,
        isRecordedAsExpense = true
      ),
      com.example.sms.SmsTaxAnomalyEntry(
        id = "t_jan",
        taxAmount = 30.0,
        detectedAtMillis = jan1Current,
        isRecordedAsExpense = true
      ),
      com.example.sms.SmsTaxAnomalyEntry(
        id = "t_nov",
        taxAmount = 120.5,
        detectedAtMillis = novCurrent,
        isRecordedAsExpense = false
      )
    )

    val summary = com.example.sms.AlgPosteSmsAnalyzer.computeAnnualTaxesSummary(
      taxes = taxes,
      referenceNowMillis = novCurrent,
      timeZone = utc
    )

    assertEquals(2026, summary.year)
    assertEquals("1.1.2026", summary.referenceStartDateLabel)
    assertEquals(2, summary.yearTaxCount)
    assertEquals(150.5, summary.totalYearTaxesDzd, 0.01)
    assertEquals(30.0, summary.recordedYearTaxesDzd, 0.01)
    assertEquals(120.5, summary.unrecordedYearTaxesDzd, 0.01)
  }

  @Test
  fun goldAssetCodec_andValuationEngine_computeLiveSquareWorthAccurately() {
    val karats = com.example.market.SquareMarketCatalog.buildGoldKarats(
      goldOzUsd = 3110.34768, // exact 100.0 USD / gram for 24K
      usdBankDzd = 135.0,
      usdParallelDzd = 240.0
    )
    val snapshot = com.example.market.SquareMarketSnapshot(
      goldOzUsd = 3110.34768,
      goldKarats = karats,
      currencies = listOf(
        com.example.market.CurrencySquareRate(
          code = "USD",
          nameEn = "US Dollar",
          nameAr = "دولار",
          flag = "🇺🇸",
          symbol = "$",
          bankRateDzd = 135.0,
          parallelBuyDzd = 238.0,
          parallelSellDzd = 240.0
        )
      )
    )

    val holdings = listOf(
      com.example.market.GoldAssetHolding(id = "g1", karat = 24, grams = 2.0, label = "24K Bar"),
      com.example.market.GoldAssetHolding(id = "g2", karat = 18, grams = 10.0, label = "Ring"),
      com.example.market.GoldAssetHolding(id = "g_bad", karat = 18, grams = 0.0)
    )
    val encoded = com.example.market.GoldAssetCodec.encodeHoldings(holdings)
    val decoded = com.example.market.GoldAssetCodec.parseHoldings(encoded)
    assertEquals(2, decoded.size)

    val eval = com.example.market.GoldValuationEngine.evaluatePortfolio(decoded, snapshot)
    // 24K: 2g * 100 USD = 200 USD * 240 = 48,000 DZD
    // 18K: 10g * 75 USD = 750 USD * 240 = 180,000 DZD
    assertTrue(eval.hasLivePrices)
    assertEquals(12.0, eval.totalGrams, 0.001)
    assertEquals(950.0, eval.totalValueUsd, 0.01)
    assertEquals(228_000.0, eval.totalValueDzdParallel, 1.0)

    val cryptoDzd = com.example.market.GoldValuationEngine.convertUsdToSquareParallelDzd(50.0, snapshot)
    assertNotNull(cryptoDzd)
    assertEquals(12_000.0, cryptoDzd!!, 0.01)
  }

  @Test
  fun releaseChangelogProvider_parsesLatestOnlyJsonAndMarkdownStrippingBoilerplate() {
    val embeddedBody = """
      ### ✨ What's New in Vaultix (v1.0.9)
      - **Assets & Saving** — Gold & Crypto sync added
      - **Developer:** Charfeddine Ka
      - **Version:** 1.0.9 (Build 9)
      <!-- WHATS_NEW_JSON:{"versionTag":"v1.0.9","features":[{"titleAr":"ميزة الأصول","titleEn":"Assets Vault","descAr":"تخزين الذهب","descEn":"Store Gold","badge":"New"}]} -->
    """.trimIndent()

    val parsedEmbedded = com.example.update.ReleaseChangelogProvider.parseRemoteReleaseNotes(embeddedBody)
    assertNotNull(parsedEmbedded)
    assertEquals("v1.0.9", parsedEmbedded!!.versionTag)
    assertEquals(1, parsedEmbedded.features.size)
    assertEquals("ميزة الأصول", parsedEmbedded.features[0].titleAr)

    val markdownOnly = """
      ### Release v1.0.10
      - **Gold Sync** — Live valuation from Square
      - **Developer:** Charfeddine Ka
      - **Package:** Vaultix.apk
    """.trimIndent()
    val parsedMd = com.example.update.ReleaseChangelogProvider.parseRemoteReleaseNotes(markdownOnly)
    assertNotNull(parsedMd)
    assertEquals(1, parsedMd!!.features.size)
    assertEquals("Gold Sync", parsedMd.features[0].titleEn)
    assertEquals("Live valuation from Square", parsedMd.features[0].descEn)
  }

  @Test
  fun autoAddTaxes_deduplicatesLoggedRefTagsAndLegacyEntries_withoutDuplicates() {
    val t1 = 1_763_500_000_000L
    val t2 = 1_763_500_100_000L
    val t3 = 1_763_500_200_000L
    val t4 = 1_763_500_300_000L

    val anomalies = listOf(
      com.example.sms.SmsTaxAnomalyEntry(
        id = "tax_flagged",
        taxAmount = 30.0,
        detectedAtMillis = t1,
        isRecordedAsExpense = true
      ),
      com.example.sms.SmsTaxAnomalyEntry(
        id = "tax_ref_logged",
        taxAmount = 50.0,
        detectedAtMillis = t2,
        isRecordedAsExpense = false
      ),
      com.example.sms.SmsTaxAnomalyEntry(
        id = "tax_legacy_logged",
        taxAmount = 120.0,
        detectedAtMillis = t3,
        isRecordedAsExpense = false
      ),
      com.example.sms.SmsTaxAnomalyEntry(
        id = "tax_unlogged_valid",
        taxAmount = 75.5,
        detectedAtMillis = t4,
        isRecordedAsExpense = false
      ),
      com.example.sms.SmsTaxAnomalyEntry(
        id = "tax_unlogged_valid",
        taxAmount = 75.5,
        detectedAtMillis = t4,
        isRecordedAsExpense = false
      ),
      com.example.sms.SmsTaxAnomalyEntry(
        id = "tax_over_cap",
        taxAmount = 1500.0,
        detectedAtMillis = t4 + 10_000L,
        isRecordedAsExpense = false
      )
    )

    val existingTransactions = listOf(
      com.example.data.TransactionEntity(
        id = 1,
        title = "SMS Fee",
        amount = 50.0,
        type = TransactionType.EXPENSE,
        category = "taxes_fees",
        note = "Auto fee [tax_ref:tax_ref_logged]",
        dateMillis = t2 + 20_000L
      ),
      com.example.data.TransactionEntity(
        id = 2,
        title = "Legacy Fee",
        amount = 120.0,
        type = TransactionType.EXPENSE,
        category = "taxes_fees",
        note = "BaridiMob deduction",
        dateMillis = t3 + 1_500L
      )
    )

    val unlogged = com.example.viewmodel.findUnloggedTaxAnomaliesForExpenses(
      anomalies = anomalies,
      existingTransactions = existingTransactions
    )

    assertEquals(1, unlogged.size)
    assertEquals("tax_unlogged_valid", unlogged[0].id)
    assertEquals(75.5, unlogged[0].taxAmount, 0.001)
  }

  @Test
  fun deleteTransaction_refundsSavingsWalletOnlyForPaidFromSavingsExpense() {
    val savingsExpense = com.example.data.TransactionEntity(
      id = 10,
      title = "Emergency",
      amount = 4500.0,
      type = TransactionType.EXPENSE,
      category = "other",
      dateMillis = 1_763_500_000_000L,
      isPaidFromSavings = true
    )
    assertEquals(
      14_500.0,
      com.example.viewmodel.computeSavingsBalanceAfterTransactionDelete(savingsExpense, 10_000.0),
      0.001
    )

    val normalExpense = savingsExpense.copy(id = 11, isPaidFromSavings = false)
    assertEquals(
      10_000.0,
      com.example.viewmodel.computeSavingsBalanceAfterTransactionDelete(normalExpense, 10_000.0),
      0.001
    )

    val incomeTx = savingsExpense.copy(id = 12, type = TransactionType.INCOME, isPaidFromSavings = true)
    assertEquals(
      10_000.0,
      com.example.viewmodel.computeSavingsBalanceAfterTransactionDelete(incomeTx, 10_000.0),
      0.001
    )
  }

  @Test
  fun localizedStrings_includeTaxesFeesCategoryAndHalfSecondHoldHint() {
    assertEquals("Taxes & Fees", EnglishStrings.categoryName("taxes_fees"))
    assertEquals("ضرائب ورسوم", ArabicStrings.categoryName("taxes_fees"))
    assertTrue(EnglishStrings.holdToEditCardHint.contains("0.5s"))
    assertTrue(
      ArabicStrings.holdToEditCardHint.contains("نصف ثانية") ||
        ArabicStrings.holdToEditCardHint.contains("0.5")
    )
  }

  @Test
  fun autoAddTaxes_defaultsOffAndStrictlySkipsPastTaxesWhenBaselineProvided() {
    val defaults = com.example.data.UserSettings()
    assertEquals(false, defaults.autoAddTaxesEnabled)
    assertEquals(0L, defaults.autoAddTaxesBaselineMillis)

    val baseline = 1_765_000_000_000L
    val pastTax = com.example.sms.SmsTaxAnomalyEntry(
      id = "tax_past",
      taxAmount = 35.0,
      detectedAtMillis = baseline - 60_000L,
      isRecordedAsExpense = false
    )
    val futureTaxAtBoundary = com.example.sms.SmsTaxAnomalyEntry(
      id = "tax_future_boundary",
      taxAmount = 40.0,
      detectedAtMillis = baseline,
      isRecordedAsExpense = false
    )
    val futureTaxLater = com.example.sms.SmsTaxAnomalyEntry(
      id = "tax_future_later",
      taxAmount = 85.0,
      detectedAtMillis = baseline + 120_000L,
      isRecordedAsExpense = false
    )

    val eligible = com.example.viewmodel.findUnloggedTaxAnomaliesForExpenses(
      anomalies = listOf(pastTax, futureTaxAtBoundary, futureTaxLater),
      existingTransactions = emptyList(),
      minTimestampMillis = baseline
    )

    assertEquals(2, eligible.size)
    assertTrue(eligible.none { it.id == "tax_past" })
    assertEquals(listOf("tax_future_boundary", "tax_future_later"), eligible.map { it.id })
  }

  @Test
  fun arabicLocalization_preservesIdentityAndEliminatesLatinLeaks() {
    assertEquals("Vaultix", APP_NAME)
    assertEquals("Charfeddine Ka", DEVELOPER_NAME)
    assertEquals("Vaultix", ArabicStrings.appName)
    assertEquals("Vaultix", EnglishStrings.appName)

    assertEquals("د.ج", ArabicStrings.currencyCode)
    assertEquals("\u206612,500 د.ج\u2069", isolateLtr("12,500 د.ج"))
    assertEquals("45٪", formatPercent(45, isArabic = true))
    assertEquals("45%", formatPercent(45, isArabic = false))
    assertTrue(formatDate(1_765_000_000_000L, isArabic = true).isNotBlank())

    val forbiddenAcronyms = listOf("DZD", "USD", "CCP", "SMS", "CVC", "EXP", "JSON", "Google Drive", "BaridiMob", "Binance", "Square")
    val auditedArabicStrings = listOf(
      ArabicStrings.currencyCode,
      ArabicStrings.currentAccount,
      ArabicStrings.cardCvcLabel,
      ArabicStrings.salaryAmount,
      ArabicStrings.monthlyInstallmentAmount,
      ArabicStrings.partialPaymentAmount,
      ArabicStrings.amount,
      ArabicStrings.backupDriveTitle,
      ArabicStrings.backupToDrive,
      ArabicStrings.restoreFromDrive,
      ArabicStrings.restoreError,
      ArabicStrings.pasteJsonFallback,
      ArabicStrings.visaComingSoonDesc
    )
    auditedArabicStrings.forEach { value ->
      forbiddenAcronyms.forEach { token ->
        assertTrue("Unexpected Latin token '$token' in Arabic string '$value'", !value.contains(token))
      }
    }
    assertTrue(ArabicStrings.visaComingSoonDesc.contains("Vaultix"))
    assertTrue(ArabicStrings.unlockApp.contains("Vaultix"))
  }

  @Test
  fun squareMarket_fiveNewMajorSources_parsersAndCodec_workCorrectly() {
    val allSources = com.example.market.SquareMarketCatalog.availableSources
    assertEquals(18, allSources.size)

    val newMajorIds = allSources.filter { it.isNewMajor }.map { it.id }.toSet()
    assertEquals(
      setOf(
        com.example.market.SquareMarketCatalog.SOURCE_KRAKEN_GOLD,
        com.example.market.SquareMarketCatalog.SOURCE_EXCHANGERATE_API_V4,
        com.example.market.SquareMarketCatalog.SOURCE_FXRATES_API,
        com.example.market.SquareMarketCatalog.SOURCE_OKX_MARKET,
        com.example.market.SquareMarketCatalog.SOURCE_BYBIT_MARKET
      ),
      newMajorIds
    )

    // Default blank JSON returns all sources; custom subset round-trips cleanly
    assertEquals(
      com.example.market.SquareMarketCatalog.ALL_SOURCE_IDS,
      com.example.market.SquareMarketCodec.parseEnabledSourceIds("")
    )
    val customSubset = setOf(
      com.example.market.SquareMarketCatalog.SOURCE_KRAKEN_GOLD,
      com.example.market.SquareMarketCatalog.SOURCE_OKX_MARKET
    )
    val encodedSubset = com.example.market.SquareMarketCodec.encodeEnabledSourceIds(customSubset)
    assertEquals(customSubset, com.example.market.SquareMarketCodec.parseEnabledSourceIds(encodedSubset))

    // Kraken Gold spot parser
    val krakenJson = """{"error":[],"result":{"PAXGUSD":{"c":["2940.50","0.2"],"o":"2900.00"}}}"""
    val krakenQuote = com.example.market.SquareMarketCatalog.parseKrakenPaxgResponse(krakenJson)
    assertNotNull(krakenQuote)
    assertEquals(2940.50, krakenQuote!!.priceOzUsd, 0.01)
    assertEquals("Kraken", krakenQuote.source)

    // Bybit PAXG spot parser
    val bybitGoldJson = """{"retCode":0,"result":{"list":[{"lastPrice":"2935.0","price24hPcnt":"0.015"}]}}"""
    val bybitQuote = com.example.market.SquareMarketCatalog.parseBybitPaxgResponse(bybitGoldJson)
    assertNotNull(bybitQuote)
    assertEquals(2935.0, bybitQuote!!.priceOzUsd, 0.01)
    assertEquals(1.5, bybitQuote.change24hPercent, 0.01)

    // OKX XAUT spot parser
    val okxGoldJson = """{"code":"0","data":[{"last":"2938.2","open24h":"2910.0"}]}"""
    val okxQuote = com.example.market.SquareMarketCatalog.parseOkxXautResponse(okxGoldJson)
    assertNotNull(okxQuote)
    assertEquals(2938.2, okxQuote!!.priceOzUsd, 0.01)

    // ExchangeRate-API v4 & FXRatesAPI bank FX parsers
    val erV4Json = """{"rates":{"DZD":134.0,"EUR":0.92,"GBP":0.79}}"""
    val erV4Rates = com.example.market.SquareMarketCatalog.parseExchangeRateApiV4Response(erV4Json)
    assertEquals(134.0, erV4Rates["USD"] ?: 0.0, 0.01)
    assertEquals(134.0 / 0.92, erV4Rates["EUR"] ?: 0.0, 0.05)

    val fxRatesJson = """{"success":true,"rates":{"DZD":134.5,"EUR":0.91}}"""
    val fxRates = com.example.market.SquareMarketCatalog.parseFxRatesApiResponse(fxRatesJson)
    assertEquals(134.5, fxRates["USD"] ?: 0.0, 0.01)
    assertEquals(134.5 / 0.91, fxRates["EUR"] ?: 0.0, 0.05)

    // OKX & Bybit P2P DZD parsers
    val okxP2pJson = """{"data":{"sell":[{"price":"246.0"},{"price":"247.0"},{"price":"248.0"}]}}"""
    assertEquals(247.0, com.example.market.SquareMarketCatalog.parseOkxP2pMedianDzd(okxP2pJson, "sell") ?: 0.0, 0.01)

    val bybitP2pJson = """{"result":{"items":[{"price":"245.5"},{"price":"246.5"},{"price":"247.5"}]}}"""
    assertEquals(246.5, com.example.market.SquareMarketCatalog.parseBybitP2pMedianDzd(bybitP2pJson) ?: 0.0, 0.01)

    // Arabic source label translation for the new major websites
    val localizedAr = com.example.market.SquareMarketCatalog.localizeSquareSourceLabel(
      source = "Kraken + OKX + Bybit + ExchangeRate-API + FXRatesAPI",
      isArabic = true
    )
    assertTrue(localizedAr.contains("كراكن"))
    assertTrue(localizedAr.contains("أو كيه إكس"))
    assertTrue(localizedAr.contains("بايبت"))
    assertTrue(localizedAr.contains("إكستشينج ريت"))
    assertTrue(localizedAr.contains("أف إكس ريتس"))
  }

  @Test
  fun appUpdate_topBarIndicatorVisibility_retryBackoff_andArabicButtons() {
    val sampleRelease = com.example.update.GitHubReleaseInfo(
      tagName = "v1.9.0",
      versionName = "1.9.0",
      title = "Vaultix v1.9.0",
      releaseNotes = "",
      publishedAt = "",
      htmlUrl = "https://github.com/charfeddine23/Salery/releases/tag/v1.9.0",
      apkName = "Vaultix-1.9.0.apk",
      apkDownloadUrl = "https://github.com/charfeddine23/Salery/releases/download/v1.9.0/Vaultix-1.9.0.apk",
      apkSizeBytes = 10_000_000L
    )

    assertFalse(com.example.update.AppUpdateUiState.Idle.hasVisibleTopIndicator)
    assertFalse(com.example.update.AppUpdateUiState.Checking.hasVisibleTopIndicator)
    assertFalse(com.example.update.AppUpdateUiState.UpToDate("1.0.0").hasVisibleTopIndicator)
    assertFalse(com.example.update.AppUpdateUiState.Error("Offline", null).hasVisibleTopIndicator)

    assertTrue(com.example.update.AppUpdateUiState.Available(sampleRelease).hasVisibleTopIndicator)
    assertTrue(com.example.update.AppUpdateUiState.Downloading(sampleRelease, 100L, 1000L).hasVisibleTopIndicator)
    assertTrue(
      com.example.update.AppUpdateUiState.ReadyToInstall(
        sampleRelease,
        java.io.File("dummy.apk")
      ).hasVisibleTopIndicator
    )
    assertTrue(
      com.example.update.AppUpdateUiState.Error(
        "Retrying",
        sampleRelease,
        isRetrying = true
      ).hasVisibleTopIndicator
    )

    assertEquals(4_000L, com.example.update.AppUpdateManager.computeRetryBackoffMillis(1))
    assertEquals(8_000L, com.example.update.AppUpdateManager.computeRetryBackoffMillis(2))
    assertEquals(15_000L, com.example.update.AppUpdateManager.computeRetryBackoffMillis(3))
    assertEquals(30_000L, com.example.update.AppUpdateManager.computeRetryBackoffMillis(4))
    assertEquals(60_000L, com.example.update.AppUpdateManager.computeRetryBackoffMillis(5))

    assertEquals("ثبّت التحديث", ArabicStrings.installUpdateButton)
    assertEquals("ربما لاحقاً", ArabicStrings.updateMaybeLater)
    assertEquals("Install Update", EnglishStrings.installUpdateButton)
    assertEquals("Maybe Later", EnglishStrings.updateMaybeLater)
  }
}
