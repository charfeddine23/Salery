package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class ThemeCategory(val labelEn: String, val labelAr: String) {
    ALL("All", "الكل"),
    CLASSIC("Core", "الأساسية"),
    LUXURY("Luxury", "فاخر وداكن"),
    NEON_CYBER("Neon & Cyber", "نيون ومستقبلي"),
    GLASS_METAL("Glass & Metal", "زجاج ومعدن"),
    MINIMAL_WARM("Minimal & Warm", "بسيط ودافئ")
}

@Immutable
data class AppThemeSpec(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val taglineEn: String,
    val taglineAr: String,
    val category: ThemeCategory,
    val surfaceEffect: ThemeSurfaceEffect,
    val isExistingClassic: Boolean = false,
    val darkColors: LiquidGlassColors,
    val lightColors: LiquidGlassColors
) {
    fun colors(isDark: Boolean): LiquidGlassColors = if (isDark) darkColors else lightColors
}

private fun darkPalette(
    id: String,
    bgStart: Long,
    bgCenter: Long,
    bgEnd: Long,
    glassSurface: Long,
    glassCard: Long,
    dialogBg: Long,
    border: Long,
    borderSubtle: Long,
    primary: Long,
    secondary: Long,
    textPrimary: Long = 0xFFF8FAFC,
    textSecondary: Long = 0xFF94A3B8,
    textMuted: Long = 0xFF64748B,
    success: Long = 0xFF10B981,
    warning: Long = 0xFFF59E0B,
    error: Long = 0xFFF43F5E,
    effect: ThemeSurfaceEffect = ThemeSurfaceEffect.AURORA_GLASS
): LiquidGlassColors = LiquidGlassColors(
    isDark = true,
    designStyle = id,
    bgStart = Color(bgStart),
    bgCenter = Color(bgCenter),
    bgEnd = Color(bgEnd),
    glassSurface = Color(glassSurface),
    glassCard = Color(glassCard),
    dialogBackground = Color(dialogBg),
    glassBorder = Color(border),
    glassBorderSubtle = Color(borderSubtle),
    primaryNeon = Color(primary),
    secondaryNeon = Color(secondary),
    textPrimary = Color(textPrimary),
    textSecondary = Color(textSecondary),
    textMuted = Color(textMuted),
    successColor = Color(success),
    warningColor = Color(warning),
    errorColor = Color(error),
    glassHighlightBrush = Brush.linearGradient(
        colors = listOf(Color(0x55FFFFFF), Color(0x14FFFFFF), Color(0x00FFFFFF))
    ),
    surfaceEffect = effect
)

private fun lightPalette(
    id: String,
    bgStart: Long,
    bgCenter: Long,
    bgEnd: Long,
    glassSurface: Long = 0xF7FFFFFF,
    glassCard: Long = 0xF2FFFFFF,
    dialogBg: Long = 0xFAFFFFFF,
    border: Long,
    borderSubtle: Long,
    primary: Long,
    secondary: Long,
    textPrimary: Long = 0xFF0F172A,
    textSecondary: Long = 0xFF475569,
    textMuted: Long = 0xFF64748B,
    success: Long = 0xFF059669,
    warning: Long = 0xFFD97706,
    error: Long = 0xFFDC2626,
    effect: ThemeSurfaceEffect = ThemeSurfaceEffect.AURORA_GLASS
): LiquidGlassColors = LiquidGlassColors(
    isDark = false,
    designStyle = id,
    bgStart = Color(bgStart),
    bgCenter = Color(bgCenter),
    bgEnd = Color(bgEnd),
    glassSurface = Color(glassSurface),
    glassCard = Color(glassCard),
    dialogBackground = Color(dialogBg),
    glassBorder = Color(border),
    glassBorderSubtle = Color(borderSubtle),
    primaryNeon = Color(primary),
    secondaryNeon = Color(secondary),
    textPrimary = Color(textPrimary),
    textSecondary = Color(textSecondary),
    textMuted = Color(textMuted),
    successColor = Color(success),
    warningColor = Color(warning),
    errorColor = Color(error),
    glassHighlightBrush = Brush.linearGradient(
        colors = listOf(Color(0x88FFFFFF), Color(0x28FFFFFF), Color(0x00FFFFFF))
    ),
    surfaceEffect = effect
)

object ThemeEngineCatalog {

