package com.example.data

enum class AlgerianBankMotif {
    EDAHABIA_ZELLIGE,
    EMERALD_ARCS,
    SOVEREIGN_RINGS,
    DIAGONAL_RIBBONS,
    ISLAMIC_STAR_LATTICE,
    SPLIT_DYNAMIC
}

data class AlgerianBankCardSpec(
    val id: String,
    val shortCode: String,
    val nameEn: String,
    val nameAr: String,
    val cardTypeLabel: String,
    val gradientHexes: List<Long>,
    val accentHex: Long,
    val secondaryTextHex: Long,
    val glowPrimaryHex: Long,
    val glowSecondaryHex: Long,
    val defaultNumberPrefix: String,
    val motif: AlgerianBankMotif
)

object AlgerianBankCardCatalog {
    val all: List<AlgerianBankCardSpec> = listOf(
        AlgerianBankCardSpec(
            id = "EDAHABIA",
            shortCode = "ALGÉRIE POSTE",
            nameEn = "Algérie Poste • Edahabia",
            nameAr = "بريد الجزائر • الذهبية",
            cardTypeLabel = "CLASSIC",
            gradientHexes = listOf(0xFF2563EB, 0xFF1D4ED8, 0xFF1A42A8, 0xFF15348A, 0xFF0F2566),
            accentHex = 0xFFFACC15,
            secondaryTextHex = 0xFFBAE6FD,
            glowPrimaryHex = 0xFF2563EB,
            glowSecondaryHex = 0xFF0284C7,
            defaultNumberPrefix = "6280  26••  ••••  9842",
            motif = AlgerianBankMotif.EDAHABIA_ZELLIGE
        ),
        AlgerianBankCardSpec(
            id = "BNA",
            shortCode = "BNA",
            nameEn = "Banque Nationale d'Algérie",
            nameAr = "البنك الوطني الجزائري",
            cardTypeLabel = "CIB BNA",
            gradientHexes = listOf(0xFF059669, 0xFF047857, 0xFF065F46, 0xFF064E3B, 0xFF022C22),
            accentHex = 0xFFFBBF24,
            secondaryTextHex = 0xFFA7F3D0,
            glowPrimaryHex = 0xFF10B981,
            glowSecondaryHex = 0xFF047857,
            defaultNumberPrefix = "6280  50••  ••••  4109",
            motif = AlgerianBankMotif.EMERALD_ARCS
        ),
        AlgerianBankCardSpec(
            id = "CPA",
            shortCode = "CPA",
            nameEn = "Crédit Populaire d'Algérie",
            nameAr = "القرض الشعبي الجزائري",
            cardTypeLabel = "CIB CPA",
            gradientHexes = listOf(0xFF0284C7, 0xFF0369A1, 0xFF075985, 0xFF0C4A6E, 0xFF082F49),
            accentHex = 0xFF38BDF8,
            secondaryTextHex = 0xFFBAE6FD,
            glowPrimaryHex = 0xFF0EA5E9,
            glowSecondaryHex = 0xFF0369A1,
            defaultNumberPrefix = "6280  54••  ••••  3318",
            motif = AlgerianBankMotif.SOVEREIGN_RINGS
        ),
        AlgerianBankCardSpec(
            id = "BEA",
            shortCode = "BEA",
            nameEn = "Banque Extérieure d'Algérie",
            nameAr = "بنك الجزائر الخارجي",
            cardTypeLabel = "CIB BEA",
            gradientHexes = listOf(0xFF1E3A8A, 0xFF1E1B4B, 0xFF172554, 0xFF0F172A, 0xFF090D16),
            accentHex = 0xFFFACC15,
            secondaryTextHex = 0xFFFDE68A,
            glowPrimaryHex = 0xFF3B82F6,
            glowSecondaryHex = 0xFFF59E0B,
            defaultNumberPrefix = "6280  52••  ••••  7741",
            motif = AlgerianBankMotif.SOVEREIGN_RINGS
        ),
        AlgerianBankCardSpec(
            id = "BADR",
            shortCode = "BADR",
            nameEn = "BADR Banque",
            nameAr = "بنك الفلاحة والتنمية الريفية",
            cardTypeLabel = "CIB BADR",
            gradientHexes = listOf(0xFF16A34A, 0xFF15803D, 0xFF166534, 0xFF14532D, 0xFF052E16),
            accentHex = 0xFFFDE047,
            secondaryTextHex = 0xFFBBF7D0,
            glowPrimaryHex = 0xFF22C55E,
            glowSecondaryHex = 0xFF15803D,
            defaultNumberPrefix = "6280  56••  ••••  6024",
            motif = AlgerianBankMotif.DIAGONAL_RIBBONS
        ),
        AlgerianBankCardSpec(
            id = "BDL",
            shortCode = "BDL",
            nameEn = "Banque de Développement Local",
            nameAr = "بنك التنمية المحلية",
            cardTypeLabel = "CIB BDL",
            gradientHexes = listOf(0xFF0D9488, 0xFF0F766E, 0xFF115E59, 0xFF134E4A, 0xFF042F2E),
            accentHex = 0xFF5EEAD4,
            secondaryTextHex = 0xFF99F6E4,
            glowPrimaryHex = 0xFF14B8A6,
            glowSecondaryHex = 0xFF0F766E,
            defaultNumberPrefix = "6280  58••  ••••  1950",
            motif = AlgerianBankMotif.EMERALD_ARCS
        ),
        AlgerianBankCardSpec(
            id = "CNEP",
            shortCode = "CNEP",
            nameEn = "CNEP-Banque",
            nameAr = "الصندوق الوطني للتوفير والاحتياط",
            cardTypeLabel = "CIB CNEP",
            gradientHexes = listOf(0xFFB91C1C, 0xFF991B1B, 0xFF7F1D1D, 0xFF31102F, 0xFF1E1B4B),
            accentHex = 0xFFFBBF24,
            secondaryTextHex = 0xFFFECACA,
            glowPrimaryHex = 0xFFEF4444,
            glowSecondaryHex = 0xFFB91C1C,
            defaultNumberPrefix = "6280  60••  ••••  8412",
            motif = AlgerianBankMotif.DIAGONAL_RIBBONS
        ),
        AlgerianBankCardSpec(
            id = "AGB",
            shortCode = "AGB",
            nameEn = "Algeria Gulf Bank",
            nameAr = "بنك الخليج الجزائر",
            cardTypeLabel = "CIB AGB",
            gradientHexes = listOf(0xFF7C3AED, 0xFF6D28D9, 0xFF4C1D95, 0xFF2E1065, 0xFF1A093E),
            accentHex = 0xFFC4B5FD,
            secondaryTextHex = 0xFFDDD6FE,
            glowPrimaryHex = 0xFF8B5CF6,
            glowSecondaryHex = 0xFF5B21B6,
            defaultNumberPrefix = "6280  62••  ••••  5508",
            motif = AlgerianBankMotif.SOVEREIGN_RINGS
        ),
        AlgerianBankCardSpec(
            id = "SGA",
            shortCode = "SG ALGÉRIE",
            nameEn = "Société Générale Algérie",
            nameAr = "سوسيتيه جنرال الجزائر",
            cardTypeLabel = "CIB SGA",
            gradientHexes = listOf(0xFFDC2626, 0xFF991B1B, 0xFF3F0D12, 0xFF18181B, 0xFF09090B),
            accentHex = 0xFFF87171,
            secondaryTextHex = 0xFFFECACA,
            glowPrimaryHex = 0xFFEF4444,
            glowSecondaryHex = 0xFF18181B,
            defaultNumberPrefix = "6280  64••  ••••  2091",
            motif = AlgerianBankMotif.SPLIT_DYNAMIC
        ),
        AlgerianBankCardSpec(
            id = "BNP",
            shortCode = "BNP PARIBAS",
            nameEn = "BNP Paribas El Djazaïr",
            nameAr = "بي إن بي باريبا الجزائر",
            cardTypeLabel = "CIB BNP",
            gradientHexes = listOf(0xFF10B981, 0xFF059669, 0xFF047857, 0xFF064E3B, 0xFF022C22),
            accentHex = 0xFFFFFFFF,
            secondaryTextHex = 0xFFD1FAE5,
            glowPrimaryHex = 0xFF10B981,
            glowSecondaryHex = 0xFF065F46,
            defaultNumberPrefix = "6280  66••  ••••  7134",
            motif = AlgerianBankMotif.DIAGONAL_RIBBONS
        ),
        AlgerianBankCardSpec(
            id = "NATIXIS",
            shortCode = "NATIXIS",
            nameEn = "Natixis Algérie",
            nameAr = "ناتيكسيس الجزائر",
            cardTypeLabel = "CIB NATIXIS",
            gradientHexes = listOf(0xFF9333EA, 0xFF7E22CE, 0xFF581C87, 0xFF3B0764, 0xFF1E0536),
            accentHex = 0xFFE9D5FF,
            secondaryTextHex = 0xFFF3E8FF,
            glowPrimaryHex = 0xFFA855F7,
            glowSecondaryHex = 0xFF6B21A8,
            defaultNumberPrefix = "6280  68••  ••••  4892",
            motif = AlgerianBankMotif.SPLIT_DYNAMIC
        ),
        AlgerianBankCardSpec(
            id = "HOUSING",
            shortCode = "HOUSING BANK",
            nameEn = "Housing Bank Algeria",
            nameAr = "بنك الإسكان الجزائر",
            cardTypeLabel = "CIB HBTF",
            gradientHexes = listOf(0xFF334155, 0xFF1E293B, 0xFF0F172A, 0xFF090D16, 0xFF020617),
            accentHex = 0xFFF59E0B,
            secondaryTextHex = 0xFFCBD5E1,
            glowPrimaryHex = 0xFF64748B,
            glowSecondaryHex = 0xFFF59E0B,
            defaultNumberPrefix = "6280  70••  ••••  3620",
            motif = AlgerianBankMotif.SOVEREIGN_RINGS
        ),
        AlgerianBankCardSpec(
            id = "AL_BARAKA",
            shortCode = "AL BARAKA",
            nameEn = "Banque Al Baraka",
            nameAr = "بنك البركة الجزائري",
            cardTypeLabel = "CIB BARAKA",
            gradientHexes = listOf(0xFFB45309, 0xFF92400E, 0xFF78350F, 0xFF292524, 0xFF1C1917),
            accentHex = 0xFFFCD34D,
            secondaryTextHex = 0xFFFDE68A,
            glowPrimaryHex = 0xFFF59E0B,
            glowSecondaryHex = 0xFF78350F,
            defaultNumberPrefix = "6280  72••  ••••  9045",
            motif = AlgerianBankMotif.ISLAMIC_STAR_LATTICE
        ),
        AlgerianBankCardSpec(
            id = "AL_SALAM",
            shortCode = "AL SALAM",
            nameEn = "Al Salam Bank Algeria",
            nameAr = "مصرف السلام الجزائر",
            cardTypeLabel = "CIB SALAM",
            gradientHexes = listOf(0xFF065F46, 0xFF047857, 0xFF78350F, 0xFF451A03, 0xFF1C1917),
            accentHex = 0xFFFACC15,
            secondaryTextHex = 0xFFFDE68A,
            glowPrimaryHex = 0xFF10B981,
            glowSecondaryHex = 0xFFD97706,
            defaultNumberPrefix = "6280  74••  ••••  1289",
            motif = AlgerianBankMotif.ISLAMIC_STAR_LATTICE
        ),
        AlgerianBankCardSpec(
            id = "TRUST",
            shortCode = "TRUST BANK",
            nameEn = "Trust Bank Algeria",
            nameAr = "ترست بنك الجزائر",
            cardTypeLabel = "CIB TRUST",
            gradientHexes = listOf(0xFF1D4ED8, 0xFF1E3A8A, 0xFF172554, 0xFF0F172A, 0xFF090D16),
            accentHex = 0xFFFB923C,
            secondaryTextHex = 0xFFBFDBFE,
            glowPrimaryHex = 0xFF3B82F6,
            glowSecondaryHex = 0xFFF97316,
            defaultNumberPrefix = "6280  76••  ••••  6410",
            motif = AlgerianBankMotif.SPLIT_DYNAMIC
        ),
        AlgerianBankCardSpec(
            id = "ABC",
            shortCode = "BANK ABC",
            nameEn = "Bank ABC Algeria",
            nameAr = "بنك المؤسسة العربية المصرفية",
            cardTypeLabel = "CIB ABC",
            gradientHexes = listOf(0xFF0369A1, 0xFF0284C7, 0xFF075985, 0xFF0C4A6E, 0xFF082F49),
            accentHex = 0xFF7DD3FC,
            secondaryTextHex = 0xFFE0F2FE,
            glowPrimaryHex = 0xFF0EA5E9,
            glowSecondaryHex = 0xFF0284C7,
            defaultNumberPrefix = "6280  78••  ••••  5033",
            motif = AlgerianBankMotif.EMERALD_ARCS
        ),
        AlgerianBankCardSpec(
            id = "FRANSABANK",
            shortCode = "FRANSABANK",
            nameEn = "Fransabank El Djazaïr",
            nameAr = "فرنسبنك الجزائر",
            cardTypeLabel = "CIB FRANSA",
            gradientHexes = listOf(0xFF9F1239, 0xFF881337, 0xFF4C0519, 0xFF1E293B, 0xFF0F172A),
            accentHex = 0xFFFDA4AF,
            secondaryTextHex = 0xFFFFE4E6,
            glowPrimaryHex = 0xFFE11D48,
            glowSecondaryHex = 0xFF881337,
            defaultNumberPrefix = "6280  80••  ••••  2719",
            motif = AlgerianBankMotif.DIAGONAL_RIBBONS
        )
    )

    private val byId = all.associateBy { it.id.uppercase() }

    fun findById(id: String): AlgerianBankCardSpec =
        byId[id.trim().uppercase()] ?: all.first()
}
