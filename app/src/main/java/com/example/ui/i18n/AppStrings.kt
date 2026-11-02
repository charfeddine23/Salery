package com.example.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.DebtType
import com.example.data.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// App Name, Version, and Developer Name MUST NEVER be translated or changed into Arabic script!
const val APP_NAME = "SaleryPelse"
const val DEVELOPER_NAME = "Kahlessenane Charfeddine"
const val APP_VERSION = "1.0.0"

interface StringsBundle {
    val appName: String get() = APP_NAME
    val developerName: String get() = DEVELOPER_NAME
    val appVersion: String get() = APP_VERSION
    val currencyCode: String

    val navDashboard: String
    val navIncome: String
    val navExpenses: String
    val navDebts: String
    val navSettings: String

    val totalBalance: String
    val monthlyIncome: String
    val monthlyExpense: String
    val activeDebts: String
    val recentTransactions: String
    val seeAll: String

    val salarySchedulerTitle: String
    val salaryAmount: String
    val salaryPayday: String
    val salaryEmployer: String
    val salaryAutoCredit: String
    val salaryCreditedNotice: String
    val salaryNextPayday: String
    val salaryDepositNow: String
    val salaryDepositedToday: String

    val debtManagement: String
    val debtsOwedByMe: String
    val debtsOwedToMe: String
    val debtAutoPay: String
    val debtDueDate: String
    val debtPending: String
    val debtPaid: String
    val markAsPaid: String
    val executePayment: String

    val addTransaction: String
    val addIncome: String
    val addExpense: String
    val addDebt: String
    val amount: String
    val category: String
    val titleOrPerson: String
    val note: String
    val date: String
    val save: String
    val cancel: String
    val delete: String
    val confirmDelete: String

    val settingsTitle: String
    val appLanguage: String
    val langEnglish: String
    val langArabic: String
    val themeMode: String
    val themeDark: String
    val themeLight: String

    val securityTitle: String
    val patternLock: String
    val patternLockDesc: String
    val setPattern: String
    val changePattern: String
    val drawPattern: String
    val confirmPattern: String
    val patternSaved: String
    val patternMismatch: String
    val patternTooShort: String
    val unlockApp: String
    val incorrectPattern: String

    val backupRestore: String
    val backupDriveTitle: String
    val exportBackup: String
    val restoreBackup: String
    val backupSuccess: String
    val restoreSuccess: String
    val restoreError: String
    val backupInfo: String
    val googleDriveSynced: String
    val syncWithDrive: String

    val aboutApp: String
    val developer: String
    val version: String
    val emptyRecords: String
    val searchPlaceholder: String
    val allFilter: String

    fun categoryName(cat: String): String
}

object EnglishStrings : StringsBundle {
    override val currencyCode: String = "DZD"

    override val navDashboard = "Dashboard"
    override val navIncome = "Income"
    override val navExpenses = "Expenses"
    override val navDebts = "Debts"
    override val navSettings = "Settings"

    override val totalBalance = "Total Balance"
    override val monthlyIncome = "Monthly Income"
    override val monthlyExpense = "Monthly Expenses"
    override val activeDebts = "Active Debts"
    override val recentTransactions = "Recent Transactions"
    override val seeAll = "See All"

    override val salarySchedulerTitle = "Smart Salary Scheduler"
    override val salaryAmount = "Monthly Salary (DZD)"
    override val salaryPayday = "Payday (Day of Month)"
    override val salaryEmployer = "Employer / Source"
    override val salaryAutoCredit = "Auto-credit wallet on payday"
    override val salaryCreditedNotice = "Salary credited automatically"
    override val salaryNextPayday = "Next Payday: Day %d of this month"
    override val salaryDepositNow = "Deposit Salary Now"
    override val salaryDepositedToday = "Salary Deposited Today!"

    override val debtManagement = "Debt Management"
    override val debtsOwedByMe = "I Owe (Payable)"
    override val debtsOwedToMe = "Owed to Me (Receivable)"
    override val debtAutoPay = "Auto-payment on due date"
    override val debtDueDate = "Due Date"
    override val debtPending = "Pending"
    override val debtPaid = "Paid"
    override val markAsPaid = "Mark as Paid"
    override val executePayment = "Pay Now"

