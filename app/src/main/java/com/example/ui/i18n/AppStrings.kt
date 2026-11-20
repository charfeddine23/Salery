package com.example.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// App Name, Version, and Developer Name MUST NEVER be translated or changed into Arabic script!
const val APP_NAME = "Vaultix"
const val DEVELOPER_NAME = "Charfeddine Ka"
val APP_VERSION: String get() = com.example.BuildConfig.APP_VERSION_NAME

interface StringsBundle {
    val appName: String get() = APP_NAME
    val appVersion: String get() = com.example.BuildConfig.APP_VERSION_NAME
    val buildDate: String get() = com.example.BuildConfig.BUILD_DATE
    val currencyCode: String

    val navDashboard: String
    val navIncome: String
    val navExpenses: String
    val navDebts: String

    val cardsHubTitle: String
    val cardsHubSubtitle: String
    val edahabiaCardTitle: String
    val visaCardTitle: String
    val visaCardSubtitle: String
    val comingSoon: String
    val visaComingSoonDesc: String
    val backToCards: String
    val activeCardsCount: String
    val currentAccount: String
    val editCardTitle: String
    val editCardSubtitle: String
    val cardHolderNameLabel: String
    val cardNumberLabel: String
    val cardExpiryLabel: String
    val cardCvcLabel: String
    val holdToEditCardHint: String
    val holdingToEditLabel: String

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
    val salaryDepositNow: String

    val debtsOwedByMe: String
    val debtsOwedToMe: String
    val debtAutoPay: String
    val debtDueDate: String
    val debtPaid: String
    val markAsPaid: String
    val executePayment: String

    val debtPaymentMode: String
    val debtPayInFull: String
    val debtMonthlyInstallments: String
    val monthlyInstallmentAmount: String
    val installmentDayOfMonth: String
    val remainingBalance: String
    val editDebt: String
    val makePartialPayment: String
    val partialPaymentAmount: String
    val recordInWallet: String
    val paidAmount: String
    val totalAmount: String
    val paidOutOf: String
    val deleteSettledDebts: String
    val clearSettledConfirm: String
    val filterActive: String
    val filterSettled: String
    val filterAll: String
    val fullySettled: String

    val addTransaction: String
    val addIncome: String
    val addExpense: String
    val addDebt: String
    val amount: String
    val category: String
    val titleOrPerson: String
    val expenseDescription: String
    val incomeSource: String
    val pickFromCalendar: String
    val pickFromContacts: String
    val initialPaidAmountHelp: String
    val note: String
    val date: String
    val save: String
    val cancel: String
    val delete: String
    val confirmDelete: String

    val settingsTitle: String
    val appLanguage: String
    val themeMode: String
    val themeDark: String
    val themeLight: String
    val themeSystem: String
    val styleLiquidGlass: String
    val styleNeumorphism: String

    val patternLock: String
    val drawPattern: String
    val confirmPattern: String
    val patternSaved: String
    val patternMismatch: String
    val patternTooShort: String
    val unlockApp: String
    val biometricUnlock: String
    val biometricUnlockDesc: String
    val biometricPromptTitle: String
    val biometricPromptSubtitle: String
    val biometricUnlockAction: String
    val biometricNotAvailable: String
    val biometricSettingsTitle: String
    val biometricActive: String
    val biometricDisabled: String
    val updatedLabel: String
    val breathingGlowTitle: String
    val breathingGlowActive: String
    val breathingGlowDisabled: String
    val screenshotProtectionTitle: String
    val screenshotProtectionActive: String
    val screenshotProtectionDisabled: String

    val backupDriveTitle: String
    val backupToDrive: String
    val restoreFromDrive: String
    val restoreBackup: String
    val backupSuccess: String
    val restoreSuccess: String
    val restoreError: String
    val backupInfo: String
    val pasteJsonFallback: String
    val lastBackupLabel: String
    val neverBackedUp: String

    val developer: String
    val version: String
    val emptyTransactionsTitle: String
    val emptyTransactionsDesc: String
    val emptyIncomeTitle: String
    val emptyIncomeDesc: String
    val emptyExpensesTitle: String
    val emptyExpensesDesc: String
    val emptyDebtsTitle: String
    val emptyDebtsDesc: String
    val addFirstTransaction: String
    val addFirstIncome: String
    val addFirstExpense: String
    val addFirstDebt: String

    val spendingVelocity: String
    val salaryRunway: String
    val debtPressure: String
    val savingsMilestone: String

    val searchPlaceholder: String
    val allFilter: String

