package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DebtType {
    OWED_BY_ME, // Payable: I owe money to someone
    OWED_TO_ME  // Receivable: Someone owes money to me
}

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: DebtType,
    val personName: String,
    val amount: Double,
    val note: String = "",
    val dueDateMillis: Long,
    val autoPayOnDueDate: Boolean = true,
    val isPaid: Boolean = false,
    val paidDateMillis: Long? = null,
    val createdDateMillis: Long = System.currentTimeMillis(),
    // Monthly recurring installment support
    val isRecurringMonthlyInstallment: Boolean = false,
    val monthlyInstallmentAmount: Double = 0.0,
    val installmentDayOfMonth: Int = 1,
    val remainingAmount: Double = amount,
    val lastInstallmentCreditedMonth: String = ""
)