    val allThemes: List<AppThemeSpec> = listOf(
        // 1-2: Existing Classic Themes
        AppThemeSpec(
            id = "LIQUID_GLASS",
            nameEn = "Liquid Glass",
            nameAr = "الزجاج السائل",
            taglineEn = "Translucent aurora glass & luminous cyan glow",
            taglineAr = "زجاج شفاف متوهج مع هالة سماوية مضيئة",
            category = ThemeCategory.CLASSIC,
            surfaceEffect = ThemeSurfaceEffect.AURORA_GLASS,
            isExistingClassic = true,
            darkColors = DarkLiquidGlassColors,
            lightColors = LightLiquidGlassColors
        ),
        AppThemeSpec(
            id = "NEUMORPHISM",
            nameEn = "Neumorphism",
            nameAr = "النيومورفيزم",
            taglineEn = "Tactile soft-extruded surfaces & dual bevel shadows",
            taglineAr = "أسطح مجسمة ناعمة بظلال ثلاثية الأبعاد",
            category = ThemeCategory.CLASSIC,
            surfaceEffect = ThemeSurfaceEffect.NEUMORPHIC,
            isExistingClassic = true,
            darkColors = DarkNeumorphicColors,
            lightColors = LightNeumorphicColors
        ),

        // 30 New Signature Themes
        AppThemeSpec(
            id = "DARK_LUXURY",
            nameEn = "Dark Luxury",
            nameAr = "الفخامة الداكنة",
            taglineEn = "Obsidian satin paired with burnished 24K gold",
            taglineAr = "أسود مخملي فخم مع لمسات الذهب الخالص",
            category = ThemeCategory.LUXURY,
            surfaceEffect = ThemeSurfaceEffect.AURORA_GLASS,
            darkColors = darkPalette(
                id = "DARK_LUXURY",
                bgStart = 0xFF0A0908, bgCenter = 0xFF14110E, bgEnd = 0xFF080706,
                glassSurface = 0xF517130F, glassCard = 0xED1E1812, dialogBg = 0xFA17130E,
                border = 0x77D4AF37, borderSubtle = 0x38D4AF37,
                primary = 0xFFE5C158, secondary = 0xFFC59B27,
                textPrimary = 0xFFFAF6EE, textSecondary = 0xFFD6CBB8, textMuted = 0xFF9E917C
            ),
            lightColors = lightPalette(
                id = "DARK_LUXURY",
                bgStart = 0xFFFBF8F2, bgCenter = 0xFFF4EDE0, bgEnd = 0xFFE8DEC8,
                glassSurface = 0xFAFFFFFF, glassCard = 0xF5FCF9F2, dialogBg = 0xFCFFFFFF,
                border = 0x80B08A2E, borderSubtle = 0x40C2A56B,
                primary = 0xFF9C7216, secondary = 0xFF6B4E0E,
                textPrimary = 0xFF1C1710, textSecondary = 0xFF574B3A, textMuted = 0xFF837560
            )
        ),
        AppThemeSpec(
            id = "NEO_GLASSMORPHISM",
            nameEn = "Neo-Glassmorphism",
            nameAr = "الزجاج الحديث",
            taglineEn = "Crisp crystal layers with electric iris & sky frost",
            taglineAr = "طبقات زجاجية حديثة بتوهج أزرق وبنفسجي",
            category = ThemeCategory.GLASS_METAL,
            surfaceEffect = ThemeSurfaceEffect.AURORA_GLASS,
            darkColors = darkPalette(
                id = "NEO_GLASSMORPHISM",
                bgStart = 0xFF0C1226, bgCenter = 0xFF151C3B, bgEnd = 0xFF0B1022,
                glassSurface = 0xE81B254B, glassCard = 0xDC222D5C, dialogBg = 0xF7161E3D,
                border = 0x70A5B4FC, borderSubtle = 0x3A818CF8,
                primary = 0xFF67E8F9, secondary = 0xFFA78BFA
            ),
            lightColors = lightPalette(
                id = "NEO_GLASSMORPHISM",
                bgStart = 0xFFEEF2FF, bgCenter = 0xFFE0E7FF, bgEnd = 0xFFDCEBFF,
                glassSurface = 0xEDFFFFFF, glassCard = 0xE5FFFFFF, dialogBg = 0xFAFFFFFF,
                border = 0x806366F1, borderSubtle = 0x45818CF8,
                primary = 0xFF4F46E5, secondary = 0xFF0284C7
            )
        ),
        AppThemeSpec(
            id = "CYBERPUNK_NEON",
            nameEn = "Cyberpunk Neon",
            nameAr = "النيوبنك المستقبلي",
            taglineEn = "High-voltage magenta, cyan & electric night grid",
            taglineAr = "تباين نيون مستقبلي بالماجينتا والسماوي المشع",
            category = ThemeCategory.NEON_CYBER,
            surfaceEffect = ThemeSurfaceEffect.AURORA_GLASS,
            darkColors = darkPalette(
                id = "CYBERPUNK_NEON",
                bgStart = 0xFF090414, bgCenter = 0xFF160729, bgEnd = 0xFF060C1F,
                glassSurface = 0xF0170B2B, glassCard = 0xE820103D, dialogBg = 0xFA140926,
                border = 0x88FF2A85, borderSubtle = 0x4400F0FF,
                primary = 0xFF00F0FF, secondary = 0xFFFF2A85,
                textPrimary = 0xFFFDF4FF, textSecondary = 0xFFE2B8FF, textMuted = 0xFF9F75B8,
                success = 0xFF00FFA3, warning = 0xFFFCEE09, error = 0xFFFF2A6D
            ),
            lightColors = lightPalette(
                id = "CYBERPUNK_NEON",
                bgStart = 0xFFF9ECFF, bgCenter = 0xFFECFEFF, bgEnd = 0xFFFCE7F3,
                glassSurface = 0xF7FFFFFF, glassCard = 0xF2FFFFFF, dialogBg = 0xFCFFFFFF,
                border = 0x88DB2777, borderSubtle = 0x440891B2,
                primary = 0xFFD90368, secondary = 0xFF0891B2,
                textPrimary = 0xFF1A0B2E, textSecondary = 0xFF5B377A, textMuted = 0xFF816299
            )
        ),
        AppThemeSpec(
            id = "MINIMALIST_MONOCHROME",
            nameEn = "Minimalist Monochrome",
            nameAr = "الأحادي البسيط",
            taglineEn = "Pure grayscale balance with crisp typographic poise",
            taglineAr = "توازن أحادي نقي بتدرجات الرمادي والأبيض",
            category = ThemeCategory.MINIMAL_WARM,
            surfaceEffect = ThemeSurfaceEffect.MINIMAL_FLAT,
            darkColors = darkPalette(
                id = "MINIMALIST_MONOCHROME",
                bgStart = 0xFF0D0D0D, bgCenter = 0xFF141414, bgEnd = 0xFF0A0A0A,
                glassSurface = 0xF51A1A1A, glassCard = 0xF0222222, dialogBg = 0xFA181818,
                border = 0x55FFFFFF, borderSubtle = 0x26FFFFFF,
                primary = 0xFFF5F5F5, secondary = 0xFFA3A3A3,
                textPrimary = 0xFFFAFAFA, textSecondary = 0xFFB5B5B5, textMuted = 0xFF7A7A7A
            ),
            lightColors = lightPalette(
                id = "MINIMALIST_MONOCHROME",
                bgStart = 0xFFF7F7F7, bgCenter = 0xFFEFEFEF, bgEnd = 0xFFE6E6E6,
                glassSurface = 0xFAFFFFFF, glassCard = 0xF7FFFFFF, dialogBg = 0xFCFFFFFF,
                border = 0x44171717, borderSubtle = 0x22525252,
                primary = 0xFF171717, secondary = 0xFF525252,
                textPrimary = 0xFF111111, textSecondary = 0xFF525252, textMuted = 0xFF737373
            )
        ),
        AppThemeSpec(
            id = "EMERALD_OPULENCE",
            nameEn = "Emerald Opulence",
            nameAr = "الزمرد الفاخر",
            taglineEn = "Deep imperial malachite velvet & gilded mint sheen",
            taglineAr = "زمرد ملكي عميق مع بريق ذهبي وأخضر فخم",
            category = ThemeCategory.LUXURY,
            surfaceEffect = ThemeSurfaceEffect.AURORA_GLASS,
            darkColors = darkPalette(
                id = "EMERALD_OPULENCE",
                bgStart = 0xFF041410, bgCenter = 0xFF07231C, bgEnd = 0xFF03100D,
                glassSurface = 0xF20A2920, glassCard = 0xEB0E362B, dialogBg = 0xFA08211A,
                border = 0x6634D399, borderSubtle = 0x33D4AF37,
                primary = 0xFF34D399, secondary = 0xFFEAB308,
                textPrimary = 0xFFECFDF5, textSecondary = 0xFFA7F3D0, textMuted = 0xFF6EE7B7
            ),
            lightColors = lightPalette(
                id = "EMERALD_OPULENCE",
                bgStart = 0xFFECFDF5, bgCenter = 0xFFD1FAE5, bgEnd = 0xFFE6F7F0,
                glassSurface = 0xFAFFFFFF, glassCard = 0xF4F7FFFB, dialogBg = 0xFCFFFFFF,
                border = 0x77059669, borderSubtle = 0x3810B981,
                primary = 0xFF047857, secondary = 0xFFB45309,
                textPrimary = 0xFF062C22, textSecondary = 0xFF225445, textMuted = 0xFF4D7C6E
            )
        ),
        AppThemeSpec(
            id = "CHAMPAGNE_GOLD",
            nameEn = "Champagne Gold",
            nameAr = "الذهب الشمباني",
            taglineEn = "Warm rose-gold luster on espresso & ivory silk",
            taglineAr = "بريق الذهب الشمباني الدافئ على خلفية حريرية",
            category = ThemeCategory.LUXURY,
            surfaceEffect = ThemeSurfaceEffect.METALLIC_SHEEN,
            darkColors = darkPalette(
                id = "CHAMPAGNE_GOLD",
                bgStart = 0xFF15110E, bgCenter = 0xFF211A14, bgEnd = 0xFF120E0B,
                glassSurface = 0xF2241D17, glassCard = 0xEA2E251D, dialogBg = 0xFA1E1813,
                border = 0x70F3D5A5, borderSubtle = 0x35E6C28A,
                primary = 0xFFF3D5A5, secondary = 0xFFD4A373,
                textPrimary = 0xFFFFFBF4, textSecondary = 0xFFEADBC8, textMuted = 0xFFAC9B86
            ),
            lightColors = lightPalette(
                id = "CHAMPAGNE_GOLD",
                bgStart = 0xFFFDF8F0, bgCenter = 0xFFF7ECDA, bgEnd = 0xFFF2E2C9,
                glassSurface = 0xFAFFFCF7, glassCard = 0xF5FFFDF8, dialogBg = 0xFCFFFFFF,
                border = 0x77C89D54, borderSubtle = 0x3CD4B079,
                primary = 0xFF9A6B2F, secondary = 0xFFB88644,
                textPrimary = 0xFF2B2115, textSecondary = 0xFF63513B, textMuted = 0xFF8A765C
            )
        ),
        AppThemeSpec(
            id = "OLED_MIDNIGHT",
            nameEn = "OLED Midnight",
            nameAr = "منتصف الليل الداكن",
            taglineEn = "True #000000 AMOLED void with laser cobalt rim",
            taglineAr = "أسود أموليد حقيقي موفر للطاقة بإضاءة زرقاء",
            category = ThemeCategory.LUXURY,
            surfaceEffect = ThemeSurfaceEffect.MINIMAL_FLAT,
            darkColors = darkPalette(
                id = "OLED_MIDNIGHT",
                bgStart = 0xFF000000, bgCenter = 0xFF000000, bgEnd = 0xFF020409,
                glassSurface = 0xFF0B0E14, glassCard = 0xFF111622, dialogBg = 0xFF0D111A,
                border = 0x6038BDF8, borderSubtle = 0x2E38BDF8,
                primary = 0xFF38BDF8, secondary = 0xFF818CF8,
                textPrimary = 0xFFFFFFFF, textSecondary = 0xFFA5B4CE, textMuted = 0xFF64748B
            ),
            lightColors = lightPalette(
                id = "OLED_MIDNIGHT",
                bgStart = 0xFFF0F6FF, bgCenter = 0xFFE2EEFF, bgEnd = 0xFFD9E7FC,
                glassSurface = 0xFAFFFFFF, glassCard = 0xF5FFFFFF, dialogBg = 0xFCFFFFFF,
                border = 0x700284C7, borderSubtle = 0x3564748B,
                primary = 0xFF0369A1, secondary = 0xFF4F46E5
            )
        ),
        AppThemeSpec(
            id = "ROYAL_VELVET",
            nameEn = "Royal Velvet",
            nameAr = "القطيفة الملكية",
            taglineEn = "Imperial indigo-violet plush with gold accents",
            taglineAr = "أزرق ملكي مخملي مع أرجواني فاخر ولمعان ذهبي",
            category = ThemeCategory.LUXURY,
            surfaceEffect = ThemeSurfaceEffect.AURORA_GLASS,
            darkColors = darkPalette(
                id = "ROYAL_VELVET",
                bgStart = 0xFF0F0A24, bgCenter = 0xFF1B1240, bgEnd = 0xFF120A2A,
                glassSurface = 0xF21E1542, glassCard = 0xEA281B57, dialogBg = 0xFA191138,
                border = 0x68C084FC, borderSubtle = 0x34FBBF24,
                primary = 0xFFC084FC, secondary = 0xFFFBBF24,
                textPrimary = 0xFFFAF5FF, textSecondary = 0xFFD8B4FE, textMuted = 0xFF9F7AEA
            ),
            lightColors = lightPalette(
                id = "ROYAL_VELVET",
                bgStart = 0xFFF5F3FF, bgCenter = 0xFFEDE9FE, bgEnd = 0xFFE5DCFD,
                glassSurface = 0xFAFFFFFF, glassCard = 0xF5FAF8FF, dialogBg = 0xFCFFFFFF,
                border = 0x777C3AED, borderSubtle = 0x3CA78BFA,
                primary = 0xFF6D28D9, secondary = 0xFFD97706,
                textPrimary = 0xFF1E103C, textSecondary = 0xFF4C3575, textMuted = 0xFF745B9E
            )
        ),
        AppThemeSpec(
            id = "TITANIUM_METAL",
            nameEn = "Titanium Metal",
            nameAr = "التيتانيوم المعدني",
            taglineEn = "Precision aerospace grade 5 brushed alloy finish",
            taglineAr = "مظهر معدن التيتانيوم المصقول بدقة عالية",
            category = ThemeCategory.GLASS_METAL,
            surfaceEffect = ThemeSurfaceEffect.METALLIC_SHEEN,
            darkColors = darkPalette(
                id = "TITANIUM_METAL",
                bgStart = 0xFF16191E, bgCenter = 0xFF21262D, bgEnd = 0xFF13161B,
                glassSurface = 0xF2242931, glassCard = 0xEC2B313B, dialogBg = 0xFA1E232A,
                border = 0x66B0BAC9, borderSubtle = 0x338B949E,
                primary = 0xFFD0D7DE, secondary = 0xFF79C0FF,
                textPrimary = 0xFFF0F6FC, textSecondary = 0xFFB1BAC4, textMuted = 0xFF7D8590
            ),
            lightColors = lightPalette(
                id = "TITANIUM_METAL",
                bgStart = 0xFFEAECEF, bgCenter = 0xFFDCDFE4, bgEnd = 0xFFCFD4DC,
                glassSurface = 0xF5F6F8FA, glassCard = 0xF0FFFFFF, dialogBg = 0xFCFFFFFF,
                border = 0x6657606A, borderSubtle = 0x358B949E,
                primary = 0xFF32383F, secondary = 0xFF0969DA,
                textPrimary = 0xFF1F2328, textSecondary = 0xFF4B5563, textMuted = 0xFF6E7781
            )
        ),
        AppThemeSpec(
            id = "ROSE_QUARTZ_SLATE",
            nameEn = "Rose Quartz & Slate",
            nameAr = "الكوارتز الوردي والرمادي",
            taglineEn = "Blush mineral rose softened against cool slate",
            taglineAr = "تناغم الكوارتز الوردي الرقيق مع الرمادي الهادئ",
            category = ThemeCategory.MINIMAL_WARM,
            surfaceEffect = ThemeSurfaceEffect.AURORA_GLASS,
            darkColors = darkPalette(
                id = "ROSE_QUARTZ_SLATE",
                bgStart = 0xFF161822, bgCenter = 0xFF21202E, bgEnd = 0xFF151620,
                glassSurface = 0xF2242333, glassCard = 0xEB2D2B3E, dialogBg = 0xFA1F1E2C,
                border = 0x66F9A8D4, borderSubtle = 0x3494A3B8,
                primary = 0xFFF9A8D4, secondary = 0xFF94A3B8,
                textPrimary = 0xFFFDF2F8, textSecondary = 0xFFE2D4DE, textMuted = 0xFF9CA3AF
            ),
            lightColors = lightPalette(
                id = "ROSE_QUARTZ_SLATE",
                bgStart = 0xFFFDF2F6, bgCenter = 0xFFF3ECF1, bgEnd = 0xFFE8EBEF,
                glassSurface = 0xFAFFFFFF, glassCard = 0xF5FFF9FB, dialogBg = 0xFCFFFFFF,
                border = 0x70EC4899, borderSubtle = 0x3894A3B8,
                primary = 0xFFBE185D, secondary = 0xFF475569,
                textPrimary = 0xFF1E293B, textSecondary = 0xFF475569, textMuted = 0xFF64748B
            )
        ),
        AppThemeSpec(
            id = "CYBER_ORGANIC",
            nameEn = "Cyber Organic",
            nameAr = "الطبيعي السيبراني",
            taglineEn = "Botanical forest moss infused with acid lime synth",
            taglineAr = "مزيج حيوي بين الطبيعة الخضراء والنبض السيبراني",
            category = ThemeCategory.NEON_CYBER,
            surfaceEffect = ThemeSurfaceEffect.AURORA_GLASS,
            darkColors = darkPalette(
                id = "CYBER_ORGANIC",
                bgStart = 0xFF07150F, bgCenter = 0xFF0E2419, bgEnd = 0xFF09140E,
                glassSurface = 0xF010281C, glassCard = 0xE8163625, dialogBg = 0xFA0E2217,
                border = 0x7084CC16, borderSubtle = 0x362DD4BF,
                primary = 0xFFA3E635, secondary = 0xFF2DD4BF,
                textPrimary = 0xFFF7FEE7, textSecondary = 0xFFBEF264, textMuted = 0xFF65A30D
            ),
            lightColors = lightPalette(
                id = "CYBER_ORGANIC",
                bgStart = 0xFFF2FBE9, bgCenter = 0xFFE3F7D4, bgEnd = 0xFFD9F3E7,
                glassSurface = 0xFAFFFFFF, glassCard = 0xF4F9FFF4, dialogBg = 0xFCFFFFFF,
                border = 0x7765A30D, borderSubtle = 0x380D9488,
                primary = 0xFF3F6212, secondary = 0xFF0F766E,
                textPrimary = 0xFF13260C, textSecondary = 0xFF365314, textMuted = 0xFF5D7838
            )
        ),
        AppThemeSpec(
            id = "SUNSET_GRADIENT",
            nameEn = "Sunset Gradient",
            nameAr = "تدرجات غروب الشمس",
            taglineEn = "Dusk coral horizon melting into golden tangerine",
            taglineAr = "تدرجات الغروب الدافئة من المرجاني إلى الذهبي",
            category = ThemeCategory.MINIMAL_WARM,
            surfaceEffect = ThemeSurfaceEffect.AURORA_GLASS,
            darkColors = darkPalette(
                id = "SUNSET_GRADIENT",
                bgStart = 0xFF1F0D1D, bgCenter = 0xFF2E1420, bgEnd = 0xFF1C1018,
                glassSurface = 0xEE2B1626, glassCard = 0xE5391C2B, dialogBg = 0xFA261221,
                border = 0x77FB923C, borderSubtle = 0x3DF43F5E,
                primary = 0xFFFB923C, secondary = 0xFFF43F5E,
                textPrimary = 0xFFFFF7ED, textSecondary = 0xFFFED7AA, textMuted = 0xFFC28E85
            ),
            lightColors = lightPalette(
                id = "SUNSET_GRADIENT",
                bgStart = 0xFFFFF5EB, bgCenter = 0xFFFFE4D6, bgEnd = 0xFFFCE7F3,
                glassSurface = 0xFAFFFFFF, glassCard = 0xF5FFF9F5, dialogBg = 0xFCFFFFFF,
                border = 0x77EA580C, borderSubtle = 0x3CE11D48,
                primary = 0xFFEA580C, secondary = 0xFFE11D48,
                textPrimary = 0xFF2B120C, textSecondary = 0xFF7C2D12, textMuted = 0xFF9A5B4B
            )
        ),
        AppThemeSpec(
            id = "DEEP_AMETHYST",
            nameEn = "Deep Amethyst",
            nameAr = "الجمشت العضوي",
            taglineEn = "Translucent geode violet crystal & orchid radiance",
            taglineAr = "بلورات الجمشت العميق مع وهج بنفسجي فاخر",
            category = ThemeCategory.LUXURY,
            surfaceEffect = ThemeSurfaceEffect.AURORA_GLASS,
            darkColors = darkPalette(
                id = "DEEP_AMETHYST",
                bgStart = 0xFF13091F, bgCenter = 0xFF221036, bgEnd = 0xFF11081C,
                glassSurface = 0xF2231238, glassCard = 0xEB2D1747, dialogBg = 0xFA1D0E30,
                border = 0x70D946EF, borderSubtle = 0x38A855F7,
                primary = 0xFFE879F9, secondary = 0xFFA855F7,
                textPrimary = 0xFFFDF4FF, textSecondary = 0xFFF0ABFC, textMuted = 0xFFA876B8
            ),
            lightColors = lightPalette(
                id = "DEEP_AMETHYST",
                bgStart = 0xFFFAF5FF, bgCenter = 0xFFF3E8FF, bgEnd = 0xFFFAE8FF,
                glassSurface = 0xFAFFFFFF, glassCard = 0xF5FCF8FF, dialogBg = 0xFCFFFFFF,
                border = 0x77A21CAF, borderSubtle = 0x389333EA,
                primary = 0xFF86198F, secondary = 0xFF7E22CE,
                textPrimary = 0xFF270B33, textSecondary = 0xFF581C87, textMuted = 0xFF7E4FA3
            )
        ),
        AppThemeSpec(
            id = "NEO_BRUTALIST_LUXURY",
            nameEn = "Neo-Brutalist Luxury",
            nameAr = "البروتاليزم المطور",
            taglineEn = "Architectural raw stone framed with bold saffron ink",
            taglineAr = "تصميم معماري حاد بحدود واضحة ولمسات فاخرة",
            category = ThemeCategory.LUXURY,
            surfaceEffect = ThemeSurfaceEffect.BRUTALIST_LUXURY,
            darkColors = darkPalette(
                id = "NEO_BRUTALIST_LUXURY",
                bgStart = 0xFF111111, bgCenter = 0xFF181818, bgEnd = 0xFF101010,
                glassSurface = 0xFF1E1E1E, glassCard = 0xFF252525, dialogBg = 0xFF1D1D1D,
                border = 0xFFFACC15, borderSubtle = 0x80FACC15,
                primary = 0xFFFACC15, secondary = 0xFFFB7185,
                textPrimary = 0xFFFAFAFA, textSecondary = 0xFFD4D4D8, textMuted = 0xFF9CA3AF,
                effect = ThemeSurfaceEffect.BRUTALIST_LUXURY
            ),
            lightColors = lightPalette(
                id = "NEO_BRUTALIST_LUXURY",
                bgStart = 0xFFF5F0E6, bgCenter = 0xFFEDE6D6, bgEnd = 0xFFE7DFCD,
                glassSurface = 0xFFFFFDF7, glassCard = 0xFFFFFDF9, dialogBg = 0xFFFFFFFF,
                border = 0xFF18181B, borderSubtle = 0x9918181B,
                primary = 0xFFB45309, secondary = 0xFF18181B,
                textPrimary = 0xFF111111, textSecondary = 0xFF3F3F46, textMuted = 0xFF71717A,
                effect = ThemeSurfaceEffect.BRUTALIST_LUXURY
            )
        ),
        AppThemeSpec(
            id = "LIQUID_METAL",
            nameEn = "Liquid Metal",
            nameAr = "المعدن السائل",
            taglineEn = "Molten mercury chrome reflections & specular sheen",
            taglineAr = "انسيابية الكروم السائل والانعكاسات المعدنية",
            category = ThemeCategory.GLASS_METAL,
            surfaceEffect = ThemeSurfaceEffect.METALLIC_SHEEN,
            darkColors = darkPalette(
                id = "LIQUID_METAL",
                bgStart = 0xFF10141B, bgCenter = 0xFF1D2430, bgEnd = 0xFF0E1218,
                glassSurface = 0xF21E2532, glassCard = 0xEC262F3F, dialogBg = 0xFA1A202B,
                border = 0x80E2E8F0, borderSubtle = 0x3D94A3B8,
                primary = 0xFFE2E8F0, secondary = 0xFF38BDF8,
                textPrimary = 0xFFF8FAFC, textSecondary = 0xFFCBD5E1, textMuted = 0xFF8896AA
            ),
            lightColors = lightPalette(
                id = "LIQUID_METAL",
                bgStart = 0xFFE8EDF5, bgCenter = 0xFFD8E1ED, bgEnd = 0xFFCBD5E1,
                glassSurface = 0xF5F8FAFC, glassCard = 0xF0FFFFFF, dialogBg = 0xFCFFFFFF,
                border = 0x7764748B, borderSubtle = 0x4094A3B8,
                primary = 0xFF334155, secondary = 0xFF0284C7
            )
        ),
        AppThemeSpec(
            id = "FROSTED_GLASS",
            nameEn = "Frosted Glass",
            nameAr = "الزجاج المصنفر",
            taglineEn = "Arctic glacier mist with soft diffused ice highlights",
            taglineAr = "زجاج جليدي مصنفر بإضاءة قطبية ناعمة",
            category = ThemeCategory.GLASS_METAL,
            surfaceEffect = ThemeSurfaceEffect.AURORA_GLASS,
            darkColors = darkPalette(
                id = "FROSTED_GLASS",
                bgStart = 0xFF0C1824, bgCenter = 0xFF15293B, bgEnd = 0xFF0D1B28,
                glassSurface = 0xDC1C354C, glassCard = 0xD223415C, dialogBg = 0xF515293B,
                border = 0x77BAE6FD, borderSubtle = 0x3D7DD3FC,
                primary = 0xFFBAE6FD, secondary = 0xFF7DD3FC,
                textPrimary = 0xFFF0F9FF, textSecondary = 0xFFBAE6FD, textMuted = 0xFF7FA6C4
            ),
            lightColors = lightPalette(
                id = "FROSTED_GLASS",
                bgStart = 0xFFF0F9FF, bgCenter = 0xFFE0F2FE, bgEnd = 0xFFBAE6FD,
                glassSurface = 0xE6FFFFFF, glassCard = 0xDDFFFFFF, dialogBg = 0xFAFFFFFF,
                border = 0x8038BDF8, borderSubtle = 0x457DD3FC,
                primary = 0xFF0284C7, secondary = 0xFF0369A1
            )
        ),
        AppThemeSpec(
            id = "PLATINUM_MINIMAL",
            nameEn = "Platinum Minimal",
            nameAr = "البلاتيني البسيط",
            taglineEn = "Refined silver platinum poise with cerulean whisper",
            taglineAr = "بساطة البلاتين الراقي بلمسات فضية صافية",
            category = ThemeCategory.MINIMAL_WARM,
            surfaceEffect = ThemeSurfaceEffect.MINIMAL_FLAT,
            darkColors = darkPalette(
                id = "PLATINUM_MINIMAL",
                bgStart = 0xFF121417, bgCenter = 0xFF1A1D22, bgEnd = 0xFF101215,
                glassSurface = 0xF51E2228, glassCard = 0xF0252A31, dialogBg = 0xFA191C21,
                border = 0x55CBD5E1, borderSubtle = 0x2A94A3B8,
                primary = 0xFFE5E7EB, secondary = 0xFF60A5FA
            ),
            lightColors = lightPalette(
                id = "PLATINUM_MINIMAL",
                bgStart = 0xFFF8FAFC, bgCenter = 0xFFF1F5F9, bgEnd = 0xFFE5E7EB,
                glassSurface = 0xFAFFFFFF, glassCard = 0xF7FFFFFF, dialogBg = 0xFCFFFFFF,
                border = 0x5594A3B8, borderSubtle = 0x2DCBD5E1,
                primary = 0xFF334155, secondary = 0xFF2563EB
            )
        ),
        AppThemeSpec(
            id = "COSMIC_NEBULA",
            nameEn = "Cosmic Nebula",
            nameAr = "السديم الكوني",
            taglineEn = "Deep space violet dust & starlight aurora bloom",
            taglineAr = "أعماق الفضاء الكوني بألوان السديم والنجوم",
            category = ThemeCategory.NEON_CYBER,
            surfaceEffect = ThemeSurfaceEffect.AURORA_GLASS,
            darkColors = darkPalette(
                id = "COSMIC_NEBULA",
                bgStart = 0xFF07051A, bgCenter = 0xFF140C36, bgEnd = 0xFF09122E,
                glassSurface = 0xED151038, glassCard = 0xE51E164D, dialogBg = 0xFA120C2E,
                border = 0x70A78BFA, borderSubtle = 0x3D38BDF8,
                primary = 0xFF38BDF8, secondary = 0xFFF472B6,
                textPrimary = 0xFFF5F3FF, textSecondary = 0xFFC4B5FD, textMuted = 0xFF8B7EC8
            ),
            lightColors = lightPalette(
                id = "COSMIC_NEBULA",
                bgStart = 0xFFF3F0FF, bgCenter = 0xFFE5E0FF, bgEnd = 0xFFE0F2FE,
                glassSurface = 0xFAFFFFFF, glassCard = 0xF5F8F6FF, dialogBg = 0xFCFFFFFF,
                border = 0x777C3AED, borderSubtle = 0x3D2563EB,
                primary = 0xFF5B21B6, secondary = 0xFFDB2777
            )
        ),
        AppThemeSpec(
            id = "RETRO_NOIR",
            nameEn = "Retro Noir",
            nameAr = "النوير الكلاسيكي",
            taglineEn = "1950s cinema grain with warm amber spotlight",
            taglineAr = "كلاسيكية أفلام النوير بإضاءة عنبرية دافئة",
            category = ThemeCategory.LUXURY,
            surfaceEffect = ThemeSurfaceEffect.RETRO_NOIR,
            darkColors = darkPalette(
                id = "RETRO_NOIR",
                bgStart = 0xFF0C0B09, bgCenter = 0xFF171410, bgEnd = 0xFF0A0908,
                glassSurface = 0xF51A1713, glassCard = 0xF0221E18, dialogBg = 0xFA171410,
                border = 0x60D97706, borderSubtle = 0x30A8A29E,
                primary = 0xFFF59E0B, secondary = 0xFFD6D3D1,
                textPrimary = 0xFFF5F0E6, textSecondary = 0xFFD6CFC2, textMuted = 0xFF8C8477,
                effect = ThemeSurfaceEffect.RETRO_NOIR
            ),
            lightColors = lightPalette(
                id = "RETRO_NOIR",
                bgStart = 0xFFF3EFE6, bgCenter = 0xFFE7E0D0, bgEnd = 0xFFDBD2BE,
                glassSurface = 0xFAFBF8F1, glassCard = 0xF5F7F2E8, dialogBg = 0xFCFFFDF8,
                border = 0x6692400E, borderSubtle = 0x3878716C,
                primary = 0xFF92400E, secondary = 0xFF44403C,
                textPrimary = 0xFF1C1917, textSecondary = 0xFF44403C, textMuted = 0xFF78716C,
                effect = ThemeSurfaceEffect.RETRO_NOIR
            )
        ),
        AppThemeSpec(
            id = "WARM_EARTHY_TONES",
            nameEn = "Warm Earthy Tones",
            nameAr = "درجات الأرض الدافئة",
            taglineEn = "Terracotta clay, roasted cedar & olive sage",
            taglineAr = "درجات الطين الدافئ والخشب والزيتون الطبيعي",
            category = ThemeCategory.MINIMAL_WARM,
            surfaceEffect = ThemeSurfaceEffect.MINIMAL_FLAT,
            darkColors = darkPalette(
                id = "WARM_EARTHY_TONES",
                bgStart = 0xFF18110D, bgCenter = 0xFF261A14, bgEnd = 0xFF16100C,
                glassSurface = 0xF2291D16, glassCard = 0xEC33241C, dialogBg = 0xFA221712,
                border = 0x68E07A5F, borderSubtle = 0x3581B29A,
                primary = 0xFFE07A5F, secondary = 0xFF81B29A,
                textPrimary = 0xFFFAF3EB, textSecondary = 0xFFE5D2C1, textMuted = 0xFFA68F7E
            ),
            lightColors = lightPalette(
                id = "WARM_EARTHY_TONES",
                bgStart = 0xFFFAF3EB, bgCenter = 0xFFF4E6D8, bgEnd = 0xFFEBDDD0,
                glassSurface = 0xFAFFFCF8, glassCard = 0xF5FFF9F2, dialogBg = 0xFCFFFFFF,
                border = 0x70C0563B, borderSubtle = 0x3852796F,
                primary = 0xFFB44C32, secondary = 0xFF3A6B5C,
                textPrimary = 0xFF2A1C15, textSecondary = 0xFF5E4538, textMuted = 0xFF876F61
            )
        ),
        AppThemeSpec(
            id = "CYBER_METALLIC",
            nameEn = "Cyber Metallic",
            nameAr = "المعدني السيبراني",
            taglineEn = "Gunmetal armor edges charged with electric teal",
            taglineAr = "معدن سيبراني صلب مع إضاءة فيروزية عالية التقنية",
            category = ThemeCategory.GLASS_METAL,
            surfaceEffect = ThemeSurfaceEffect.METALLIC_SHEEN,
            darkColors = darkPalette(
                id = "CYBER_METALLIC",
                bgStart = 0xFF0E131A, bgCenter = 0xFF18212C, bgEnd = 0xFF0B1016,
                glassSurface = 0xF21B2531, glassCard = 0xEC222E3D, dialogBg = 0xFA161F2A,
                border = 0x772DD4BF, borderSubtle = 0x3894A3B8,
                primary = 0xFF2DD4BF, secondary = 0xFF60A5FA
            ),
            lightColors = lightPalette(
                id = "CYBER_METALLIC",
                bgStart = 0xFFE7EFF5, bgCenter = 0xFFD9E6EE, bgEnd = 0xFFCCDAE4,
                glassSurface = 0xF5F6FAFD, glassCard = 0xF0FFFFFF, dialogBg = 0xFCFFFFFF,
                border = 0x770D9488, borderSubtle = 0x3D64748B,
                primary = 0xFF0F766E, secondary = 0xFF1D4ED8
            )
        ),
        AppThemeSpec(
            id = "DEEP_BURGUNDY",
            nameEn = "Deep Burgundy",
            nameAr = "العنابي العميق",
            taglineEn = "Rich Bordeaux crimson blended with antique brass",
            taglineAr = "عنابي فاخر عميق متناغم مع لمسات النحاس الدافئة",
            category = ThemeCategory.LUXURY,
            surfaceEffect = ThemeSurfaceEffect.AURORA_GLASS,
            darkColors = darkPalette(
                id = "DEEP_BURGUNDY",
                bgStart = 0xFF18070C, bgCenter = 0xFF2B0C16, bgEnd = 0xFF14050A,
                glassSurface = 0xF22B0E18, glassCard = 0xEC381320, dialogBg = 0xFA230B13,
                border = 0x70FB7185, borderSubtle = 0x38F59E0B,
                primary = 0xFFFB7185, secondary = 0xFFFBBF24,
                textPrimary = 0xFFFFF1F2, textSecondary = 0xFFFECDD3, textMuted = 0xFFB87E8B
            ),
            lightColors = lightPalette(
                id = "DEEP_BURGUNDY",
                bgStart = 0xFFFFF1F2, bgCenter = 0xFFFFE4E6, bgEnd = 0xFFFCE7EC,
                glassSurface = 0xFAFFFFFF, glassCard = 0xF5FFF7F8, dialogBg = 0xFCFFFFFF,
                border = 0x779F1239, borderSubtle = 0x3CF43F5E,
                primary = 0xFF881337, secondary = 0xFFB45309,
                textPrimary = 0xFF2F0814, textSecondary = 0xFF6B2136, textMuted = 0xFF945265
            )
        ),
        AppThemeSpec(
            id = "NEON_BLUEPRINT",
            nameEn = "Neon Blueprint",
            nameAr = "المخطط الازرق النيوني",
            taglineEn = "CAD architectural draft grid glowing in cobalt cyan",
            taglineAr = "مخطط هندسي تقني يتوهج بخطوط أزرق نيوني",
            category = ThemeCategory.NEON_CYBER,
            surfaceEffect = ThemeSurfaceEffect.BLUEPRINT_GRID,
            darkColors = darkPalette(
                id = "NEON_BLUEPRINT",
                bgStart = 0xFF04142E, bgCenter = 0xFF07214A, bgEnd = 0xFF031126,
                glassSurface = 0xEB09244D, glassCard = 0xE50C2D5E, dialogBg = 0xFA071D3D,
                border = 0x8000E5FF, borderSubtle = 0x4538BDF8,
                primary = 0xFF00E5FF, secondary = 0xFF60A5FA,
                textPrimary = 0xFFECF8FF, textSecondary = 0xFFBAE6FD, textMuted = 0xFF6796C4,
                effect = ThemeSurfaceEffect.BLUEPRINT_GRID
            ),
            lightColors = lightPalette(
                id = "NEON_BLUEPRINT",
                bgStart = 0xFFEDF6FF, bgCenter = 0xFFE0EEFF, bgEnd = 0xFFD4E6FC,
                glassSurface = 0xF5F8FCFF, glassCard = 0xF0FFFFFF, dialogBg = 0xFCFFFFFF,
                border = 0x800284C7, borderSubtle = 0x453B82F6,
                primary = 0xFF0369A1, secondary = 0xFF1D4ED8,
                effect = ThemeSurfaceEffect.BLUEPRINT_GRID
            )
        ),
        AppThemeSpec(
            id = "SOFT_PASTELS",
            nameEn = "Soft Pastels",
            nameAr = "الباستيل الناعم",
            taglineEn = "Dreamy lavender, mint cloud & peach macaron tones",
            taglineAr = "ألوان باستيل هادئة من اللافندر والنعناع الناعم",
            category = ThemeCategory.MINIMAL_WARM,
            surfaceEffect = ThemeSurfaceEffect.AURORA_GLASS,
            darkColors = darkPalette(
                id = "SOFT_PASTELS",
                bgStart = 0xFF181726, bgCenter = 0xFF212034, bgEnd = 0xFF171A26,
                glassSurface = 0xEE242338, glassCard = 0xE82D2B42, dialogBg = 0xFA201F32,
                border = 0x66C4B5FD, borderSubtle = 0x3899F6E4,
                primary = 0xFFC4B5FD, secondary = 0xFF99F6E4,
                textPrimary = 0xFFFDFBFF, textSecondary = 0xFFDDD6FE, textMuted = 0xFFA39DC4
            ),
            lightColors = lightPalette(
                id = "SOFT_PASTELS",
                bgStart = 0xFFFBF7FF, bgCenter = 0xFFF0FDF9, bgEnd = 0xFFFFF5F5,
                glassSurface = 0xFAFFFFFF, glassCard = 0xF4FFFFFF, dialogBg = 0xFCFFFFFF,
                border = 0x70A78BFA, borderSubtle = 0x405EEAD4,
                primary = 0xFF7C3AED, secondary = 0xFF0D9488,
                textPrimary = 0xFF1F1D36, textSecondary = 0xFF4B476B, textMuted = 0xFF777394
            )
        ),
        AppThemeSpec(
            id = "INDUSTRIAL_STEEL",
            nameEn = "Industrial Steel",
            nameAr = "الفولاذ الصناعي",
            taglineEn = "Forged iron chassis highlighted with hazard amber",
            taglineAr = "فولاذ صناعي صلب مع لمسات تحذير برتقالية أنيقة",
            category = ThemeCategory.GLASS_METAL,
            surfaceEffect = ThemeSurfaceEffect.METALLIC_SHEEN,
            darkColors = darkPalette(
                id = "INDUSTRIAL_STEEL",
                bgStart = 0xFF141618, bgCenter = 0xFF1E2226, bgEnd = 0xFF121417,
                glassSurface = 0xF522262B, glassCard = 0xF02B3036, dialogBg = 0xFA1C2024,
                border = 0x689CA3AF, borderSubtle = 0x38F97316,
                primary = 0xFFF97316, secondary = 0xFF9CA3AF,
                textPrimary = 0xFFF3F4F6, textSecondary = 0xFFD1D5DB, textMuted = 0xFF9CA3AF
            ),
            lightColors = lightPalette(
                id = "INDUSTRIAL_STEEL",
                bgStart = 0xFFECEEF0, bgCenter = 0xFFE1E4E8, bgEnd = 0xFFD5D9DF,
                glassSurface = 0xF7F9FAFB, glassCard = 0xF2FFFFFF, dialogBg = 0xFCFFFFFF,
                border = 0x664B5563, borderSubtle = 0x35EA580C,
                primary = 0xFFEA580C, secondary = 0xFF4B5563
            )
        ),
        AppThemeSpec(
            id = "HOLOGRAPHIC_HOLOGRAM",
            nameEn = "Holographic Hologram",
            nameAr = "الهولوغرافي المضيء",
            taglineEn = "Iridescent spectral prism shimmer across glass facets",
            taglineAr = "انعكاسات طيفية هولوغرافية مضيئة متعددة الألوان",
            category = ThemeCategory.NEON_CYBER,
            surfaceEffect = ThemeSurfaceEffect.HOLOGRAPHIC_PRISM,
            darkColors = darkPalette(
                id = "HOLOGRAPHIC_HOLOGRAM",
                bgStart = 0xFF0A1024, bgCenter = 0xFF191338, bgEnd = 0xFF091E2A,
                glassSurface = 0xE8161C3B, glassCard = 0xDE1E244A, dialogBg = 0xF7131833,
                border = 0x8067E8F9, borderSubtle = 0x45F472B6,
                primary = 0xFF5EEAD4, secondary = 0xFFF472B6,
                textPrimary = 0xFFF8FAFC, textSecondary = 0xFFBAE6FD, textMuted = 0xFF94A3D8,
                effect = ThemeSurfaceEffect.HOLOGRAPHIC_PRISM
            ),
            lightColors = lightPalette(
                id = "HOLOGRAPHIC_HOLOGRAM",
                bgStart = 0xFFF0FDFF, bgCenter = 0xFFF5F0FF, bgEnd = 0xFFFFF0F8,
                glassSurface = 0xEDFFFFFF, glassCard = 0xE6FFFFFF, dialogBg = 0xFAFFFFFF,
                border = 0x8006B6D4, borderSubtle = 0x45EC4899,
                primary = 0xFF0891B2, secondary = 0xFFDB2777,
                effect = ThemeSurfaceEffect.HOLOGRAPHIC_PRISM
            )
        ),
        AppThemeSpec(
            id = "OBSIDIAN_BLACK",
            nameEn = "Obsidian Black",
            nameAr = "الأسود البركاني",
            taglineEn = "Volcanic glass jet black with molten ember core",
            taglineAr = "أسود بركاني حاد مع توهج الجمر البرتقالي",
            category = ThemeCategory.LUXURY,
            surfaceEffect = ThemeSurfaceEffect.AURORA_GLASS,
            darkColors = darkPalette(
                id = "OBSIDIAN_BLACK",
                bgStart = 0xFF050506, bgCenter = 0xFF0C0B0D, bgEnd = 0xFF040405,
                glassSurface = 0xF5111013, glassCard = 0xF018161A, dialogBg = 0xFA0E0D10,
                border = 0x68FF5722, borderSubtle = 0x3052525B,
                primary = 0xFFFF6D3A, secondary = 0xFFFFAB40,
                textPrimary = 0xFFFAFAFA, textSecondary = 0xFFD4D4D8, textMuted = 0xFF71717A
            ),
            lightColors = lightPalette(
                id = "OBSIDIAN_BLACK",
                bgStart = 0xFFF4F4F5, bgCenter = 0xFFE4E4E7, bgEnd = 0xFFD4D4D8,
                glassSurface = 0xFAFFFFFF, glassCard = 0xF5FFFFFF, dialogBg = 0xFCFFFFFF,
                border = 0x70EA580C, borderSubtle = 0x3552525B,
                primary = 0xFFC2410C, secondary = 0xFF27272A
            )
        ),
        AppThemeSpec(
            id = "DUAL_TONE_CONTRAST",
            nameEn = "Dual-Tone Contrast",
            nameAr = "التباين الثنائي",
            taglineEn = "High-clarity electric cobalt versus crisp coral",
            taglineAr = "تباين ثنائي جريء بين الأزرق النابض والمرجاني",
            category = ThemeCategory.MINIMAL_WARM,
            surfaceEffect = ThemeSurfaceEffect.MINIMAL_FLAT,
            darkColors = darkPalette(
                id = "DUAL_TONE_CONTRAST",
                bgStart = 0xFF090E1A, bgCenter = 0xFF10182E, bgEnd = 0xFF0B1120,
                glassSurface = 0xF5131E38, glassCard = 0xF0192747, dialogBg = 0xFA10192E,
                border = 0x703B82F6, borderSubtle = 0x45FB7185,
                primary = 0xFF3B82F6, secondary = 0xFFFB7185
            ),
            lightColors = lightPalette(
                id = "DUAL_TONE_CONTRAST",
                bgStart = 0xFFF8FAFC, bgCenter = 0xFFEFF6FF, bgEnd = 0xFFFFF1F2,
                glassSurface = 0xFAFFFFFF, glassCard = 0xF7FFFFFF, dialogBg = 0xFCFFFFFF,
                border = 0x771D4ED8, borderSubtle = 0x45E11D48,
                primary = 0xFF1D4ED8, secondary = 0xFFE11D48
            )
        ),
        AppThemeSpec(
            id = "BIO_LUMINESCENT",
            nameEn = "Bio-Luminescent",
            nameAr = "الإضاءة الحيوية",
            taglineEn = "Abyssal ocean trench pulsing with aqua bioluminescence",
            taglineAr = "إضاءة بحرية حيوية بألوان الفيروزي والزمرد المشع",
            category = ThemeCategory.NEON_CYBER,
            surfaceEffect = ThemeSurfaceEffect.AURORA_GLASS,
            darkColors = darkPalette(
                id = "BIO_LUMINESCENT",
                bgStart = 0xFF020C14, bgCenter = 0xFF031D29, bgEnd = 0xFF021118,
                glassSurface = 0xF0052230, glassCard = 0xE8072D3E, dialogBg = 0xFA041B26,
                border = 0x7700FFCC, borderSubtle = 0x3D00B4D8,
                primary = 0xFF00FFCC, secondary = 0xFF38BDF8,
                textPrimary = 0xFFECFEFF, textSecondary = 0xFFA5F3FC, textMuted = 0xFF5EA8B8
            ),
            lightColors = lightPalette(
                id = "BIO_LUMINESCENT",
                bgStart = 0xFFECFEFF, bgCenter = 0xFFD9F9FA, bgEnd = 0xFFD1FAE5,
                glassSurface = 0xFAFFFFFF, glassCard = 0xF5F4FFFE, dialogBg = 0xFCFFFFFF,
                border = 0x770D9488, borderSubtle = 0x400284C7,
                primary = 0xFF0F766E, secondary = 0xFF0284C7
            )
        ),
        AppThemeSpec(
            id = "MODERN_SWISS",
            nameEn = "Modern Swiss",
            nameAr = "السويسري الحديث",
            taglineEn = "Clean International Typographic grid & Swiss vermilion",
            taglineAr = "المدرسة السويسرية الحديثة بدقة ونقاء أحمر سويسري",
            category = ThemeCategory.MINIMAL_WARM,
            surfaceEffect = ThemeSurfaceEffect.MINIMAL_FLAT,
            darkColors = darkPalette(
                id = "MODERN_SWISS",
                bgStart = 0xFF101113, bgCenter = 0xFF17181B, bgEnd = 0xFF101113,
                glassSurface = 0xFF1C1D21, glassCard = 0xFF23252A, dialogBg = 0xFF1A1B1F,
                border = 0x77EF4444, borderSubtle = 0x35FFFFFF,
                primary = 0xFFEF4444, secondary = 0xFFF8FAFC,
                textPrimary = 0xFFFFFFFF, textSecondary = 0xFFD1D5DB, textMuted = 0xFF9CA3AF
            ),
            lightColors = lightPalette(
                id = "MODERN_SWISS",
                bgStart = 0xFFFFFFFF, bgCenter = 0xFFF8F8F8, bgEnd = 0xFFF1F1F1,
                glassSurface = 0xFFFFFFFF, glassCard = 0xFFFFFFFF, dialogBg = 0xFFFFFFFF,
                border = 0x77DC2626, borderSubtle = 0x3318181B,
                primary = 0xFFDC2626, secondary = 0xFF111111,
                textPrimary = 0xFF111111, textSecondary = 0xFF404040, textMuted = 0xFF737373
            )
        )
    )

    private val themesById: Map<String, AppThemeSpec> = allThemes.associateBy { it.id }

    fun findById(designStyle: String): AppThemeSpec {
        return themesById[designStyle] ?: allThemes.first()
    }

    fun resolveColors(designStyle: String, isDark: Boolean): LiquidGlassColors {
        return findById(designStyle).colors(isDark)
    }
}