    val transferToSavings: String
    val withdrawFromSavings: String
    val spendFromSavings: String
    val paidFromSavingsBadge: String

    // In-App Update Strings
    val checkForUpdates: String
    val updateBadgeLabel: String
    val updateActionButton: String
    val downloadingUpdate: String
    val installUpdateButton: String
    val grantInstallPermButton: String
    val retryUpdateButton: String
    val checkingForUpdates: String
    val alreadyUpToDate: String
    val updateCheckFailed: String
    val updateInstallPermNote: String

    fun categoryName(cat: String): String
}

object EnglishStrings : StringsBundle {
    override val currencyCode: String = "DZD"

    override val navDashboard = "Cards"
    override val navIncome = "Income"
    override val navExpenses = "Expenses"
    override val navDebts = "Debts"

    override val cardsHubTitle = "Cards Hub"
    override val cardsHubSubtitle = "Manage your payment cards & accounts"
    override val edahabiaCardTitle = "Classic Card"
    override val visaCardTitle = "Visa Premier"
    override val visaCardSubtitle = "International Multi-Currency Card"
    override val comingSoon = "Coming Soon"
    override val visaComingSoonDesc = "International payments, online subscriptions and multi-currency balances will be available very soon in Vaultix."
    override val backToCards = "Back to Cards"
    override val activeCardsCount = "2 Cards Active"
    override val currentAccount = "Current Account • CCP"
    override val editCardTitle = "Edit Classic Card"
    override val editCardSubtitle = "Customize your personal Algérie Poste card details"
    override val cardHolderNameLabel = "Full Name (Cardholder)"
    override val cardNumberLabel = "Card Number (16 digits)"
    override val cardExpiryLabel = "Expiry Date (MM/YY)"
    override val cardCvcLabel = "CVC"
    override val holdToEditCardHint = "Tap to open dashboard • Hold 0.5s to edit details"
    override val holdingToEditLabel = "Hold 0.5s to edit..."

    override val totalBalance = "Balance"
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
    override val salaryDepositNow = "Deposit Salary Now"

    override val debtsOwedByMe = "I Owe (Payable)"
    override val debtsOwedToMe = "Owed to Me (Receivable)"
    override val debtAutoPay = "Auto-payment on due date"
    override val debtDueDate = "Due Date"
    override val debtPaid = "Paid"
    override val markAsPaid = "Mark as Paid"
    override val executePayment = "Pay Now"

    override val debtPaymentMode = "Payment Plan"
    override val debtPayInFull = "Full Payment"
    override val debtMonthlyInstallments = "Monthly Recurring Installments"
    override val monthlyInstallmentAmount = "Monthly Installment (DZD)"
    override val installmentDayOfMonth = "Installment Day of Month"
    override val remainingBalance = "Remaining Balance"
    override val editDebt = "Edit Debt"
    override val makePartialPayment = "Make Partial Payment"
    override val partialPaymentAmount = "Payment Amount (DZD)"
    override val recordInWallet = "Update Wallet Balance"
    override val paidAmount = "Paid"
    override val totalAmount = "Total"
    override val paidOutOf = "paid of"
    override val deleteSettledDebts = "Clear Settled Debts"
    override val clearSettledConfirm = "Delete all settled debts? This action is permanent."
    override val filterActive = "Active"
    override val filterSettled = "Settled"
    override val filterAll = "All"
    override val fullySettled = "Fully Settled"

    override val addTransaction = "Add Transaction"
    override val addIncome = "Add Income"
    override val addExpense = "Add Expense"
    override val addDebt = "Add Debt"
    override val amount = "Amount (DZD)"
    override val category = "Category"
    override val titleOrPerson = "Title / Person"
    override val expenseDescription = "Expense Description (e.g. Internet, Bill, Shopping)"
    override val incomeSource = "Income Source / Title"
    override val pickFromCalendar = "Pick from Calendar"
    override val pickFromContacts = "Pick from Contacts"
    override val initialPaidAmountHelp = "Amount already paid previously (if any)"
    override val note = "Note (Optional)"
    override val date = "Date"
    override val save = "Save"
    override val cancel = "Cancel"
    override val delete = "Delete"
    override val confirmDelete = "Are you sure you want to delete this record?"

    override val settingsTitle = "Settings"
    override val appLanguage = "Language"
    override val themeMode = "Mode"
    override val themeDark = "Dark"
    override val themeLight = "Light"
    override val themeSystem = "System"
    override val styleLiquidGlass = "Liquid Glass"
    override val styleNeumorphism = "Neumorphism"

