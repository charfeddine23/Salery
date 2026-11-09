package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.RoseNeon

/**
 * Pre-defined expense and income categories with rich metadata (icons, colors, and localized titles).
 */
data class CategoryItem(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val icon: ImageVector,
    val color: Color,
    val isExpense: Boolean = true
)

object ExpenseCategories {

    // Pre-defined Expense Categories
    val CATEGORY_FOOD = CategoryItem(
        id = "food",
        nameEn = "Food & Dining",
        nameAr = "طعام ومطاعم",
        icon = Icons.Default.Fastfood,
        color = AmberNeon,
        isExpense = true
    )

    val CATEGORY_TRANSPORT = CategoryItem(
        id = "transport",
        nameEn = "Transport",
        nameAr = "نقل ومواصلات",
        icon = Icons.Default.DirectionsCar,
        color = CyanNeon,
        isExpense = true
    )

    val CATEGORY_UTILITIES = CategoryItem(
        id = "utilities",
        nameEn = "Utilities",
        nameAr = "فواتير وخدمات",
        icon = Icons.Default.Bolt,
        color = Color(0xFFFBBF24),
        isExpense = true
    )

    val CATEGORY_SHOPPING = CategoryItem(
        id = "shopping",
        nameEn = "Shopping",
        nameAr = "تسوق ومشتريات",
        icon = Icons.Default.ShoppingCart,
        color = RoseNeon,
        isExpense = true
    )

    val CATEGORY_HOUSING = CategoryItem(
        id = "housing",
        nameEn = "Housing & Rent",
        nameAr = "سكن وإيجار",
        icon = Icons.Default.Home,
        color = Color(0xFF8B5CF6),
        isExpense = true
    )

    val CATEGORY_HEALTH = CategoryItem(
        id = "health",
        nameEn = "Healthcare",
        nameAr = "صحة وعلاج",
        icon = Icons.Default.MedicalServices,
        color = EmeraldNeon,
        isExpense = true
    )

    val CATEGORY_ENTERTAINMENT = CategoryItem(
        id = "entertainment",
        nameEn = "Entertainment",
        nameAr = "ترفيه وأنشطة",
        icon = Icons.Default.SportsEsports,
        color = Color(0xFFA855F7),
        isExpense = true
    )

    val CATEGORY_EDUCATION = CategoryItem(
        id = "education",
        nameEn = "Education",
        nameAr = "تعليم ودراسة",
        icon = Icons.Default.School,
        color = Color(0xFF3B82F6),
        isExpense = true
    )

    val CATEGORY_INTERNET = CategoryItem(
        id = "internet",
        nameEn = "Internet & Phone",
        nameAr = "انترنت واتصالات",
        icon = Icons.Default.Wifi,
        color = Color(0xFF0EA5E9),
        isExpense = true
    )

    val CATEGORY_PERSONAL_CARE = CategoryItem(
        id = "personal_care",
        nameEn = "Personal Care",
        nameAr = "عناية شخصية",
        icon = Icons.Default.Spa,
        color = Color(0xFFEC4899),
        isExpense = true
    )

    val CATEGORY_TRAVEL = CategoryItem(
        id = "travel",
        nameEn = "Travel & Trips",
        nameAr = "سفر ورحلات",
        icon = Icons.Default.Flight,
        color = Color(0xFF14B8A6),
        isExpense = true
    )

    val CATEGORY_SAVINGS_EXPENSE = CategoryItem(
        id = "savings_expense",
        nameEn = "Savings Reserve",
        nameAr = "محفظة الادخار",
        icon = Icons.Default.Savings,
        color = AmberNeon,
        isExpense = true
    )

    val CATEGORY_OTHER_EXPENSE = CategoryItem(
        id = "other",
        nameEn = "Other Expense",
        nameAr = "مصاريف أخرى",
        icon = Icons.Default.AccountBalanceWallet,
        color = Color(0xFF94A3B8),
        isExpense = true
    )

    // Pre-defined Income Categories
    val CATEGORY_SALARY = CategoryItem(
        id = "salary",
        nameEn = "Salary",
        nameAr = "راتب شهري",
        icon = Icons.Default.Paid,
        color = EmeraldNeon,
        isExpense = false
    )

    val CATEGORY_FREELANCE = CategoryItem(
        id = "freelance",
        nameEn = "Freelance",
        nameAr = "عمل حر",
        icon = Icons.Default.Work,
        color = CyanNeon,
        isExpense = false
    )

