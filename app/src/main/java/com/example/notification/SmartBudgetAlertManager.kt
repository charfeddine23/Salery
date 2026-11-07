package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.i18n.formatDzdAmount
import java.util.Calendar
import java.util.Locale

/**
 * Data model for a Category Budget Alert.
 * Flags when expenses reach 80% or exceed 100% of the specified monthly cap.
 */
data class CategoryBudgetAlert(
    val categoryId: String,
    val categoryNameEn: String,
    val categoryNameAr: String,
    val spentAmount: Double,
    val budgetCap: Double,
    val percentage: Double,
    val monthNameEn: String,
    val monthNameAr: String,
    val isNearCap: Boolean,     // >= 80% and < 100%
    val isExceeded: Boolean,    // >= 100%
    val remainingAmount: Double,
    val adviceAr: String,
    val adviceEn: String
)

/**
 * Smart Budget Alert Manager & Local Notification Dispatcher.
 * Monitors category spending velocity, detects 80% threshold warnings,
 * provides tailored proactive advice to avoid deficit, and dispatches high-priority
 * Android system notifications.
 */
object SmartBudgetAlertManager {
    const val CHANNEL_ID = "smart_budget_alerts"
    private const val TAG = "SmartBudgetAlertManager"

    // Default monthly budget caps in Algerian Dinars (DZD)
    val DEFAULT_CAPS = mapOf(
        "shopping" to 30000.0,
        "food" to 35000.0,
        "bills" to 25000.0,
        "transport" to 15000.0,
        "health" to 10000.0,
        "entertainment" to 12000.0,
        "other" to 15000.0
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Smart Budget & Deficit Alerts"
            val descriptionText = "Proactive alerts when spending approaches or exceeds 80% of category budget limits"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun normalizeCategoryKey(rawCategory: String): String {
        val lower = rawCategory.trim().lowercase(Locale.ROOT)
        return when {
            lower.contains("shop") || lower.contains("تسوق") -> "shopping"
            lower.contains("food") || lower.contains("طعام") || lower.contains("مطاعم") || lower.contains("أكل") -> "food"
            lower.contains("bill") || lower.contains("فواتير") || lower.contains("كهرباء") || lower.contains("ماء") -> "bills"
            lower.contains("transport") || lower.contains("نقل") || lower.contains("بنزين") || lower.contains("وقود") -> "transport"
            lower.contains("health") || lower.contains("صحة") || lower.contains("دواء") || lower.contains("علاج") -> "health"
            lower.contains("entertain") || lower.contains("ترفيه") || lower.contains("نزهة") -> "entertainment"
            else -> "other"
        }
    }

    fun getCategoryArabicName(key: String): String {
        return when (key) {
            "shopping" -> "التسوق"
            "food" -> "الطعام والمطاعم"
            "bills" -> "الفواتير والخدمات"
            "transport" -> "المواصلات والوقود"
            "health" -> "الصحة والعلاج"
            "entertainment" -> "الترفيه والأنشطة"
            else -> "المصاريف المتنوعة"
        }
    }

    fun getCategoryEnglishName(key: String): String {
        return when (key) {
            "shopping" -> "Shopping"
            "food" -> "Food & Dining"
            "bills" -> "Bills & Utilities"
            "transport" -> "Transport & Fuel"
            "health" -> "Healthcare"
            "entertainment" -> "Entertainment"
            else -> "Miscellaneous"
        }
    }

    fun getMonthNameAr(monthIndex: Int): String {
        return when (monthIndex) {
            Calendar.JANUARY -> "يناير"
            Calendar.FEBRUARY -> "فبراير"
            Calendar.MARCH -> "مارس"
            Calendar.APRIL -> "أبريل"
            Calendar.MAY -> "مايو"
            Calendar.JUNE -> "يونيو"
            Calendar.JULY -> "يوليو"
            Calendar.AUGUST -> "أغسطس"
            Calendar.SEPTEMBER -> "سبتمبر"
            Calendar.OCTOBER -> "أكتوبر"
            Calendar.NOVEMBER -> "نوفمبر"
            Calendar.DECEMBER -> "ديسمبر"
            else -> "الشهر الحالي"
        }
    }

    fun calculateAlerts(
        transactions: List<TransactionEntity>,
        customCaps: Map<String, Double> = emptyMap(),
        targetMonthCalendar: Calendar = Calendar.getInstance()
    ): List<CategoryBudgetAlert> {
        val targetMonth = targetMonthCalendar.get(Calendar.MONTH)
        val targetYear = targetMonthCalendar.get(Calendar.YEAR)

        val monthNameEn = targetMonthCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH) ?: "Current Month"
        val monthNameAr = getMonthNameAr(targetMonth)

        // Filter transactions for the target month
        val monthExpenses = transactions.filter {
            it.type == TransactionType.EXPENSE
        }.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            cal.get(Calendar.MONTH) == targetMonth && cal.get(Calendar.YEAR) == targetYear
        }