    override val patternLock = "Pattern Gesture Lock"
    override val drawPattern = "Draw your unlock gesture pattern"
    override val confirmPattern = "Draw the pattern again to confirm"
    override val patternSaved = "Gesture pattern saved securely"
    override val patternMismatch = "Patterns do not match. Try again."
    override val patternTooShort = "Connect at least 4 dots."
    override val unlockApp = "Unlock Vaultix"
    override val biometricUnlock = "Biometric Unlock (Fingerprint / Face)"
    override val biometricUnlockDesc = "Quick authentication via fingerprint or facial recognition"
    override val biometricPromptTitle = "Unlock Vaultix"
    override val biometricPromptSubtitle = "Touch fingerprint sensor or verify face to continue"
    override val biometricUnlockAction = "Unlock with Biometrics"
    override val biometricNotAvailable = "Biometric hardware unavailable or not enrolled"
    override val biometricSettingsTitle = "Fingerprint Authentication"
    override val biometricActive = "Enabled"
    override val biometricDisabled = "Disabled"
    override val updatedLabel = "Updated"
    override val breathingGlowTitle = "Card Breathing Glow"
    override val breathingGlowActive = "Enabled • Liquid Glass v3 aura"
    override val breathingGlowDisabled = "Disabled • Static card surfaces"
    override val screenshotProtectionTitle = "No-Screenshot Mode"
    override val screenshotProtectionActive = "Enabled • Blocked on Cards Hub"
    override val screenshotProtectionDisabled = "Disabled • Screenshots allowed"

    override val backupDriveTitle = "Google Drive Backups"
    override val backupToDrive = "Backup to Google Drive"
    override val restoreFromDrive = "Restore from Google Drive"
    override val restoreBackup = "Restore Data"
    override val backupSuccess = "Backup created! Choose Google Drive to save file."
    override val restoreSuccess = "Data restored successfully from Google Drive"
    override val restoreError = "Invalid backup format or corrupted JSON file"
    override val backupInfo = "Securely backup your financial data to Google Drive or local storage."
    override val pasteJsonFallback = "Or paste raw JSON directly"
    override val lastBackupLabel = "Last Backup: %s"
    override val neverBackedUp = "No backup performed yet"

    override val developer = "Developer"
    override val version = "Version"
    override val emptyTransactionsTitle = "No Transactions Yet"
    override val emptyTransactionsDesc = "Start tracking your financial balance by logging your first transaction."
    override val emptyIncomeTitle = "No Income Records"
    override val emptyIncomeDesc = "Record your monthly salary, freelance gigs, or returns."
    override val emptyExpensesTitle = "No Expenses Logged"
    override val emptyExpensesDesc = "Track where your money goes by adding daily purchases and bills."
    override val emptyDebtsTitle = "No Debts Recorded"
    override val emptyDebtsDesc = "Manage money you owe or money owed to you with automatic due-date plans."
    override val addFirstTransaction = "Add Transaction"
    override val addFirstIncome = "Add Income"
    override val addFirstExpense = "Add Expense"
    override val addFirstDebt = "Add Debt"

    override val spendingVelocity = "Spending Velocity"
    override val salaryRunway = "Salary Runway"
    override val debtPressure = "Debt Pressure"
    override val savingsMilestone = "Savings Milestone"

    override val searchPlaceholder = "Search transactions or notes..."
    override val allFilter = "All"

    override val transferToSavings = "Deposit to Savings"
    override val withdrawFromSavings = "Withdraw to Available"
    override val spendFromSavings = "Spend from Savings"
    override val paidFromSavingsBadge = "Savings Wallet"

    override val checkForUpdates = "Check for Updates"
    override val updateBadgeLabel = "New Update"
    override val updateActionButton = "Update"
    override val downloadingUpdate = "Downloading update..."
    override val installUpdateButton = "Install Update"
    override val grantInstallPermButton = "Allow & Install"
    override val retryUpdateButton = "Retry"
    override val checkingForUpdates = "Checking for updates..."
    override val alreadyUpToDate = "You are on the latest version"
    override val updateCheckFailed = "Could not connect to check for updates"
    override val updateInstallPermNote = "Allow installing apps from Vaultix to complete the update."

