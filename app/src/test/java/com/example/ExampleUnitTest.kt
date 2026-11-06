package com.example

import com.example.ai.FinancialInsightsEngine
import com.example.analytics.BalanceProjectionEngine
import com.example.analytics.ForecastPeriod
import com.example.data.DebtType
import com.example.data.TransactionType
import com.example.data.UserSettings
import com.example.ui.components.toPatternHash
import com.example.ui.i18n.APP_NAME
import com.example.ui.i18n.APP_VERSION
import com.example.ui.i18n.DEVELOPER_NAME
import com.example.ui.i18n.formatDzdAmount
import com.example.viewmodel.FinancialSummary
import org.junit.Assert.assertEquals
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
}
