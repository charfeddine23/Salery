package com.example

import com.example.data.DebtType
import com.example.data.TransactionType
import com.example.ui.components.toPatternHash
import com.example.ui.i18n.APP_NAME
import com.example.ui.i18n.APP_VERSION
import com.example.ui.i18n.DEVELOPER_NAME
import com.example.ui.i18n.formatDzd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun appIdentity_andDeveloper_areCorrect() {
    assertEquals("SaleryPelse", APP_NAME)
    assertEquals("Kahlessenane Charfeddine", DEVELOPER_NAME)
    assertEquals("1.0.1", APP_VERSION)
  }

  @Test
  fun currencyFormatting_usesDzd() {
    val formatted = formatDzd(85000.0, "DZD")
    assertTrue(formatted.contains("85,000") && formatted.contains("DZD"))
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
}