    override fun categoryName(cat: String): String {
        return when (cat.lowercase()) {
            "food", "food & dining" -> "Food & Dining"
            "transport" -> "Transport"
            "utilities", "bills", "bills & utilities" -> "Utilities"
            "shopping" -> "Shopping"
            "housing", "rent" -> "Housing & Rent"
            "health", "healthcare" -> "Healthcare"
            "entertainment" -> "Entertainment"
            "education" -> "Education"
            "internet", "telecom" -> "Internet & Phone"
            "personal_care" -> "Personal Care"
            "travel" -> "Travel & Trips"
            "savings_expense", "savings" -> "Savings Reserve"
            "taxes_fees", "taxes", "fees" -> "Taxes & Fees"
            "salary" -> "Salary"
            "freelance" -> "Freelance"
            "investment" -> "Investment"
            "transfer" -> "Transfer"
            "withdrawal" -> "Withdrawal"
            "gift" -> "Gift"
            "debt_payment" -> "Debt Payment"
            "debt_collection" -> "Debt Collection"
            "other", "other_income" -> "Other"
            else -> cat.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}

object ArabicStrings : StringsBundle {
    override val currencyCode: String = "د.ج"

    override val navDashboard = "البطاقات"
    override val navIncome = "المداخيل"
    override val navExpenses = "المصاريف"
    override val navDebts = "الديون"

    override val cardsHubTitle = "محفظة البطاقات"
    override val cardsHubSubtitle = "إدارة بطاقات الدفع والحسابات المالية"
    override val edahabiaCardTitle = "البطاقة الكلاسيكية"
    override val visaCardTitle = "بطاقة فيزا بريميير"
    override val visaCardSubtitle = "بطاقة دولية متعددة العملات"
    override val comingSoon = "قريباً"
    override val visaComingSoonDesc = "خدمات الدفع الدولي، الاشتراكات العالمية وحسابات العملات الأجنبية ستتوفر قريباً جداً في Vaultix."
    override val backToCards = "العودة إلى البطاقات"
    override val activeCardsCount = "بطاقتان نشطتان"
    override val currentAccount = "حساب جاري • CCP"
    override val editCardTitle = "تعديل معلومات البطاقة"
    override val editCardSubtitle = "تخصيص بيانات بطاقة بريد الجزائر الكلاسيكية"
    override val cardHolderNameLabel = "الاسم واللقب"
    override val cardNumberLabel = "رقم البطاقة (16 رقماً)"
    override val cardExpiryLabel = "تاريخ نهاية الصلاحية (MM/YY)"
    override val cardCvcLabel = "رمز الأمان CVC"
    override val holdToEditCardHint = "اضغط لفتح لوحة التحكم • اضغط نصف ثانية للتعديل"
    override val holdingToEditLabel = "استمر بالضغط 0.5 ث..."

    override val totalBalance = "الرصيد"
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
    override val salaryDepositNow = "إيداع الراتب الآن"

    override val debtsOwedByMe = "ديون عليّ (مستحقة للدفع)"
    override val debtsOwedToMe = "ديون لي (مستحقة للقبض)"
    override val debtAutoPay = "دفع تلقائي في تاريخ الاستحقاق"
    override val debtDueDate = "تاريخ الاستحقاق"
    override val debtPaid = "تم السداد"
    override val markAsPaid = "سداد كامل"
    override val executePayment = "سداد كامل"
    override val debtPaymentMode = "طريقة السداد"
    override val debtPayInFull = "سداد كامل في موعد الاستحقاق"
    override val debtMonthlyInstallments = "أقساط شهرية تلقائية"
    override val monthlyInstallmentAmount = "قيمة القسط الشهري (د.ج)"
    override val installmentDayOfMonth = "يوم سداد القسط من كل شهر"
    override val remainingBalance = "المبلغ المتبقي"
    override val editDebt = "تعديل الدين"
    override val makePartialPayment = "دفع جزئي"
    override val partialPaymentAmount = "مبلغ الدفعة (د.ج)"
    override val recordInWallet = "تحديث رصيد المحفظة"
    override val paidAmount = "المدفوع"
    override val totalAmount = "الإجمالي"
    override val paidOutOf = "مدفوع من"
    override val deleteSettledDebts = "مسح الديون المسددة"
    override val clearSettledConfirm = "هل تريد حذف جميع الديون المسددة نهائياً؟"
    override val filterActive = "الجارية"
    override val filterSettled = "المسددة"
    override val filterAll = "الكل"
    override val fullySettled = "مسدد بالكامل"

    override val addTransaction = "إضافة معاملة"
    override val addIncome = "إضافة دخل"
    override val addExpense = "إضافة مصروف"
    override val addDebt = "إضافة دين"
    override val amount = "المبلغ (د.ج)"
    override val category = "الفئة"
    override val titleOrPerson = "العنوان / الشخص"
    override val expenseDescription = "نوع المصروف (مثلاً: اشتراك انترنت، فواتير، تسوق، سحب...)"
    override val incomeSource = "مصدر الدخل / العنوان"
    override val pickFromCalendar = "اختيار من التقويم"
    override val pickFromContacts = "اختيار من جهات الاتصال"
    override val initialPaidAmountHelp = "المبلغ المسدد مسبقاً (إن وجد)"
    override val note = "ملاحظة (اختياري)"
    override val date = "التاريخ"
    override val save = "حفظ"
    override val cancel = "إلغاء"
    override val delete = "حذف"
    override val confirmDelete = "هل أنت متأكد من رغبتك في حذف هذا السجل؟"

    override val settingsTitle = "الإعدادات"
    override val appLanguage = "اللغة"
    override val themeMode = "الوضع"
    override val themeDark = "داكن"
    override val themeLight = "فاتح"
    override val themeSystem = "تلقائي"
    override val styleLiquidGlass = "زجاج سائل"
    override val styleNeumorphism = "نيومورفيزم"

    override val patternLock = "قفل النمط (Gesture Lock)"
    override val drawPattern = "ارسم نمط إيماءة فتح القفل"
    override val confirmPattern = "أعد رسم النمط للتأكيد"
    override val patternSaved = "تم حفظ نمط الإيماءة بنجاح"
    override val patternMismatch = "الأنماط غير متطابقة. أعد المحاولة."
    override val patternTooShort = "صل 4 نقاط على الأقل."
    override val unlockApp = "فتح قفل Vaultix"
    override val biometricUnlock = "فتح القفل بالبصمة (Fingerprint / Face ID)"
    override val biometricUnlockDesc = "دخول سريع وآمن باستخدام بصمة الإصبع أو التعرف على الوجه"
    override val biometricPromptTitle = "فتح قفل Vaultix"
    override val biometricPromptSubtitle = "المس مستشعر البصمة أو تحقق من الوجه للمتابعة"
    override val biometricUnlockAction = "فتح القفل بالبصمة"
    override val biometricNotAvailable = "ميزة البصمة غير متوفرة أو غير مسجلة بالجهاز"
    override val biometricSettingsTitle = "إعدادات التحقق بالبصمة"
    override val biometricActive = "مفعل"
    override val biometricDisabled = "معطل"
    override val updatedLabel = "آخر تحديث"
    override val breathingGlowTitle = "تأثير التوهج التنفسي للبطاقات"
    override val breathingGlowActive = "مفعل • إضاءة ديناميكية Liquid Glass v3"
    override val breathingGlowDisabled = "معطل • بطاقات ثابتة بدون توهج"
    override val screenshotProtectionTitle = "منع لقطات الشاشة (No-Screenshot)"
    override val screenshotProtectionActive = "مفعل • التصوير محظور في البطاقات"
    override val screenshotProtectionDisabled = "معطل • مسموح بتصوير الشاشة"

    override val backupDriveTitle = "إعدادات النسخ الإحتياطي"
    override val backupToDrive = "نسخ احتياطي إلى Google Drive"
    override val restoreFromDrive = "استعادة من Google Drive"
    override val restoreBackup = "استعادة البيانات"
    override val backupSuccess = "تم إنشاء النسخة! اختر Google Drive للحفظ."
    override val restoreSuccess = "تمت استعادة البيانات بنجاح من Google Drive"
    override val restoreError = "ملف النسخة الاحتياطية غير صالح أو تالف"
    override val backupInfo = "نسخ السجلات المالية بأمان إلى Google Drive أو ذاكرة الجهاز."
    override val pasteJsonFallback = "أو لصق نص JSON مباشرة"
    override val lastBackupLabel = "آخر نسخة: %s"
    override val neverBackedUp = "لم يتم النسخ بعد"

    override val developer = "المطور"
    override val version = "الإصدار"
    override val emptyTransactionsTitle = "لا توجد معاملات بعد"
    override val emptyTransactionsDesc = "ابدأ بتتبع رصيدك المالي عبر تسجيل أول معاملة لك."
    override val emptyIncomeTitle = "لا توجد مداخيل مسجلة"
    override val emptyIncomeDesc = "سجل راتبك الشهري، أرباح العمل الحر، أو العوائد المالية."
    override val emptyExpensesTitle = "لا توجد مصاريف مسجلة"
    override val emptyExpensesDesc = "راقب مصاريفك بدقة من خلال إضافة الفواتير والمشتريات اليومية."
    override val emptyDebtsTitle = "لا توجد ديون مسجلة"
    override val emptyDebtsDesc = "نظّم ما عليك من ديون وما لك لدى الآخرين مع جدول السداد التلقائي."
    override val addFirstTransaction = "إضافة معاملة"
    override val addFirstIncome = "إضافة دخل"
    override val addFirstExpense = "إضافة مصروف"
    override val addFirstDebt = "إضافة دين"

    override val spendingVelocity = "سرعة الإنفاق"
    override val salaryRunway = "مدرج الراتب"
    override val debtPressure = "ضغط الديون"
    override val savingsMilestone = "معدل الادخار"

    override val searchPlaceholder = "البحث في المعاملات أو الملاحظات..."
    override val allFilter = "الكل"

    override val transferToSavings = "تحويل للادخار"
    override val withdrawFromSavings = "سحب للمتاح"
    override val spendFromSavings = "صرف من الادخار"
    override val paidFromSavingsBadge = "محفظة الادخار"

    override val checkForUpdates = "التحقق من التحديثات"
    override val updateBadgeLabel = "تحديث جديد"
    override val updateActionButton = "تحديث"
    override val downloadingUpdate = "جاري تحميل التحديث..."
    override val installUpdateButton = "تثبيت التحديث"
    override val grantInstallPermButton = "السماح بالتثبيت"
    override val retryUpdateButton = "إعادة المحاولة"
    override val checkingForUpdates = "جاري البحث عن تحديثات..."
    override val alreadyUpToDate = "أنت تستخدم أحدث إصدار بالفعل"
    override val updateCheckFailed = "تعذر الاتصال للتحقق من التحديثات"
    override val updateInstallPermNote = "يرجى السماح بتثبيت التطبيقات من هذا المصدر لإتمام التحديث."

    override fun categoryName(cat: String): String {
        return when (cat.lowercase()) {
            "salary" -> "الراتب"
            "freelance" -> "عمل حر"
            "investment" -> "استثمار"
            "gift" -> "هدية"
            "internet", "telecom" -> "انترنت واتصالات"
            "transfer" -> "تحويل مالي"
            "withdrawal" -> "سحب نقدي"
            "food", "food & dining" -> "طعام ومطاعم"
            "shopping" -> "تسوق ومشتريات"
            "transport" -> "نقل ومواصلات"
            "utilities", "bills", "bills & utilities" -> "فواتير وخدمات"
            "housing", "rent" -> "سكن وإيجار"
            "health", "healthcare" -> "صحة وعلاج"
            "entertainment" -> "ترفيه وأنشطة"
            "education" -> "تعليم ودراسة"
            "personal_care" -> "عناية شخصية"
            "travel" -> "سفر ورحلات"
            "savings_expense", "savings" -> "محفظة الادخار"
            "taxes_fees", "taxes", "fees" -> "ضرائب ورسوم"
            "debt_payment" -> "سداد دين"
            "debt_collection" -> "تحصيل دين"
            "other", "other_income" -> "أخرى / متنوعة"
            else -> cat
        }
    }
}

val LocalStrings = staticCompositionLocalOf<StringsBundle> { EnglishStrings }
val LocalStealthMode = compositionLocalOf { false }

object AppStrings {
    val current: StringsBundle
        @Composable
        get() = LocalStrings.current
}

fun formatDzdAmount(amount: Double, currencyText: String = "DZD", isStealth: Boolean = false): String {
    if (isStealth) {
        return "•••••• $currencyText"
    }
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 0
    return "${formatter.format(amount)} $currencyText"
}

/**
 * Formats percentages correctly according to active language directionality.
 * In Arabic, uses the authentic Arabic percent symbol '٪' (\u066A), ensuring
 * bidirectional text (BiDi) does not flip parentheses or shuffle words.
 */
fun formatPercent(percent: Number, isArabic: Boolean): String {
    val p = percent.toDouble().toInt()
    return if (isArabic) "$p٪" else "$p%"
}

@Composable
fun formatPercent(percent: Number): String {
    val isArabic = LocalStrings.current is ArabicStrings
    return formatPercent(percent, isArabic)
}

@Composable
fun formatDzd(
    amount: Double, 
    currencyText: String = LocalStrings.current.currencyCode, 
    isStealth: Boolean = LocalStealthMode.current
): String {
    return formatDzdAmount(amount, currencyText, isStealth)
}

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}