    override val addTransaction = "Add Transaction"
    override val addIncome = "Add Income"
    override val addExpense = "Add Expense"
    override val addDebt = "Add Debt"
    override val amount = "Amount (DZD)"
    override val category = "Category"
    override val titleOrPerson = "Title / Person"
    override val note = "Note (Optional)"
    override val date = "Date"
    override val save = "Save"
    override val cancel = "Cancel"
    override val delete = "Delete"
    override val confirmDelete = "Are you sure you want to delete this record?"

    override val settingsTitle = "Liquid Glass Settings"
    override val appLanguage = "App Language"
    override val langEnglish = "English"
    override val langArabic = "العربية"
    override val themeMode = "Theme Appearance"
    override val themeDark = "Dark Glass"
    override val themeLight = "Light Crystal"

    override val securityTitle = "Security & Privacy"
    override val patternLock = "Pattern Gesture Lock"
    override val patternLockDesc = "Protect SaleryPelse with 3x3 gesture matrix"
    override val setPattern = "Set Gesture Pattern"
    override val changePattern = "Change Pattern"
    override val drawPattern = "Draw your unlock gesture pattern"
    override val confirmPattern = "Draw the pattern again to confirm"
    override val patternSaved = "Gesture pattern saved securely"
    override val patternMismatch = "Patterns do not match. Try again."
    override val patternTooShort = "Connect at least 4 dots."
    override val unlockApp = "Unlock SaleryPelse"
    override val incorrectPattern = "Incorrect pattern! Try again."

    override val backupRestore = "Backup & Restore"
    override val backupDriveTitle = "Google Drive & Local Backup"
    override val exportBackup = "Export & Backup Data"
    override val restoreBackup = "Restore Data"
    override val backupSuccess = "Backup exported successfully"
    override val restoreSuccess = "Data restored successfully"
    override val restoreError = "Invalid backup format or corrupted JSON"
    override val backupInfo = "Securely backup financial records to Google Drive or export as JSON."
    override val googleDriveSynced = "Google Drive Status: Connected & Synced"
    override val syncWithDrive = "Sync with Google Drive"

    override val aboutApp = "About"
    override val developer = "Developer"
    override val version = "Version"
    override val emptyRecords = "No records found. Tap + to add one."
    override val searchPlaceholder = "Search transactions or notes..."
    override val allFilter = "All"

    override fun categoryName(cat: String): String = cat
}

object ArabicStrings : StringsBundle {
    override val currencyCode: String = "د.ج"

    override val navDashboard = "لوحة التحكم"
    override val navIncome = "المداخيل"
    override val navExpenses = "المصاريف"
    override val navDebts = "الديون"
    override val navSettings = "الإعدادات"

    override val totalBalance = "الرصيد الإجمالي"
    override val monthlyIncome = "الدخل الشهري"
    override val monthlyExpense = "مصاريف الشهر"
    override val activeDebts = "الديون النشطة"
    override val recentTransactions = "أحدث المعاملات"
    override val seeAll = "عرض الكل"

    override val salarySchedulerTitle = "جدولة الراتب الذكية"
    override val salaryAmount = "قيمة الراتب الشهري (د.ج)"
    override val salaryPayday = "يوم استلام الراتب (يوم من الشهر)"
    override val salaryEmployer = "جهة العمل / المصدر"
    override val salaryAutoCredit = "إضافة تلقائية للرصيد في يوم الراتب"
    override val salaryCreditedNotice = "تم إيداع الراتب تلقائياً في الرصيد"
    override val salaryNextPayday = "موعد الراتب القادم: يوم %d من هذا الشهر"
    override val salaryDepositNow = "إيداع الراتب الآن"
    override val salaryDepositedToday = "تم إيداع الراتب اليوم!"

    override val debtManagement = "إدارة الديون"
    override val debtsOwedByMe = "ديون عليّ (مستحقة للدفع)"
    override val debtsOwedToMe = "ديون لي (مستحقة للقبض)"
    override val debtAutoPay = "دفع تلقائي في تاريخ الاستحقاق"
    override val debtDueDate = "تاريخ الاستحقاق"
    override val debtPending = "قيد الانتظار"
    override val debtPaid = "تم السداد"
    override val markAsPaid = "تحديد كمسدد"
    override val executePayment = "سداد الآن"

