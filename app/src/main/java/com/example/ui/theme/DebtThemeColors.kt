package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Harmonized, elegant color palette for the Debts (depts) module in SaleryPulse.
 *
 * Meticulously designed for both Light Mode and Dark Mode:
 * - Light Mode: Rich, luxurious jewel tones (cognac amber, deep sapphire azure, forest emerald, ruby red)
 *   with high contrast against frosted crystal backgrounds (WCAG AA/AAA compliant > 4.5:1 ratio).
 * - Dark Mode: Soft luminous tones (warm honey amber, sky cyan, soft mint emerald, coral rose)
 *   that eliminate eye strain, glare, and harsh clashing neons while maintaining the signature Liquid Glass glow.
 */
object DebtColors {
    // Money I Owe (Payable)
    fun payable(isDark: Boolean): Color =
        if (isDark) Color(0xFFFFB74D) else Color(0xFFB45309) // Warm Honey in Dark, Rich Cognac Amber in Light

    fun payableContainer(isDark: Boolean): Color =
        if (isDark) Color(0x2BFFB74D) else Color(0x18B45309)

    fun payableBorder(isDark: Boolean): Color =
        if (isDark) Color(0x66FFB74D) else Color(0x3DB45309)

    // Money Owed to Me (Receivable)
    fun receivable(isDark: Boolean): Color =
        if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7) // Soft Sky Cyan in Dark, Deep Sapphire Azure in Light

    fun receivableContainer(isDark: Boolean): Color =
        if (isDark) Color(0x2B38BDF8) else Color(0x180284C7)

    fun receivableBorder(isDark: Boolean): Color =
        if (isDark) Color(0x6638BDF8) else Color(0x3D0284C7)

    // Settled & Paid-off Debts
    fun settled(isDark: Boolean): Color =
        if (isDark) Color(0xFF34D399) else Color(0xFF059669) // Soft Mint Emerald in Dark, Deep Forest Emerald in Light

    fun settledContainer(isDark: Boolean): Color =
        if (isDark) Color(0x2B34D399) else Color(0x18059669)

    fun settledBorder(isDark: Boolean): Color =
        if (isDark) Color(0x6634D399) else Color(0x3D059669)

    // Overdue / Deletions / Danger
    fun danger(isDark: Boolean): Color =
        if (isDark) Color(0xFFF87171) else Color(0xFFDC2626) // Soft Coral Rose in Dark, Rich Ruby Crimson in Light

    fun dangerContainer(isDark: Boolean): Color =
        if (isDark) Color(0x2BF87171) else Color(0x18DC2626)

    fun dangerBorder(isDark: Boolean): Color =
        if (isDark) Color(0x66F87171) else Color(0x3DDC2626)

    // Recurring & Auto-Payment Badge
    fun autoPay(isDark: Boolean): Color =
        if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9) // Soft Violet in Dark, Royal Violet in Light

    fun autoPayContainer(isDark: Boolean): Color =
        if (isDark) Color(0x2BA78BFA) else Color(0x186D28D9)

    fun autoPayBorder(isDark: Boolean): Color =
        if (isDark) Color(0x66A78BFA) else Color(0x3D6D28D9)
}
