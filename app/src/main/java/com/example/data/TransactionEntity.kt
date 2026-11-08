package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    INCOME,
    EXPENSE
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val title: String,
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val isSalaryCredit: Boolean = false,
    val isPaidFromSavings: Boolean = false
)