    override val addTransaction = "إضافة معاملة"
    override val addIncome = "إضافة دخل"
    override val addExpense = "إضافة مصروف"
    override val addDebt = "إضافة دين"
    override val amount = "المبلغ (د.ج)"
    override val category = "الفئة"
    override val titleOrPerson = "العنوان / الشخص"
    override val note = "ملاحظة (اختياري)"
    override val date = "التاريخ"
    override val save = "حفظ"
    override val cancel = "إلغاء"
    override val delete = "حذف"
    override val confirmDelete = "هل أنت متأكد من رغبتك في حذف هذا السجل؟"

    override val settingsTitle = "إعدادات Liquid Glass"
    override val appLanguage = "لغة التطبيق"
    override val langEnglish = "English"
    override val langArabic = "العربية"
    override val themeMode = "مظهر التصميم"
    override val themeDark = "زجاج داكن (Dark Glass)"
    override val themeLight = "كريستال مضيء (Light Crystal)"

    override val securityTitle = "الأمان والخصوصية"
    override val patternLock = "قفل النمط (Gesture Lock)"
    override val patternLockDesc = "حماية SaleryPelse بمصفوفة إيماءات 3x3"
    override val setPattern = "تعيين نمط جديد"
    override val changePattern = "تغيير النمط"
    override val drawPattern = "ارسم نمط إيماءة فتح القفل"
    override val confirmPattern = "أعد رسم النمط للتأكيد"
    override val patternSaved = "تم حفظ نمط الإيماءة بنجاح"
    override val patternMismatch = "الأنماط غير متطابقة. أعد المحاولة."
    override val patternTooShort = "صل 4 نقاط على الأقل."
    override val unlockApp = "فتح قفل SaleryPelse"
    override val incorrectPattern = "النمط غير صحيح! حاول ثانية."

    override val backupRestore = "النسخ الاحتياطي والاستعادة"
    override val backupDriveTitle = "Google Drive والنسخ المحلي"
    override val exportBackup = "تصدير وحفظ نسخة احتياطية"
    override val restoreBackup = "استعادة البيانات"
    override val backupSuccess = "تم تصدير النسخة الاحتياطية بنجاح"
    override val restoreSuccess = "تم استعادة البيانات بنجاح"
    override val restoreError = "ملف النسخ غير صالح أو تالف"
    override val backupInfo = "نسخ السجلات المالية بأمان إلى Google Drive أو تصدير ملف JSON."
    override val googleDriveSynced = "حالة Google Drive: متصل ومزامن"
    override val syncWithDrive = "مزامنة مع Google Drive"

    override val aboutApp = "حول التطبيق"
    override val developer = "المطور"
    override val version = "الإصدار"
    override val emptyRecords = "لا توجد سجلات حالياً. انقر على + للإضافة."
    override val searchPlaceholder = "البحث في المعاملات أو الملاحظات..."
    override val allFilter = "الكل"

    override fun categoryName(cat: String): String {
        return when (cat.lowercase()) {
            "salary" -> "الراتب"
            "freelance" -> "عمل حر"
            "investment" -> "استثمار"
            "gift" -> "هدية"
            "food", "food & dining" -> "طعام ومطاعم"
            "shopping" -> "تسوق"
            "transport" -> "نقل ومواصلات"
            "bills", "bills & utilities" -> "فواتير وخدمات"
            "health", "healthcare" -> "صحة وعلاج"
            "entertainment" -> "ترفيه"
            "education" -> "تعليم"
            "debt_payment" -> "سداد دين"
            "debt_collection" -> "تحصيل دين"
            else -> cat
        }
    }
}

val LocalStrings = staticCompositionLocalOf<StringsBundle> { EnglishStrings }

object AppStrings {
    val current: StringsBundle
        @Composable
        get() = LocalStrings.current
}

fun formatDzd(amount: Double, currencyText: String = "DZD"): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 0
    return "${formatter.format(amount)} $currencyText"
}

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}
