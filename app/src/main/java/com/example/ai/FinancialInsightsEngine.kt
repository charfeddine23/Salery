package com.example.ai

import com.example.data.DebtEntity
import com.example.data.DebtType
import com.example.viewmodel.FinancialSummary
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.data.UserSettings
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.formatDzdAmount
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.abs

enum class InsightIconType {
    VELOCITY,
    RUNWAY,
    DEBT,
    SAVINGS,
    TIP
}

data class FinancialInsight(
    val id: String,
    val category: String,
    val title: String,
    val description: String,
    val metricValue: String? = null,
    val metricDelta: String? = null,
    val isPositive: Boolean = true,
    val iconType: InsightIconType = InsightIconType.VELOCITY
)

object FinancialInsightsEngine {

    fun generateInsights(
        transactions: List<TransactionEntity>,
        debts: List<DebtEntity>,
        summary: FinancialSummary,
        userSettings: UserSettings,
        isArabic: Boolean = false
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // 1. Spending Velocity Analysis (Last 7 Days vs Prior 7 Days)
        val sevenDaysMillis = TimeUnit.DAYS.toMillis(7)
        val fourteenDaysMillis = TimeUnit.DAYS.toMillis(14)

        val recentWeekExpenses = transactions
            .filter { it.type == TransactionType.EXPENSE && (now - it.dateMillis) <= sevenDaysMillis }
            .sumOf { it.amount }

        val priorWeekExpenses = transactions
            .filter { it.type == TransactionType.EXPENSE && (now - it.dateMillis) in (sevenDaysMillis + 1)..fourteenDaysMillis }
            .sumOf { it.amount }

        if (recentWeekExpenses > 0 || priorWeekExpenses > 0) {
            val deltaPct = if (priorWeekExpenses > 0) {
                ((recentWeekExpenses - priorWeekExpenses) / priorWeekExpenses) * 100.0
            } else 0.0

            val isAccelerated = deltaPct > 10.0
            val isFrugal = deltaPct < -10.0

            val title = when {
                isAccelerated -> if (isArabic) "تسارع في المصاريف الأسبوعية" else "Accelerated Spending Velocity"
                isFrugal -> if (isArabic) "انخفاض ممتاز في وتيرة الإنفاق" else "Frugal Spending Pace"
                else -> if (isArabic) "وتيرة إنفاق أسبوعية مستقرة" else "Stable Weekly Spending Pace"
            }

            val desc = when {
                isAccelerated -> if (isArabic) {
                    "ارتفعت مصاريفك بنسبة ${abs(deltaPct).toInt()}% مقارنة بالأسبوع الماضي (${formatDzdAmount(recentWeekExpenses)} مقابل ${formatDzdAmount(priorWeekExpenses)}). ننصحك بالحد من المشتريات غير الضرورية."
                } else {
                    "Weekly expenses rose by ${abs(deltaPct).toInt()}% vs prior week (${formatDzdAmount(recentWeekExpenses)} vs ${formatDzdAmount(priorWeekExpenses)}). Consider trimming discretionary shopping."
                }
                isFrugal -> if (isArabic) {
                    "أحسنت! انخفضت مصاريفك هذا الأسبوع بنسبة ${abs(deltaPct).toInt()}% (${formatDzdAmount(recentWeekExpenses)}). هذا يوفر لك سيولة إضافية حتى نهاية الشهر."
                } else {
                    "Great work! Expenses fell by ${abs(deltaPct).toInt()}% this week (${formatDzdAmount(recentWeekExpenses)}). You are retaining more capital."
                }
                else -> if (isArabic) {
                    "إنفاقك الأسبوعي مستقر بمعدل ${formatDzdAmount(recentWeekExpenses / 7.0)} يومياً وهو متوافق مع ميزانيتك الشهرية."
                } else {
                    "Your weekly spending is stable at ~${formatDzdAmount(recentWeekExpenses / 7.0)}/day, aligned with your monthly plan."
                }
            }

            insights.add(
                FinancialInsight(
                    id = "velocity",
                    category = if (isArabic) "سرعة الإنفاق" else "Spending Velocity",
                    title = title,
                    description = desc,
                    metricValue = formatDzdAmount(recentWeekExpenses),
                    metricDelta = if (priorWeekExpenses > 0) "${if (deltaPct > 0) "+" else ""}${deltaPct.toInt()}%" else null,
                    isPositive = !isAccelerated,
                    iconType = InsightIconType.VELOCITY
                )
            )
        }

        // 2. Salary Runway & Daily Budget Countdown
        val payday = userSettings.salaryPayday.coerceIn(1, 31)
        val daysUntilPayday = if (currentDay <= payday) {
            payday - currentDay
        } else {
            (maxDaysInMonth - currentDay) + payday
        }

        val safeDays = maxOf(1, daysUntilPayday)
        val dailyRunwayBudget = (summary.totalBalance / safeDays).coerceAtLeast(0.0)

        val runwayTitle = if (daysUntilPayday == 0) {
            if (isArabic) "اليوم هو يوم الراتب!" else "Today is Salary Payday!"
        } else {
            if (isArabic) "باقي $daysUntilPayday يوماً على الراتب" else "$daysUntilPayday Days Until Payday"
        }

        val runwayDesc = if (daysUntilPayday == 0) {
            if (isArabic) {
                "سيتم تسجيل راتب ${userSettings.salaryEmployer} بقيمة ${formatDzdAmount(userSettings.salaryAmount)} اليوم تلقائياً."
            } else {
                "${userSettings.salaryEmployer} salary of ${formatDzdAmount(userSettings.salaryAmount)} will credit automatically today."
            }
        } else {
            if (isArabic) {
                "بناءً على رصيدك المتاح، يمكنك إنفاق حتى ${formatDzdAmount(dailyRunwayBudget)} يومياً بأمان تام حتى وصول الراتب في يوم $payday."
            } else {
                "Based on current funds, your safe daily burn limit is ${formatDzdAmount(dailyRunwayBudget)}/day until payday on Day $payday."
            }
        }

        insights.add(
            FinancialInsight(
                id = "runway",
                category = if (isArabic) "مدرج الراتب" else "Salary Runway",
                title = runwayTitle,
                description = runwayDesc,
                metricValue = formatDzdAmount(dailyRunwayBudget),
                metricDelta = if (isArabic) "$daysUntilPayday يوم" else "$daysUntilPayday days",
                isPositive = dailyRunwayBudget > 1500.0,
                iconType = InsightIconType.RUNWAY
            )
        )

        // 3. Debt Burden & Liquidity Health
        val pendingOwedByMe = summary.pendingDebtsOwedByMe
        val pendingOwedToMe = summary.pendingDebtsOwedToMe

        if (pendingOwedByMe > 0) {
            val debtRatio = if (summary.totalBalance > 0) (pendingOwedByMe / summary.totalBalance) * 100.0 else 100.0
            val isHeavy = debtRatio > 35.0

            val debtTitle = if (isHeavy) {
                if (isArabic) "تنبيه: التزامات الديون مرتفعة" else "Debt Pressure Alert"
            } else {
                if (isArabic) "التزامات الديون تحت السيطرة" else "Debts Under Control"
            }

            val debtDesc = if (isArabic) {
                "عليك ديون مستحقة بقيمة ${formatDzdAmount(pendingOwedByMe)} تمثل ${debtRatio.toInt()}% من رصيدك الحالي. جدولتها بأقساط شهرية تجنبك الضغط المالي المفاجئ."
            } else {
                "You have ${formatDzdAmount(pendingOwedByMe)} in pending debts (${debtRatio.toInt()}% of available balance). Monthly auto-installments will safeguard liquidity."
            }

            insights.add(
                FinancialInsight(
                    id = "debt_obligation",
                    category = if (isArabic) "إدارة الديون" else "Debt Pressure",
                    title = debtTitle,
                    description = debtDesc,
                    metricValue = formatDzdAmount(pendingOwedByMe),
                    metricDelta = "${debtRatio.toInt()}% balance",
                    isPositive = !isHeavy,
                    iconType = InsightIconType.DEBT
                )
            )
        } else if (pendingOwedToMe > 0) {
            insights.add(
                FinancialInsight(
                    id = "debt_receivable",
                    category = if (isArabic) "مستحقات خارجية" else "Receivables",
                    title = if (isArabic) "لديك مستحقات تنتظر التحصيل" else "Pending Receivables",
                    description = if (isArabic) {
                        "لديك ${formatDzdAmount(pendingOwedToMe)} مستحقة لدى أشخاص آخرين. تحصيلها سيعزز احتياطياتك المالية فوراً."
                    } else {
                        "Others owe you ${formatDzdAmount(pendingOwedToMe)}. Collecting them will boost your working capital."
                    },
                    metricValue = formatDzdAmount(pendingOwedToMe),
                    isPositive = true,
                    iconType = InsightIconType.DEBT
                )
            )
        }

        // 4. Savings Rate & Cash Surplus
        if (summary.monthlyIncome > 0) {
            val netSavings = summary.monthlyIncome - summary.monthlyExpenses
            val savingsRate = ((netSavings / summary.monthlyIncome) * 100.0).coerceAtLeast(0.0)

            val savingsTitle = when {
                savingsRate >= 30.0 -> if (isArabic) "معدل ادخار ممتاز (+30%)" else "Superb Savings Rate (+30%)"
                savingsRate >= 15.0 -> if (isArabic) "معدل ادخار صحي" else "Healthy Savings Cushion"
                else -> if (isArabic) "فرصة لتحسين الادخار" else "Savings Growth Opportunity"
            }

            val savingsDesc = if (isArabic) {
                "حققت فائضاً شهرياً قدره ${formatDzdAmount(netSavings.coerceAtLeast(0.0))} بمعدل ادخار ${savingsRate.toInt()}%. الاستمرار على هذا النحو يبني درع أمان مالي متين."
            } else {
                "Net monthly surplus is ${formatDzdAmount(netSavings.coerceAtLeast(0.0))} (${savingsRate.toInt()}% savings rate). You are consistently growing your safety buffer."
            }

            insights.add(
                FinancialInsight(
                    id = "savings",
                    category = if (isArabic) "معدل الادخار" else "Savings Milestone",
                    title = savingsTitle,
                    description = savingsDesc,
                    metricValue = "${savingsRate.toInt()}%",
                    metricDelta = formatDzdAmount(netSavings.coerceAtLeast(0.0)),
                    isPositive = savingsRate >= 15.0,
                    iconType = InsightIconType.SAVINGS
                )
            )
        } else {
            // Friendly onboarding tip
            insights.add(
                FinancialInsight(
                    id = "starter_tip",
                    category = if (isArabic) "نصيحة ذكية" else "Smart Tip",
                    title = if (isArabic) "تفعيل جدولة الراتب" else "Smart Salary Scheduler",
                    description = if (isArabic) {
                        "قم بضبط راتبك الشهري (${userSettings.salaryEmployer}) في القائمة الجانبية لتفعيل التنبؤات المالية ومدرج الأمان التلقائي."
                    } else {
                        "Set up your ${userSettings.salaryEmployer} salary in the drawer to unlock automated daily burn forecasts and runway predictions."
                    },
                    isPositive = true,
                    iconType = InsightIconType.TIP
                )
            )
        }

        return insights
    }
}
