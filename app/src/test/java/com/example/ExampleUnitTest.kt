package com.example

import com.example.ai.FinancialInsightsEngine
import com.example.analytics.BalanceProjectionEngine
import com.example.analytics.ForecastPeriod
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
import com.example.ui.i18n.formatDzdAmount
import com.example.viewmodel.FinancialSummary
import com.example.update.isMandatoryBlockActive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun appIdentity_andDeveloper_areCorrect() {
    assertEquals("SaleryPulse", APP_NAME)
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
  fun financialInsights_generatesRunwayAndInsights() {
    val userSettings = UserSettings(salaryAmount = 90000.0, salaryPayday = 20)
    val summary = FinancialSummary(totalBalance = 50000.0, monthlyIncome = 90000.0, monthlyExpenses = 30000.0)
    val insights = FinancialInsightsEngine.generateInsights(
      transactions = emptyList(),
      debts = emptyList(),
      summary = summary,
      userSettings = userSettings,
      isArabic = false
    )
    assertTrue(insights.isNotEmpty())
    assertTrue(insights.any { it.id == "runway" })
  }

  @Test
  fun balanceProjection_calculatesPoints() {
    val userSettings = UserSettings(salaryAmount = 85000.0, salaryPayday = 25)
    val result7 = BalanceProjectionEngine.calculateProjection(
      currentBalance = 40000.0,
      transactions = emptyList(),
      debts = emptyList(),
      userSettings = userSettings,
      period = ForecastPeriod.DAYS_7
    )
    assertEquals(8, result7.points.size) // Day 0 through Day 7
    assertTrue(result7.dailyBurn > 0.0)

    val result30 = BalanceProjectionEngine.calculateProjection(
      currentBalance = 40000.0,
      transactions = emptyList(),
      debts = emptyList(),
      userSettings = userSettings,
      period = ForecastPeriod.DAYS_30
    )
    assertTrue(result30.points.size >= 6)
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
  fun appUpdateUiState_mandatoryBlockActive_enforcesNonSkippableUpdates() {
    val dummyRelease = com.example.update.GitHubReleaseInfo(
      tagName = "v9.9.9",
      versionName = "9.9.9",
      title = "Mandatory Release",
      releaseNotes = "Critical update",
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

    assertTrue(com.example.update.AppUpdateUiState.Available(dummyRelease).isMandatoryBlockActive)
    assertTrue(com.example.update.AppUpdateUiState.Downloading(dummyRelease, 500L, 1000L).isMandatoryBlockActive)
    assertTrue(
      com.example.update.AppUpdateUiState.ReadyToInstall(
        dummyRelease,
        java.io.File("dummy.apk")
      ).isMandatoryBlockActive
    )
    assertTrue(com.example.update.AppUpdateUiState.Error("Failed download", dummyRelease).isMandatoryBlockActive)
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
}
