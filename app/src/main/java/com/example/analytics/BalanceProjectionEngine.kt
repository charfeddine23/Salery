package com.example.analytics

import com.example.data.DebtEntity
import com.example.data.DebtType
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.data.UserSettings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

enum class ForecastPeriod {
    DAYS_7,
    DAYS_30,
    MONTHS_6
}

data class ProjectionPoint(
    val stepIndex: Int,
    val label: String,
    val balance: Double,
    val dateMillis: Long,
    val isDepositDay: Boolean = false,
    val isDebtDay: Boolean = false,
    val eventDescription: String? = null
)

data class ProjectionResult(
    val period: ForecastPeriod,
    val points: List<ProjectionPoint>,
    val startBalance: Double,
    val endBalance: Double,
    val netChange: Double,
    val dailyBurn: Double,
    val totalInflow: Double,
    val totalOutflow: Double
)

object BalanceProjectionEngine {

    fun calculateProjection(
        currentBalance: Double,
        transactions: List<TransactionEntity>,
        debts: List<DebtEntity>,
        userSettings: UserSettings,
        period: ForecastPeriod
    ): ProjectionResult {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // 1. Calculate Average Daily Expense Burn Rate
        val thirtyDaysAgo = now - TimeUnit.DAYS.toMillis(30)
        val recentExpenses = transactions
            .filter { it.type == TransactionType.EXPENSE && it.dateMillis >= thirtyDaysAgo }
            .sumOf { it.amount }

        val dailyBurn = if (recentExpenses > 0) {
            recentExpenses / 30.0
        } else if (userSettings.salaryAmount > 0) {
            (userSettings.salaryAmount * 0.55) / 30.0
        } else {
            1200.0 // Default reasonable baseline DZD/day
        }

        var runningBalance = currentBalance
        var totalInflow = 0.0
        var totalOutflow = 0.0
        val points = mutableListOf<ProjectionPoint>()

        val dayDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val monthDateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        when (period) {
            ForecastPeriod.DAYS_7 -> {
                // Day 0: Today
                points.add(
                    ProjectionPoint(
                        stepIndex = 0,
                        label = "Now",
                        balance = runningBalance,
                        dateMillis = now
                    )
                )

                for (day in 1..7) {
                    val targetCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, day) }
                    val targetDayOfMonth = targetCal.get(Calendar.DAY_OF_MONTH)
                    val targetMillis = targetCal.timeInMillis

                    var isDeposit = false
                    var isDebt = false
                    var eventDesc: String? = null

                    // Deduct daily burn
                    runningBalance -= dailyBurn
                    totalOutflow += dailyBurn

                    // Check Smart Salary Payday
                    if (targetDayOfMonth == userSettings.salaryPayday && userSettings.salaryAmount > 0) {
                        runningBalance += userSettings.salaryAmount
                        totalInflow += userSettings.salaryAmount
                        isDeposit = true
                        eventDesc = "Salary: +${userSettings.salaryAmount.toLong()}"
                    }

                    // Check Debt Settlements due in this window
                    for (debt in debts) {
                        if (!debt.isPaid && debt.dueDateMillis in (targetMillis - 43200000L)..(targetMillis + 43200000L)) {
                            if (debt.type == DebtType.OWED_BY_ME) {
                                runningBalance -= debt.remainingAmount
                                totalOutflow += debt.remainingAmount
                                isDebt = true
                                eventDesc = (eventDesc?.plus(" | ") ?: "") + "Debt: -${debt.remainingAmount.toLong()}"
                            } else {
                                runningBalance += debt.remainingAmount
                                totalInflow += debt.remainingAmount
                                isDeposit = true
                                eventDesc = (eventDesc?.plus(" | ") ?: "") + "Collection: +${debt.remainingAmount.toLong()}"
                            }
                        }
                    }

                    points.add(
                        ProjectionPoint(
                            stepIndex = day,
                            label = dayDateFormat.format(targetCal.time),
                            balance = max(0.0, runningBalance),
                            dateMillis = targetMillis,
                            isDepositDay = isDeposit,
                            isDebtDay = isDebt,
                            eventDescription = eventDesc
                        )
                    )
                }
            }

