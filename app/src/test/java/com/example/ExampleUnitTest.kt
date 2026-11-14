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
}
