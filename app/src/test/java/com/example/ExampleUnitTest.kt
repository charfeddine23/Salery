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
}