    val CATEGORY_INVESTMENT = CategoryItem(
        id = "investment",
        nameEn = "Investment Return",
        nameAr = "عائد استثمار",
        icon = Icons.Default.TrendingUp,
        color = Color(0xFF3B82F6),
        isExpense = false
    )

    val CATEGORY_TRANSFER = CategoryItem(
        id = "transfer",
        nameEn = "Bank Transfer",
        nameAr = "تحويل مالي",
        icon = Icons.Default.Send,
        color = Color(0xFF6366F1),
        isExpense = false
    )

    val CATEGORY_GIFT = CategoryItem(
        id = "gift",
        nameEn = "Gift & Bonus",
        nameAr = "هدية ومكافأة",
        icon = Icons.Default.CardGiftcard,
        color = Color(0xFFEC4899),
        isExpense = false
    )

    val CATEGORY_OTHER_INCOME = CategoryItem(
        id = "other_income",
        nameEn = "Other Income",
        nameAr = "دخل آخر",
        icon = Icons.Default.AccountBalanceWallet,
        color = Color(0xFF94A3B8),
        isExpense = false
    )

    val ALL_EXPENSE_CATEGORIES = listOf(
        CATEGORY_FOOD,
        CATEGORY_TRANSPORT,
        CATEGORY_UTILITIES,
        CATEGORY_SHOPPING,
        CATEGORY_HOUSING,
        CATEGORY_HEALTH,
        CATEGORY_ENTERTAINMENT,
        CATEGORY_EDUCATION,
        CATEGORY_INTERNET,
        CATEGORY_PERSONAL_CARE,
        CATEGORY_TRAVEL,
        CATEGORY_SAVINGS_EXPENSE,
        CATEGORY_OTHER_EXPENSE
    )

    val ALL_INCOME_CATEGORIES = listOf(
        CATEGORY_SALARY,
        CATEGORY_FREELANCE,
        CATEGORY_INVESTMENT,
        CATEGORY_TRANSFER,
        CATEGORY_GIFT,
        CATEGORY_OTHER_INCOME
    )

    fun getExpenseCategories(): List<CategoryItem> = ALL_EXPENSE_CATEGORIES

    fun getIncomeCategories(): List<CategoryItem> = ALL_INCOME_CATEGORIES

    fun findCategory(id: String): CategoryItem {
        val lower = id.trim().lowercase()
        return (ALL_EXPENSE_CATEGORIES + ALL_INCOME_CATEGORIES).firstOrNull { it.id == lower }
            ?: when {
                lower.contains("food") || lower.contains("طعام") -> CATEGORY_FOOD
                lower.contains("trans") || lower.contains("نقل") -> CATEGORY_TRANSPORT
                lower.contains("util") || lower.contains("bill") || lower.contains("فواتير") || lower.contains("كهرباء") -> CATEGORY_UTILITIES
                lower.contains("shop") || lower.contains("تسوق") -> CATEGORY_SHOPPING
                lower.contains("house") || lower.contains("rent") || lower.contains("سكن") -> CATEGORY_HOUSING
                lower.contains("health") || lower.contains("صحة") || lower.contains("دواء") -> CATEGORY_HEALTH
                lower.contains("entertain") || lower.contains("ترفيه") -> CATEGORY_ENTERTAINMENT
                lower.contains("edu") || lower.contains("تعليم") -> CATEGORY_EDUCATION
                lower.contains("net") || lower.contains("wifi") || lower.contains("phone") || lower.contains("اتصال") -> CATEGORY_INTERNET
                lower.contains("care") || lower.contains("عناية") -> CATEGORY_PERSONAL_CARE
                lower.contains("trip") || lower.contains("travel") || lower.contains("سفر") -> CATEGORY_TRAVEL
                lower.contains("saving") || lower.contains("ادخار") -> CATEGORY_SAVINGS_EXPENSE
                lower.contains("salary") || lower.contains("راتب") -> CATEGORY_SALARY
                lower.contains("free") || lower.contains("عمل") -> CATEGORY_FREELANCE
                else -> CATEGORY_OTHER_EXPENSE
            }
    }

    fun getIcon(id: String): ImageVector = findCategory(id).icon

    fun getColor(id: String): Color = findCategory(id).color

    fun getLocalizedName(id: String, isArabic: Boolean): String {
        val item = findCategory(id)
        return if (isArabic) item.nameAr else item.nameEn
    }
}