            ForecastPeriod.DAYS_30 -> {
                // 30 days forecast with 7 strategic milestone points
                points.add(
                    ProjectionPoint(
                        stepIndex = 0,
                        label = "Now",
                        balance = runningBalance,
                        dateMillis = now
                    )
                )

                for (day in 1..30) {
                    val targetCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, day) }
                    val targetDayOfMonth = targetCal.get(Calendar.DAY_OF_MONTH)
                    val targetMillis = targetCal.timeInMillis

                    var isDeposit = false
                    var isDebt = false
                    var eventDesc: String? = null

                    runningBalance -= dailyBurn
                    totalOutflow += dailyBurn

                    if (targetDayOfMonth == userSettings.salaryPayday && userSettings.salaryAmount > 0) {
                        runningBalance += userSettings.salaryAmount
                        totalInflow += userSettings.salaryAmount
                        isDeposit = true
                        eventDesc = "Salary Deposit"
                    }

                    for (debt in debts) {
                        if (!debt.isPaid) {
                            if (debt.isRecurringMonthlyInstallment && debt.installmentDayOfMonth == targetDayOfMonth) {
                                val pay = if (debt.monthlyInstallmentAmount > 0) minOf(debt.monthlyInstallmentAmount, debt.remainingAmount) else debt.remainingAmount
                                if (debt.type == DebtType.OWED_BY_ME) {
                                    runningBalance -= pay
                                    totalOutflow += pay
                                    isDebt = true
                                } else {
                                    runningBalance += pay
                                    totalInflow += pay
                                    isDeposit = true
                                }
                            } else if (debt.dueDateMillis in (targetMillis - 43200000L)..(targetMillis + 43200000L)) {
                                if (debt.type == DebtType.OWED_BY_ME) {
                                    runningBalance -= debt.remainingAmount
                                    totalOutflow += debt.remainingAmount
                                    isDebt = true
                                } else {
                                    runningBalance += debt.remainingAmount
                                    totalInflow += debt.remainingAmount
                                    isDeposit = true
                                }
                            }
                        }
                    }

                    // Sample at Days 5, 10, 15, 20, 25, 30
                    if (day % 5 == 0 || day == 30) {
                        points.add(
                            ProjectionPoint(
                                stepIndex = day,
                                label = dayDateFormat.format(targetCal.time),
                                balance = max(0.0, runningBalance),
                                dateMillis = targetMillis,
                                isDepositDay = isDeposit,
                                isDebtDay = isDebt,
                                eventDescription = eventDesc
                            )
                        )
                    }
                }
            }

            ForecastPeriod.MONTHS_6 -> {
                // 6 Monthly milestones
                points.add(
                    ProjectionPoint(
                        stepIndex = 0,
                        label = "Now",
                        balance = runningBalance,
                        dateMillis = now
                    )
                )

                val monthlySalary = userSettings.salaryAmount
                val monthlyBurn = dailyBurn * 30.0

                for (month in 1..6) {
                    val targetCal = Calendar.getInstance().apply { add(Calendar.MONTH, month) }
                    val targetMillis = targetCal.timeInMillis

                    runningBalance += monthlySalary
                    totalInflow += monthlySalary

                    runningBalance -= monthlyBurn
                    totalOutflow += monthlyBurn

                    // Settle monthly installments
                    for (debt in debts) {
                        if (!debt.isPaid && debt.isRecurringMonthlyInstallment && debt.remainingAmount > 0) {
                            val pay = if (debt.monthlyInstallmentAmount > 0) minOf(debt.monthlyInstallmentAmount, debt.remainingAmount) else debt.remainingAmount
                            if (debt.type == DebtType.OWED_BY_ME) {
                                runningBalance -= pay
                                totalOutflow += pay
                            } else {
                                runningBalance += pay
                                totalInflow += pay
                            }
                        }
                    }

                    points.add(
                        ProjectionPoint(
                            stepIndex = month,
                            label = monthDateFormat.format(targetCal.time),
                            balance = max(0.0, runningBalance),
                            dateMillis = targetMillis,
                            isDepositDay = true,
                            eventDescription = "Month $month Net"
                        )
                    )
                }
            }
        }

        val endBalance = points.lastOrNull()?.balance ?: runningBalance
        val netChange = endBalance - currentBalance

        return ProjectionResult(
            period = period,
            points = points,
            startBalance = currentBalance,
            endBalance = endBalance,
            netChange = netChange,
            dailyBurn = dailyBurn,
            totalInflow = totalInflow,
            totalOutflow = totalOutflow
        )
    }
}