        val spentByCategory = mutableMapOf<String, Double>()
        for (tx in monthExpenses) {
            val catKey = normalizeCategoryKey(tx.category)
            spentByCategory[catKey] = (spentByCategory[catKey] ?: 0.0) + tx.amount
        }

        val orderedKeys = listOf("shopping", "food", "bills", "transport", "health", "entertainment", "other")

        return orderedKeys.map { key ->
            val cap = customCaps[key] ?: DEFAULT_CAPS[key] ?: 20000.0
            val spent = spentByCategory[key] ?: 0.0
            val percentage = if (cap > 0) (spent / cap) * 100.0 else 0.0
            val remaining = (cap - spent).coerceAtLeast(0.0)
            val isNearCap = percentage >= 80.0 && percentage < 100.0
            val isExceeded = percentage >= 100.0

            val catAr = getCategoryArabicName(key)
            val catEn = getCategoryEnglishName(key)

            val adviceAr = generateAdviceAr(key, catAr, percentage, remaining, monthNameAr)
            val adviceEn = generateAdviceEn(key, catEn, percentage, remaining, monthNameEn)

            CategoryBudgetAlert(
                categoryId = key,
                categoryNameEn = catEn,
                categoryNameAr = catAr,
                spentAmount = spent,
                budgetCap = cap,
                percentage = percentage,
                monthNameEn = monthNameEn,
                monthNameAr = monthNameAr,
                isNearCap = isNearCap,
                isExceeded = isExceeded,
                remainingAmount = remaining,
                adviceAr = adviceAr,
                adviceEn = adviceEn
            )
        }
    }

    /**
     * Generates a sample/highlight alert for previewing the 80% Shopping threshold for November,
     * fulfilling the exact requirement described by the user.
     */
    fun createSampleNovemberShoppingAlert(): CategoryBudgetAlert {
        val spent = 24000.0
        val cap = 30000.0
        val remaining = 6000.0
        val pct = 80.0
        return CategoryBudgetAlert(
            categoryId = "shopping",
            categoryNameEn = "Shopping",
            categoryNameAr = "التسوق",
            spentAmount = spent,
            budgetCap = cap,
            percentage = pct,
            monthNameEn = "November",
            monthNameAr = "نوفمبر",
            isNearCap = true,
            isExceeded = false,
            remainingAmount = remaining,
            adviceAr = "💡 نصيحة سريعة لتفادي العجز: لقد استهلكت 80% من ميزانية التسوق لشهر نوفمبر ومتبقٍ ${formatDzdAmount(remaining)} دج فقط. لتفادي عجز الميزانية: يُنصح بتأجيل شراء الملابس أو الكماليات غير العاجلة حتى موعد الراتب القادم، واستخدام الفائض من بنود أخرى إن لزم الأمر.",
            adviceEn = "💡 Quick Deficit Prevention Advice: You have reached 80% of your Shopping budget for November (${formatDzdAmount(remaining)} DZD remaining). To avert a deficit: freeze non-essential purchases and luxury shopping until your next salary cycle, and stick strictly to essential household needs."
        )
    }

    private fun generateAdviceAr(
        key: String,
        catAr: String,
        percentage: Double,
        remaining: Double,
        monthNameAr: String
    ): String {
        val formattedRemaining = formatDzdAmount(remaining)
        return when {
            percentage >= 100.0 -> {
                "⚠️ تحذير عجز: لقد تجاوزت سقف ميزانية $catAr لشهر $monthNameAr بالكامل. يُنصح بتجميد أي إنفاق إضافي في هذا البند فوراً وتحويل الفائض المالي من بنود أخرى لسد الفجوة."
            }
            percentage >= 80.0 -> {
                when (key) {
                    "shopping" -> "💡 نصيحة سريعة لتفادي العجز: لقد بلغت 80% من سقف ميزانية التسوق لشهر $monthNameAr (متبقٍ $formattedRemaining دج فقط). يُوصى بتجميد مشتريات الملابس والأجهزة الكمالية للأسبوع القادم، وحصر الصرف في الضروريات لتفادي عجز الميزانية قبل الراتب القادم."
                    "food" -> "💡 نصيحة سريعة لتفادي العجز: بلغت مصاريف الطعام والمطاعم 80% من سقف $monthNameAr. يُنصح بالاعتماد على الطهي المنزلي وقوائم البقالة المحددة وتجنب طلبات التوصيل اليومية لإنهاء الشهر برصيد آمن."
                    "bills" -> "💡 نصيحة سريعة لتفادي العجز: استهلكت 80% من مخصصات الفواتير لشهر $monthNameAr. تأكد من سداد الالتزامات الأساسية المتبقية فوراً وتجنب الرسوم الإضافية قبل الصرف الترفيهي."
                    "transport" -> "💡 نصيحة سريعة لتفادي العجز: بلغت 80% من سقف النقل والوقود. يُنصح بدمج المشاوير الأسبوعية واستخدام وسائل النقل المشتركة عند الإمكان للحد من حرق الرصيد."
                    else -> "💡 نصيحة سريعة لتفادي العجز: استهلكت 80% من سقف ميزانية $catAr لشهر $monthNameAr (متبقٍ $formattedRemaining دج). خفّض وتيرة الصرف اليومية وركّز على الأولويات لتفادي الوقوع في العجز المالي."
                }
            }
            else -> {
                "وضع الميزانية آمن (متبقٍ $formattedRemaining دج). حافظ على هذا النسق المتزن لضمان تحقيق فائض ادخاري ممتاز في نهاية الشهر."
            }
        }
    }

    private fun generateAdviceEn(
        key: String,
        catEn: String,
        percentage: Double,
        remaining: Double,
        monthNameEn: String
    ): String {
        val formattedRemaining = formatDzdAmount(remaining)
        return when {
            percentage >= 100.0 -> {
                "⚠️ Deficit Alert: You have exceeded 100% of your $catEn budget for $monthNameEn. Freeze any further spending in this category immediately and cover the shortfall from savings."
            }
            percentage >= 80.0 -> {
                when (key) {
                    "shopping" -> "💡 Quick Deficit Prevention Advice: You reached 80% of your Shopping budget for $monthNameEn ($formattedRemaining DZD remaining). Freeze non-essential leisure shopping until payday to keep your cashflow positive."
                    "food" -> "💡 Quick Deficit Prevention Advice: Food & dining expenses hit 80% of the $monthNameEn cap. Favor home cooking and cut down takeout orders to finish the month with a surplus."
                    "bills" -> "💡 Quick Deficit Prevention Advice: 80% of utilities & bills cap utilized. Prioritize mandatory recurring payments and avoid late fees before discretionary spending."
                    "transport" -> "💡 Quick Deficit Prevention Advice: 80% of transport cap consumed. Plan and batch errands together to conserve fuel and transport spend."
                    else -> "💡 Quick Deficit Prevention Advice: 80% of $catEn budget used ($formattedRemaining DZD left). Slow down daily spending velocity to ensure your reserves stay intact."
                }
            }
            else -> {
                "Budget healthy ($formattedRemaining DZD remaining). Maintain this pace to guarantee a strong net savings cushion at month end."
            }
        }
    }

    /**
     * Dispatches a high-priority Android notification to the device status bar.
     */
    fun sendAlertNotification(context: Context, alert: CategoryBudgetAlert, isArabic: Boolean): Boolean {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            if (status != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
                return false
            }
        }

        val title = if (isArabic) {
            "⚠️ تنبيه سقف الميزانية: ${alert.categoryNameAr} (${alert.percentage.toInt()}%)"
        } else {
            "⚠️ Budget Alert: ${alert.categoryNameEn} (${alert.percentage.toInt()}%)"
        }

        val adviceText = if (isArabic) alert.adviceAr else alert.adviceEn
        val remainingFormatted = formatDzdAmount(alert.remainingAmount)
        val body = if (isArabic) {
            "بلغت ${alert.percentage.toInt()}% من ميزانية ${alert.categoryNameAr} لشهر ${alert.monthNameAr} (متبقٍ ${remainingFormatted} دج).\n${adviceText}"
        } else {
            "Reached ${alert.percentage.toInt()}% of ${alert.categoryNameEn} budget for ${alert.monthNameEn} (${remainingFormatted} DZD remaining).\n${adviceText}"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (alert.categoryId.hashCode() and 0x7FFFFFFF),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationId = 8000 + (alert.categoryId.hashCode().let { if (it < 0) -it else it } % 1000)
        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while notifying: ${e.message}")
            false
        }
    }
}
