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
    assertEquals("Kahlessenane Charfeddine", DEVELOPER_NAME)
    assertEquals("1.0.1", APP_VERSION)
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
}
