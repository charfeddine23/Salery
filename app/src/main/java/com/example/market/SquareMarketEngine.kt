package com.example.market

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

const val DEFAULT_SQUARE_SYNC_INTERVAL_MINUTES = 60
const val SQUARE_REFRESH_INTERVAL_MILLIS = DEFAULT_SQUARE_SYNC_INTERVAL_MINUTES * 60_000L // 1 hour
val SQUARE_SYNC_INTERVAL_OPTIONS_MINUTES = listOf(5, 15, 30, 60, 180, 360, 720, 1440, 0)
const val SQUARE_SNAPSHOT_VERSION = 3
const val TROY_OUNCE_IN_GRAMS = 31.1034768

data class HardCurrencySpec(
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val flag: String,
    val symbol: String,
    val accentHex: Long
)

@JsonClass(generateAdapter = true)
data class GoldKaratPrice(
    val karat: Int,
    val purityLabel: String,
    val titleEn: String,
    val titleAr: String,
    val pricePerGramUsd: Double,
    val pricePerGramDzdParallel: Double,
    val pricePerGramDzdBank: Double,
    val change24hParallelPercent: Double = 0.0,
    val change24hBankPercent: Double = 0.0,
    val change24hUsdPercent: Double = 0.0,
    val previousGramDzdParallel: Double = 0.0,
    val previousGramDzdBank: Double = 0.0,
    val previousGramUsd: Double = 0.0,
    val has24hBaseline: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CurrencySquareRate(
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val flag: String,
    val symbol: String,
    val bankRateDzd: Double,
    val parallelBuyDzd: Double,
    val parallelSellDzd: Double,
    val parallelSource: String = "",
    val sourceCount: Int = 0,
    val change24hParallelPercent: Double = 0.0,
    val change24hBankPercent: Double = 0.0,
    val previousParallelSellDzd: Double = 0.0,
    val previousBankRateDzd: Double = 0.0,
    val has24hBaseline: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SquareMarketSnapshot(
    val version: Int = SQUARE_SNAPSHOT_VERSION,
    val goldOzUsd: Double = 0.0,
    val gold24hChangePercent: Double = 0.0,
    val goldSource: String = "",
    val bankSource: String = "",
    val parallelSource: String = "",
    val goldSourceCount: Int = 0,
    val bankSourceCount: Int = 0,
    val parallelSourceCount: Int = 0,
    val goldKarats: List<GoldKaratPrice> = emptyList(),
    val currencies: List<CurrencySquareRate> = emptyList(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val previousGoldOzUsd: Double = 0.0,
    val hasGold24hBaseline: Boolean = false,
    val baselinePreviousDateKey: String = ""
)

@JsonClass(generateAdapter = true)
data class SquareAssetPricePoint(
    val primaryPrice: Double = 0.0,
    val secondaryPrice: Double = 0.0,
    val usdPrice: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class SquareDailyBaselineState(
    val previousDayDate: String = "",
    val previousDayPrices: Map<String, SquareAssetPricePoint> = emptyMap(),
    val isPreviousDayFromPriorDate: Boolean = false,
    val currentDayDate: String = "",
    val currentDayLatestPrices: Map<String, SquareAssetPricePoint> = emptyMap(),
    val currentDayUpdatedAtMillis: Long = 0L
)

data class GoldSpotQuote(
    val priceOzUsd: Double,
    val change24hPercent: Double,
    val source: String
)

data class ParallelPairQuote(
    val buyDzd: Double,
    val sellDzd: Double,
    val source: String = ""
)

enum class SquareSourceCategory {
    GOLD,
    BANK,
    PARALLEL
}

data class SquareMarketSourceSpec(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val domain: String,
    val category: SquareSourceCategory,
    val isNewMajor: Boolean = false,
    val secondaryTagEn: String = "",
    val secondaryTagAr: String = ""
)

object SquareMarketCatalog {
    const val SOURCE_GOLDPRICE_ORG = "goldprice_org"
    const val SOURCE_GOLD_API = "gold_api"
    const val SOURCE_YAHOO_FINANCE = "yahoo_finance"
    const val SOURCE_BINANCE_PAXG = "binance_paxg"
    const val SOURCE_COINGECKO = "coingecko"
    const val SOURCE_KRAKEN_GOLD = "kraken_gold"

    const val SOURCE_OPEN_ER_API = "open_er_api"
    const val SOURCE_FAWAZ_CURRENCY = "fawaz_currency_api"
    const val SOURCE_FLOATRATES = "floatrates"
    const val SOURCE_EXCHANGERATE_API_V4 = "exchangerate_api_v4"
    const val SOURCE_FXRATES_API = "fxrates_api"

    const val SOURCE_BINANCE_P2P = "binance_p2p"
    const val SOURCE_OKX_MARKET = "okx_market"
    const val SOURCE_BYBIT_MARKET = "bybit_market"
    const val SOURCE_DEVISESQUARE = "devisesquare"
    const val SOURCE_FOREXALGERIE = "forexalgerie"
    const val SOURCE_EURODZ = "eurodz"
    const val SOURCE_ALGERIANDINAR = "algeriandinar"

    val availableSources: List<SquareMarketSourceSpec> = listOf(
        // Gold Spot
        SquareMarketSourceSpec(
            id = SOURCE_GOLDPRICE_ORG,
            nameEn = "GoldPrice.org",
            nameAr = "غولد برايس",
            domain = "goldprice.org",
            category = SquareSourceCategory.GOLD
        ),
        SquareMarketSourceSpec(
            id = SOURCE_GOLD_API,
            nameEn = "Gold-API",
            nameAr = "غولد إيه بي آي",
            domain = "api.gold-api.com",
            category = SquareSourceCategory.GOLD
        ),
        SquareMarketSourceSpec(
            id = SOURCE_YAHOO_FINANCE,
            nameEn = "Yahoo Finance",
            nameAr = "ياهو المالية",
            domain = "finance.yahoo.com",
            category = SquareSourceCategory.GOLD
        ),
        SquareMarketSourceSpec(
            id = SOURCE_BINANCE_PAXG,
            nameEn = "Binance Spot (PAXG)",
            nameAr = "بينانس الفوري (ذهب)",
            domain = "api.binance.com",
            category = SquareSourceCategory.GOLD
        ),
        SquareMarketSourceSpec(
            id = SOURCE_COINGECKO,
            nameEn = "CoinGecko",
            nameAr = "كوين غيكو",
            domain = "api.coingecko.com",
            category = SquareSourceCategory.GOLD
        ),
        SquareMarketSourceSpec(
            id = SOURCE_KRAKEN_GOLD,
            nameEn = "Kraken",
            nameAr = "كراكن العالمية",
            domain = "api.kraken.com",
            category = SquareSourceCategory.GOLD,
            isNewMajor = true
        ),

        // Official Bank FX
        SquareMarketSourceSpec(
            id = SOURCE_OPEN_ER_API,
            nameEn = "Open ER-API",
            nameAr = "أوبن إي آر للبنوك",
            domain = "open.er-api.com",
            category = SquareSourceCategory.BANK
        ),
        SquareMarketSourceSpec(
            id = SOURCE_FAWAZ_CURRENCY,
            nameEn = "Currency-API",
            nameAr = "شبكة العملات المفتوحة",
            domain = "currency-api.pages.dev",
            category = SquareSourceCategory.BANK,
            secondaryTagEn = "Bank + Gold",
            secondaryTagAr = "بنك + ذهب"
        ),
        SquareMarketSourceSpec(
            id = SOURCE_FLOATRATES,
            nameEn = "FloatRates",
            nameAr = "فلوت ريتس",
            domain = "floatrates.com",
            category = SquareSourceCategory.BANK
        ),
        SquareMarketSourceSpec(
            id = SOURCE_EXCHANGERATE_API_V4,
            nameEn = "ExchangeRate-API",
            nameAr = "إكستشينج ريت الدولية",
            domain = "api.exchangerate-api.com",
            category = SquareSourceCategory.BANK,
            isNewMajor = true
        ),
        SquareMarketSourceSpec(
            id = SOURCE_FXRATES_API,
            nameEn = "FXRatesAPI",
            nameAr = "أف إكس ريتس",
            domain = "api.fxratesapi.com",
            category = SquareSourceCategory.BANK,
            isNewMajor = true
        ),

        // Parallel & Algerian Square
        SquareMarketSourceSpec(
            id = SOURCE_BINANCE_P2P,
            nameEn = "Binance P2P",
            nameAr = "بينانس الموازي",
            domain = "p2p.binance.com",
            category = SquareSourceCategory.PARALLEL
        ),
        SquareMarketSourceSpec(
            id = SOURCE_OKX_MARKET,
            nameEn = "OKX",
            nameAr = "أو كيه إكس",
            domain = "okx.com",
            category = SquareSourceCategory.PARALLEL,
            isNewMajor = true,
            secondaryTagEn = "P2P + Gold",
            secondaryTagAr = "موازي + ذهب"
        ),
        SquareMarketSourceSpec(
            id = SOURCE_BYBIT_MARKET,
            nameEn = "Bybit",
            nameAr = "بايبت",
            domain = "bybit.com",
            category = SquareSourceCategory.PARALLEL,
            isNewMajor = true,
            secondaryTagEn = "P2P + Gold",
            secondaryTagAr = "موازي + ذهب"
        ),
        SquareMarketSourceSpec(
            id = SOURCE_DEVISESQUARE,
            nameEn = "DeviseSquare",
            nameAr = "دوفيز سكوار",
            domain = "devisesquare.com",
            category = SquareSourceCategory.PARALLEL
        ),
        SquareMarketSourceSpec(
            id = SOURCE_FOREXALGERIE,
            nameEn = "ForexAlgérie",
            nameAr = "فوركس الجزائر",
            domain = "forexalgerie.com",
            category = SquareSourceCategory.PARALLEL
        ),
        SquareMarketSourceSpec(
            id = SOURCE_EURODZ,
            nameEn = "EuroDZ",
            nameAr = "يورو ديزاد",
            domain = "eurodz.com",
            category = SquareSourceCategory.PARALLEL
        ),
        SquareMarketSourceSpec(
            id = SOURCE_ALGERIANDINAR,
            nameEn = "AlgerianDinar",
            nameAr = "الدينار الجزائري",
            domain = "algerian-dinar.com",
            category = SquareSourceCategory.PARALLEL
        )
    )

    val ALL_SOURCE_IDS: Set<String> = availableSources.map { it.id }.toSet()

    val DEFAULT_REFERENCE_BANK_RATES: Map<String, Double> = mapOf(
        "EUR" to 145.0,
        "USD" to 134.0,
        "GBP" to 170.0,
        "CAD" to 96.0,
        "CHF" to 152.0,
        "TRY" to 4.0,
        "AED" to 36.5,
        "SAR" to 35.7,
        "TND" to 43.0,
        "MAD" to 13.5
    )

    fun localizeSquareSourceLabel(source: String, isArabic: Boolean): String {
        if (!isArabic || source.isBlank()) return source
        return source
            .replace("Binance P2P USDT", "بينانس الموازي (تيثر)", ignoreCase = true)
            .replace("Binance P2P USDC", "بينانس الموازي (دولار رقمي)", ignoreCase = true)
            .replace("Binance P2P", "بينانس الموازي", ignoreCase = true)
            .replace("Binance PAXG", "بينانس الفوري", ignoreCase = true)
            .replace("Binance", "بينانس", ignoreCase = true)
            .replace("OKX P2P USDT", "أو كيه إكس الموازي", ignoreCase = true)
            .replace("OKX XAUT", "أو كيه إكس للذهب", ignoreCase = true)
            .replace("OKX", "أو كيه إكس", ignoreCase = true)
            .replace("Bybit P2P USDT", "بايبت الموازي", ignoreCase = true)
            .replace("Bybit PAXG", "بايبت للذهب", ignoreCase = true)
            .replace("Bybit", "بايبت", ignoreCase = true)
            .replace("Kraken", "كراكن", ignoreCase = true)
            .replace("ExchangeRate-API", "إكستشينج ريت", ignoreCase = true)
            .replace("FXRatesAPI", "أف إكس ريتس", ignoreCase = true)
            .replace("Open ER-API", "أوبن إي آر", ignoreCase = true)
            .replace("Currency-Pages", "شبكة العملات", ignoreCase = true)
            .replace("Currency-API", "شبكة العملات", ignoreCase = true)
            .replace("FloatRates", "فلوت ريتس", ignoreCase = true)
            .replace("GoldPrice.org", "غولد برايس", ignoreCase = true)
            .replace("Gold-API", "غولد إيه بي آي", ignoreCase = true)
            .replace("Yahoo Finance", "ياهو المالية", ignoreCase = true)
            .replace("CoinGecko", "كوين غيكو", ignoreCase = true)
            .replace("DeviseSquare", "دوفيز سكوار", ignoreCase = true)
            .replace("ForexAlgérie", "فوركس الجزائر", ignoreCase = true)
            .replace("EuroDZ", "يورو ديزاد", ignoreCase = true)
            .replace("AlgerianDinar", "الدينار الجزائري", ignoreCase = true)
            .replace("FX Cross", "معادلة الصرف", ignoreCase = true)
            .replace("P2P/Square", "الموازي/السكوار", ignoreCase = true)
            .replace("sites", "مواقع", ignoreCase = true)
    }

    val karats: List<Triple<Int, String, Pair<String, String>>> = listOf(
        Triple(24, "99.9%", "24K Pure Gold" to "عيار 24 (ذهب خالص)"),
        Triple(22, "91.6%", "22K Gold" to "عيار 22"),
        Triple(21, "87.5%", "21K Gold" to "عيار 21"),
        Triple(18, "75.0%", "18K Local Jewelry" to "عيار 18 (الذهب المحلي)"),
        Triple(14, "58.3%", "14K Gold" to "عيار 14"),
        Triple(9, "37.5%", "9K Gold" to "عيار 9")
    )

    val trackedCurrencies: List<HardCurrencySpec> = listOf(
        HardCurrencySpec("EUR", "Euro", "اليورو الأوروبي", "🇪🇺", "€", 0xFF38BDF8),
        HardCurrencySpec("USD", "US Dollar", "الدولار الأمريكي", "🇺🇸", "$", 0xFF10B981),
        HardCurrencySpec("GBP", "British Pound", "الجنيه الإسترليني", "🇬🇧", "£", 0xFF818CF8),
        HardCurrencySpec("CAD", "Canadian Dollar", "الدولار الكندي", "🇨🇦", "C$", 0xFFF43F5E),
        HardCurrencySpec("CHF", "Swiss Franc", "الفرنك السويسري", "🇨🇭", "CHF", 0xFFFB7185),
        HardCurrencySpec("TRY", "Turkish Lira", "الليرة التركية", "🇹🇷", "₺", 0xFFF59E0B),
        HardCurrencySpec("AED", "UAE Dirham", "الدرهم الإماراتي", "🇦🇪", "د.إ", 0xFF34D399),
        HardCurrencySpec("SAR", "Saudi Riyal", "الريال السعودي", "🇸🇦", "ر.س", 0xFF22C55E),
        HardCurrencySpec("TND", "Tunisian Dinar", "الدينار التونسي", "🇹🇳", "د.ت", 0xFFEF4444),
        HardCurrencySpec("MAD", "Moroccan Dirham", "الدرهم المغربي", "🇲🇦", "د.م", 0xFFF97316)
    )

    fun findCurrencySpec(code: String): HardCurrencySpec? =
        trackedCurrencies.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }

    fun shouldRefresh(
        lastSyncMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        intervalMinutes: Int = DEFAULT_SQUARE_SYNC_INTERVAL_MINUTES
    ): Boolean {
        if (lastSyncMillis <= 0L) return true
        if (intervalMinutes <= 0) return false
        val intervalMillis = intervalMinutes.toLong() * 60_000L
        return (nowMillis - lastSyncMillis) >= intervalMillis
    }

    fun dayKeyForMillis(
        millis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
    }

    fun needsDailyRollover(
        storedBaselineJson: String,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        if (storedBaselineJson.isBlank()) return true
        val state = SquareMarketCodec.parseBaselineState(storedBaselineJson) ?: return true
        if (state.currentDayDate.isBlank()) return true
        val todayKey = dayKeyForMillis(nowMillis, timeZone)
        return todayKey > state.currentDayDate
    }

    private fun mergeAssetPricePoint(
        older: SquareAssetPricePoint?,
        newer: SquareAssetPricePoint
    ): SquareAssetPricePoint {
        if (older == null) return newer
        return SquareAssetPricePoint(
            primaryPrice = if (newer.primaryPrice > 0.0) newer.primaryPrice else older.primaryPrice,
            secondaryPrice = if (newer.secondaryPrice > 0.0) newer.secondaryPrice else older.secondaryPrice,
            usdPrice = if (newer.usdPrice > 0.0) newer.usdPrice else older.usdPrice
        )
    }

    private fun mergePriceMaps(
        older: Map<String, SquareAssetPricePoint>,
        newer: Map<String, SquareAssetPricePoint>
    ): Map<String, SquareAssetPricePoint> {
        val merged = LinkedHashMap<String, SquareAssetPricePoint>(older)
        newer.forEach { (key, point) ->
            merged[key] = mergeAssetPricePoint(merged[key], point)
        }
        return merged
    }

    private fun pctChange(currentPrice: Double, previousPrice: Double): Double {
        if (currentPrice <= 0.0 || previousPrice <= 0.0) return 0.0
        val raw = ((currentPrice - previousPrice) / previousPrice) * 100.0
        return if (!raw.isFinite() || abs(raw) < 1e-6) 0.0 else raw
    }

    /**
     * Compares today's live Square / Bank / Gold prices against each asset's stored closing price
     * from the previous day, rolling over the latest price of the finished day at midnight so every
     * 24h cycle reflects the true rise/drop percentage.
     */
    fun apply24hPriceComparison(
        fetched: SquareMarketSnapshot,
        storedBaselineJson: String,
        nowMillis: Long = fetched.updatedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Pair<SquareMarketSnapshot, String> {
        val todayKey = dayKeyForMillis(nowMillis, timeZone)
        val extractedToday = LinkedHashMap<String, SquareAssetPricePoint>()
        if (fetched.goldOzUsd > 0.0) {
            extractedToday["XAU_SPOT"] = SquareAssetPricePoint(
                primaryPrice = fetched.goldOzUsd,
                usdPrice = fetched.goldOzUsd
            )
        }
        fetched.goldKarats.forEach { k ->
            extractedToday["XAU_${k.karat}K"] = SquareAssetPricePoint(
                primaryPrice = k.pricePerGramDzdParallel,
                secondaryPrice = k.pricePerGramDzdBank,
                usdPrice = k.pricePerGramUsd
            )
        }
        fetched.currencies.forEach { fx ->
            extractedToday[fx.code] = SquareAssetPricePoint(
                primaryPrice = fx.parallelSellDzd,
                secondaryPrice = fx.bankRateDzd
            )
        }

        val existing = SquareMarketCodec.parseBaselineState(storedBaselineJson)
        val nextState: SquareDailyBaselineState = when {
            existing == null || existing.currentDayDate.isBlank() -> {
                // Seed initial Day-1 baseline; if upstream Spot Gold reported 24h %, preserve implied previous USD spot.
                val seededInitialPrev = LinkedHashMap<String, SquareAssetPricePoint>(extractedToday)
                val goldChg = fetched.gold24hChangePercent
                if (fetched.goldOzUsd > 0.0 && abs(goldChg) > 0.0001 && (1.0 + goldChg / 100.0) > 0.05) {
                    val divisor = 1.0 + goldChg / 100.0
                    val impliedPrevSpot = fetched.goldOzUsd / divisor
                    seededInitialPrev["XAU_SPOT"] = SquareAssetPricePoint(
                        primaryPrice = impliedPrevSpot,
                        usdPrice = impliedPrevSpot
                    )
                    fetched.goldKarats.forEach { k ->
                        val key = "XAU_${k.karat}K"
                        val existingPt = seededInitialPrev[key] ?: SquareAssetPricePoint()
                        seededInitialPrev[key] = existingPt.copy(
                            usdPrice = if (k.pricePerGramUsd > 0.0) k.pricePerGramUsd / divisor else existingPt.usdPrice
                        )
                    }
                }
                SquareDailyBaselineState(
                    previousDayDate = todayKey,
                    previousDayPrices = seededInitialPrev,
                    isPreviousDayFromPriorDate = false,
                    currentDayDate = todayKey,
                    currentDayLatestPrices = extractedToday,
                    currentDayUpdatedAtMillis = nowMillis
                )
            }
            todayKey > existing.currentDayDate -> {
                // New calendar day: yesterday's last closing prices become the active 24h comparison baseline.
                val rolledPrev = mergePriceMaps(existing.previousDayPrices, existing.currentDayLatestPrices)
                val completePrev = LinkedHashMap<String, SquareAssetPricePoint>(rolledPrev)
                extractedToday.forEach { (key, pt) ->
                    val prevPt = completePrev[key]
                    completePrev[key] = SquareAssetPricePoint(
                        primaryPrice = if ((prevPt?.primaryPrice ?: 0.0) > 0.0) prevPt!!.primaryPrice else pt.primaryPrice,
                        secondaryPrice = if ((prevPt?.secondaryPrice ?: 0.0) > 0.0) prevPt!!.secondaryPrice else pt.secondaryPrice,
                        usdPrice = if ((prevPt?.usdPrice ?: 0.0) > 0.0) prevPt!!.usdPrice else pt.usdPrice
                    )
                }
                SquareDailyBaselineState(
                    previousDayDate = existing.currentDayDate,
                    previousDayPrices = completePrev,
                    isPreviousDayFromPriorDate = true,
                    currentDayDate = todayKey,
                    currentDayLatestPrices = extractedToday,
                    currentDayUpdatedAtMillis = nowMillis
                )
            }
            else -> {
                // Same day (or backwards clock): keep yesterday's closing baseline frozen, update today's closing candidate.
                val updatedPrev = LinkedHashMap<String, SquareAssetPricePoint>(existing.previousDayPrices)
                extractedToday.forEach { (key, pt) ->
                    val oldPt = updatedPrev[key]
                    updatedPrev[key] = SquareAssetPricePoint(
                        primaryPrice = if ((oldPt?.primaryPrice ?: 0.0) > 0.0) oldPt!!.primaryPrice else pt.primaryPrice,
                        secondaryPrice = if ((oldPt?.secondaryPrice ?: 0.0) > 0.0) oldPt!!.secondaryPrice else pt.secondaryPrice,
                        usdPrice = if ((oldPt?.usdPrice ?: 0.0) > 0.0) oldPt!!.usdPrice else pt.usdPrice
                    )
                }
                SquareDailyBaselineState(
                    previousDayDate = existing.previousDayDate.ifBlank { todayKey },
                    previousDayPrices = updatedPrev,
                    isPreviousDayFromPriorDate = existing.isPreviousDayFromPriorDate,
                    currentDayDate = existing.currentDayDate,
                    currentDayLatestPrices = mergePriceMaps(existing.currentDayLatestPrices, extractedToday),
                    currentDayUpdatedAtMillis = nowMillis
                )
            }
        }

        val prevSpotUsd = nextState.previousDayPrices["XAU_SPOT"]?.usdPrice ?: 0.0
        val spotHasBaseline = fetched.goldOzUsd > 0.0 && prevSpotUsd > 0.0
        val computedSpotChange = if (spotHasBaseline) {
            pctChange(fetched.goldOzUsd, prevSpotUsd)
        } else {
            fetched.gold24hChangePercent
        }

        val updatedKarats = fetched.goldKarats.map { k ->
            val prevK = nextState.previousDayPrices["XAU_${k.karat}K"]
            val prevPar = prevK?.primaryPrice ?: 0.0
            val prevBnk = prevK?.secondaryPrice ?: 0.0
            val prevUsd = prevK?.usdPrice ?: 0.0
            k.copy(
                change24hParallelPercent = pctChange(k.pricePerGramDzdParallel, prevPar),
                change24hBankPercent = pctChange(k.pricePerGramDzdBank, prevBnk),
                change24hUsdPercent = pctChange(k.pricePerGramUsd, prevUsd),
                previousGramDzdParallel = prevPar,
                previousGramDzdBank = prevBnk,
                previousGramUsd = prevUsd,
                has24hBaseline = (k.pricePerGramDzdParallel > 0.0 && prevPar > 0.0) ||
                    (k.pricePerGramDzdBank > 0.0 && prevBnk > 0.0) ||
                    (k.pricePerGramUsd > 0.0 && prevUsd > 0.0)
            )
        }

        val updatedCurrencies = fetched.currencies.map { fx ->
            val prevFx = nextState.previousDayPrices[fx.code]
            val prevPar = prevFx?.primaryPrice ?: 0.0
            val prevBnk = prevFx?.secondaryPrice ?: 0.0
            fx.copy(
                change24hParallelPercent = pctChange(fx.parallelSellDzd, prevPar),
                change24hBankPercent = pctChange(fx.bankRateDzd, prevBnk),
                previousParallelSellDzd = prevPar,
                previousBankRateDzd = prevBnk,
                has24hBaseline = (fx.parallelSellDzd > 0.0 && prevPar > 0.0) ||
                    (fx.bankRateDzd > 0.0 && prevBnk > 0.0)
            )
        }

        val enrichedSnapshot = fetched.copy(
            version = SQUARE_SNAPSHOT_VERSION,
            gold24hChangePercent = computedSpotChange,
            goldKarats = updatedKarats,
            currencies = updatedCurrencies,
            previousGoldOzUsd = prevSpotUsd,
            hasGold24hBaseline = spotHasBaseline,
            baselinePreviousDateKey = if (nextState.isPreviousDayFromPriorDate) nextState.previousDayDate else ""
        )
        return enrichedSnapshot to SquareMarketCodec.encodeBaselineState(nextState)
    }

    fun buildGoldKarats(
        goldOzUsd: Double,
        usdBankDzd: Double,
        usdParallelDzd: Double
    ): List<GoldKaratPrice> {
        if (goldOzUsd <= 0.0) return emptyList()
        val base24kUsd = goldOzUsd / TROY_OUNCE_IN_GRAMS
        return karats.map { (karat, purity, titles) ->
            val factor = karat.toDouble() / 24.0
            val gramUsd = base24kUsd * factor
            GoldKaratPrice(
                karat = karat,
                purityLabel = purity,
                titleEn = titles.first,
                titleAr = titles.second,
                pricePerGramUsd = gramUsd,
                pricePerGramDzdParallel = if (usdParallelDzd > 0.0) gramUsd * usdParallelDzd else 0.0,
                pricePerGramDzdBank = if (usdBankDzd > 0.0) gramUsd * usdBankDzd else 0.0
            )
        }
    }

    private val jsonMoshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val anyMapAdapter by lazy {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        jsonMoshi.adapter<Map<String, Any?>>(type)
    }

    private fun parseJsonMap(jsonBody: String): Map<String, Any?>? {
        if (jsonBody.isBlank()) return null
        return runCatching { anyMapAdapter.fromJson(jsonBody) }.getOrNull()
    }

    private fun Any?.asDouble(): Double? = when (this) {
        is Number -> this.toDouble().takeIf { !it.isNaN() && it.isFinite() }
        is String -> this.replace(",", "").trim().toDoubleOrNull()?.takeIf { !it.isNaN() && it.isFinite() }
        else -> null
    }

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asStringAnyMap(): Map<String, Any?>? = this as? Map<String, Any?>

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asList(): List<Any?>? = this as? List<Any?>

    fun medianOf(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }

    fun filterOutliersAroundMedian(
        values: List<Double>,
        maxDeviationRatio: Double
    ): List<Double> {
        if (values.size < 3) return values
        val med = medianOf(values)
        if (med <= 0.0) return values
        val filtered = values.filter { abs(it - med) / med <= maxDeviationRatio }
        return if (filtered.size >= 2) filtered else values
    }

    fun computeGoldConsensus(quotes: List<GoldSpotQuote>): Pair<GoldSpotQuote, Int>? {
        val plausible = quotes.filter { it.priceOzUsd in 1000.0..15000.0 }
        if (plausible.isEmpty()) return null

        val med0 = medianOf(plausible.map { it.priceOzUsd })
        val agreed = if (plausible.size >= 3) {
            val tight = plausible.filter { abs(it.priceOzUsd - med0) / med0 <= 0.035 }
            if (tight.size >= 2) tight else plausible
        } else {
            plausible
        }

        val consensusPrice = medianOf(agreed.map { it.priceOzUsd })
        val nonZeroChanges = agreed.map { it.change24hPercent }.filter { abs(it) > 0.0001 }
        val consensusChange = if (nonZeroChanges.isNotEmpty()) medianOf(nonZeroChanges) else 0.0
        val distinctSources = agreed.map { it.source }.distinct()
        val sourceLabel = if (distinctSources.size == 1) {
            distinctSources.first()
        } else {
            "${distinctSources.size} sites (${distinctSources.take(3).joinToString(", ")})"
        }
        return GoldSpotQuote(
            priceOzUsd = consensusPrice,
            change24hPercent = consensusChange,
            source = sourceLabel
        ) to distinctSources.size
    }

    fun computeBankRatesConsensus(
        perSourceRates: List<Pair<String, Map<String, Double>>>
    ): Triple<Map<String, Double>, String, Int>? {
        val validSources = perSourceRates.filter { it.second.isNotEmpty() }
        if (validSources.isEmpty()) return null

        val consensus = LinkedHashMap<String, Double>()
        for (spec in trackedCurrencies) {
            val samples = validSources.mapNotNull { it.second[spec.code] }.filter { it > 0.0 }
            if (samples.isEmpty()) continue
            val filtered = filterOutliersAroundMedian(samples, maxDeviationRatio = 0.08)
            consensus[spec.code] = medianOf(filtered)
        }
        if (consensus.isEmpty()) return null

        val names = validSources.map { it.first }.distinct()
        val label = if (names.size == 1) {
            names.first()
        } else {
            "${names.size} sites (${names.take(3).joinToString(", ")})"
        }
        return Triple(consensus, label, names.size)
    }

    fun parseGoldApiResponse(jsonBody: String): GoldSpotQuote? {
        val obj = parseJsonMap(jsonBody) ?: return null
        val price = obj["price"].asDouble() ?: return null
        if (price <= 100.0) return null
        val change = obj["chp"].asDouble() ?: 0.0
        return GoldSpotQuote(priceOzUsd = price, change24hPercent = change, source = "Gold-API")
    }

    fun parseBinancePaxgResponse(jsonBody: String): GoldSpotQuote? {
        val obj = parseJsonMap(jsonBody) ?: return null
        val price = obj["lastPrice"].asDouble() ?: return null
        if (price <= 100.0) return null
        val change = obj["priceChangePercent"].asDouble() ?: 0.0
        return GoldSpotQuote(priceOzUsd = price, change24hPercent = change, source = "Binance PAXG")
    }

    fun parseGoldPriceOrgResponse(jsonBody: String): GoldSpotQuote? {
        val obj = parseJsonMap(jsonBody) ?: return null
        val items = obj["items"].asList() ?: return null
        val first = items.firstOrNull().asStringAnyMap() ?: return null
        val price = first["xauPrice"].asDouble() ?: return null
        if (price <= 100.0) return null
        val change = first["pcXau"].asDouble() ?: 0.0
        return GoldSpotQuote(priceOzUsd = price, change24hPercent = change, source = "GoldPrice.org")
    }

    fun parseCoinGeckoGoldResponse(jsonBody: String): GoldSpotQuote? {
        val obj = parseJsonMap(jsonBody) ?: return null
        val coin = (obj["pax-gold"] ?: obj["tether-gold"]).asStringAnyMap() ?: return null
        val price = coin["usd"].asDouble() ?: return null
        if (price <= 100.0) return null
        val change = coin["usd_24h_change"].asDouble() ?: 0.0
        return GoldSpotQuote(priceOzUsd = price, change24hPercent = change, source = "CoinGecko")
    }

    fun parseYahooGoldChartResponse(jsonBody: String): GoldSpotQuote? {
        val obj = parseJsonMap(jsonBody) ?: return null
        val chart = obj["chart"].asStringAnyMap() ?: return null
        val result = chart["result"].asList()?.firstOrNull().asStringAnyMap() ?: return null
        val meta = result["meta"].asStringAnyMap() ?: return null
        val price = meta["regularMarketPrice"].asDouble() ?: return null
        if (price <= 100.0) return null
        val prevClose = meta["chartPreviousClose"].asDouble() ?: meta["previousClose"].asDouble()
        val change = if (prevClose != null && prevClose > 0.0) {
            ((price - prevClose) / prevClose) * 100.0
        } else 0.0
        return GoldSpotQuote(priceOzUsd = price, change24hPercent = change, source = "Yahoo Finance")
    }

    fun parseFawazXauResponse(jsonBody: String): GoldSpotQuote? {
        val obj = parseJsonMap(jsonBody) ?: return null
        val xauObj = obj["xau"].asStringAnyMap() ?: return null
        val price = xauObj["usd"].asDouble() ?: return null
        if (price <= 100.0) return null
        return GoldSpotQuote(priceOzUsd = price, change24hPercent = 0.0, source = "Currency-API")
    }

    fun parseOpenErRatesResponse(jsonBody: String): Map<String, Double> {
        val obj = parseJsonMap(jsonBody) ?: return emptyMap()
        if (obj["result"] != "success") return emptyMap()
        val rates = obj["rates"].asStringAnyMap() ?: return emptyMap()
        val dzdPerUsd = rates["DZD"].asDouble() ?: return emptyMap()
        if (dzdPerUsd <= 0.0) return emptyMap()

        val result = LinkedHashMap<String, Double>()
        for (spec in trackedCurrencies) {
            val perUsd = if (spec.code == "USD") 1.0 else (rates[spec.code].asDouble() ?: -1.0)
            if (perUsd > 0.0) {
                result[spec.code] = dzdPerUsd / perUsd
            }
        }
        return result
    }

    fun parseFawazCurrencyResponse(jsonBody: String): Map<String, Double> {
        val obj = parseJsonMap(jsonBody) ?: return emptyMap()
        val usdObj = obj["usd"].asStringAnyMap() ?: return emptyMap()
        val dzdPerUsd = usdObj["dzd"].asDouble() ?: return emptyMap()
        if (dzdPerUsd <= 0.0) return emptyMap()

        val result = LinkedHashMap<String, Double>()
        for (spec in trackedCurrencies) {
            val lower = spec.code.lowercase(Locale.US)
            val perUsd = if (spec.code == "USD") 1.0 else (usdObj[lower].asDouble() ?: -1.0)
            if (perUsd > 0.0) {
                result[spec.code] = dzdPerUsd / perUsd
            }
        }
        return result
    }

    fun parseFloatRatesResponse(jsonBody: String): Map<String, Double> {
        val obj = parseJsonMap(jsonBody) ?: return emptyMap()
        val dzdObj = obj["dzd"].asStringAnyMap() ?: return emptyMap()
        val dzdPerUsd = dzdObj["rate"].asDouble() ?: return emptyMap()
        if (dzdPerUsd <= 0.0) return emptyMap()

        val result = LinkedHashMap<String, Double>()
        for (spec in trackedCurrencies) {
            if (spec.code == "USD") {
                result[spec.code] = dzdPerUsd
            } else {
                val entry = obj[spec.code.lowercase(Locale.US)].asStringAnyMap()
                val perUsd = entry?.get("rate").asDouble() ?: -1.0
                if (perUsd > 0.0) {
                    result[spec.code] = dzdPerUsd / perUsd
                }
            }
        }
        return result
    }

    fun parseBinanceP2pMedianDzd(jsonBody: String): Double? {
        val obj = parseJsonMap(jsonBody) ?: return null
        val data = obj["data"].asList() ?: return null
        val normalAds = mutableListOf<Double>()
        val fallbackAds = mutableListOf<Double>()

        for (row in data) {
            val rowMap = row.asStringAnyMap() ?: continue
            val adv = rowMap["adv"].asStringAnyMap() ?: continue
            val p = adv["price"].asDouble() ?: continue
            if (p !in 150.0..420.0) continue

            val qty = adv["tradableQuantity"].asDouble() ?: 0.0
            val minOrder = adv["minSingleTransAmount"].asDouble() ?: 0.0
            fallbackAds.add(p)
            // Exclude dust/promo/mega-whale outlier ads when sufficient liquid retail ads exist
            if ((qty <= 0.0 || qty >= 15.0) && minOrder <= 350_000.0) {
                normalAds.add(p)
            }
        }

        val pool = if (normalAds.size >= 3) normalAds else fallbackAds
        if (pool.isEmpty()) return null
        pool.sort()
        val trimmed = if (pool.size >= 5) {
            val cut = (pool.size * 0.2).toInt().coerceAtLeast(1)
            pool.subList(cut, pool.size - cut)
        } else {
            pool
        }
        return medianOf(trimmed)
    }

    fun parseKrakenPaxgResponse(jsonBody: String): GoldSpotQuote? {
        val obj = parseJsonMap(jsonBody) ?: return null
        val errors = obj["error"].asList()
        if (!errors.isNullOrEmpty()) return null
        val resultMap = obj["result"].asStringAnyMap() ?: return null
        val pairObj = (resultMap["PAXGUSD"] ?: resultMap.values.firstOrNull()).asStringAnyMap() ?: return null
        val lastPrice = pairObj["c"].asList()?.firstOrNull().asDouble() ?: return null
        if (lastPrice <= 100.0) return null
        val openPrice = pairObj["o"].asDouble()
        val change = if (openPrice != null && openPrice > 0.0) {
            ((lastPrice - openPrice) / openPrice) * 100.0
        } else 0.0
        return GoldSpotQuote(priceOzUsd = lastPrice, change24hPercent = change, source = "Kraken")
    }

    fun parseBybitPaxgResponse(jsonBody: String): GoldSpotQuote? {
        val obj = parseJsonMap(jsonBody) ?: return null
        if ((obj["retCode"].asDouble()?.toInt() ?: -1) != 0) return null
        val list = obj["result"].asStringAnyMap()?.get("list").asList() ?: return null
        val first = list.firstOrNull().asStringAnyMap() ?: return null
        val price = first["lastPrice"].asDouble() ?: return null
        if (price <= 100.0) return null
        val ratioChange = first["price24hPcnt"].asDouble() ?: 0.0
        return GoldSpotQuote(priceOzUsd = price, change24hPercent = ratioChange * 100.0, source = "Bybit PAXG")
    }

    fun parseOkxXautResponse(jsonBody: String): GoldSpotQuote? {
        val obj = parseJsonMap(jsonBody) ?: return null
        val codeStr = obj["code"]?.toString()?.trim()
        if (codeStr != null && codeStr != "0") return null
        val first = obj["data"].asList()?.firstOrNull().asStringAnyMap() ?: return null
        val lastPrice = first["last"].asDouble() ?: return null
        if (lastPrice <= 100.0) return null
        val open24h = first["open24h"].asDouble()
        val change = if (open24h != null && open24h > 0.0) {
            ((lastPrice - open24h) / open24h) * 100.0
        } else 0.0
        return GoldSpotQuote(priceOzUsd = lastPrice, change24hPercent = change, source = "OKX XAUT")
    }

    fun parseExchangeRateApiV4Response(jsonBody: String): Map<String, Double> {
        val obj = parseJsonMap(jsonBody) ?: return emptyMap()
        val rates = obj["rates"].asStringAnyMap() ?: return emptyMap()
        val dzdPerUsd = rates["DZD"].asDouble() ?: return emptyMap()
        if (dzdPerUsd <= 0.0) return emptyMap()

        val result = LinkedHashMap<String, Double>()
        for (spec in trackedCurrencies) {
            val perUsd = if (spec.code == "USD") 1.0 else (rates[spec.code].asDouble() ?: -1.0)
            if (perUsd > 0.0) {
                result[spec.code] = dzdPerUsd / perUsd
            }
        }
        return result
    }

    fun parseFxRatesApiResponse(jsonBody: String): Map<String, Double> {
        val obj = parseJsonMap(jsonBody) ?: return emptyMap()
        if (obj["success"] == false) return emptyMap()
        val rates = obj["rates"].asStringAnyMap() ?: return emptyMap()
        val dzdPerUsd = rates["DZD"].asDouble() ?: return emptyMap()
        if (dzdPerUsd <= 0.0) return emptyMap()

        val result = LinkedHashMap<String, Double>()
        for (spec in trackedCurrencies) {
            val perUsd = if (spec.code == "USD") 1.0 else (rates[spec.code].asDouble() ?: -1.0)
            if (perUsd > 0.0) {
                result[spec.code] = dzdPerUsd / perUsd
            }
        }
        return result
    }

    fun parseOkxP2pMedianDzd(jsonBody: String, sideKey: String): Double? {
        val obj = parseJsonMap(jsonBody) ?: return null
        val dataObj = obj["data"].asStringAnyMap() ?: return null
        val rows = dataObj[sideKey].asList()
            ?: dataObj["sell"].asList()
            ?: dataObj["buy"].asList()
            ?: return null
        val prices = rows.mapNotNull { entry ->
            entry.asStringAnyMap()?.get("price").asDouble()?.takeIf { it in 150.0..420.0 }
        }.toMutableList()
        if (prices.isEmpty()) return null
        prices.sort()
        val trimmed = if (prices.size >= 5) {
            val cut = (prices.size * 0.2).toInt().coerceAtLeast(1)
            prices.subList(cut, prices.size - cut)
        } else prices
        return medianOf(trimmed)
    }

    fun parseBybitP2pMedianDzd(jsonBody: String): Double? {
        val obj = parseJsonMap(jsonBody) ?: return null
        val resultObj = obj["result"].asStringAnyMap() ?: return null
        val items = resultObj["items"].asList() ?: return null
        val prices = items.mapNotNull { entry ->
            entry.asStringAnyMap()?.get("price").asDouble()?.takeIf { it in 150.0..420.0 }
        }.toMutableList()
        if (prices.isEmpty()) return null
        prices.sort()
        val trimmed = if (prices.size >= 5) {
            val cut = (prices.size * 0.2).toInt().coerceAtLeast(1)
            prices.subList(cut, prices.size - cut)
        } else prices
        return medianOf(trimmed)
    }

    private data class CurrencyMatchSpec(
        val code: String,
        val regexes: List<Regex>
    )

    private val currencyMatchSpecs: List<CurrencyMatchSpec> by lazy {
        val aliases = mapOf(
            "EUR" to listOf("EUR", "Euro", "Euros", "اليورو"),
            "USD" to listOf("USD", "Dollar US", "US Dollar", "Dollar", "الدولار"),
            "GBP" to listOf("GBP", "Livre Sterling", "Pound", "Sterling", "الإسترليني", "الاسترليني"),
            "CAD" to listOf("CAD", "Dollar Canadien", "Canadian Dollar", "الكندي"),
            "CHF" to listOf("CHF", "Franc Suisse", "Swiss Franc", "السويسري"),
            "TRY" to listOf("TRY", "Livre Turque", "Turkish Lira", "الليرة التركية"),
            "AED" to listOf("AED", "Dirham EAU", "Dirham des Émirats", "الدرهم الإماراتي"),
            "SAR" to listOf("SAR", "Riyal Saoudien", "Saudi Riyal", "الريال السعودي"),
            "TND" to listOf("TND", "Dinar Tunisien", "Tunisian Dinar", "الدينار التونسي"),
            "MAD" to listOf("MAD", "Dirham Marocain", "Moroccan Dirham", "الدرهم المغربي")
        )
        aliases.map { (code, tokens) ->
            CurrencyMatchSpec(
                code = code,
                regexes = tokens.map { token ->
                    // Strict word boundary across Latin & Arabic letters so "heure" NEVER matches "EUR",
                    // "country" NEVER matches "TRY", "cadre" NEVER matches "CAD".
                    Regex("(?<![A-Za-z\\u0600-\\u06FF])${Regex.escape(token)}(?![A-Za-z\\u0600-\\u06FF])", RegexOption.IGNORE_CASE)
                }
            )
        }
    }

    /**
     * Parses Algerian Square parallel DZD Buy/Sell rates strictly at row/block level with
     * word-boundary matching so French/Arabic prose never false-triggers currency extraction.
     */
    fun parseSquareHtmlQuotes(
        html: String,
        bankRates: Map<String, Double>,
        sourceLabel: String = ""
    ): Map<String, ParallelPairQuote> {
        if (html.isBlank() || bankRates.isEmpty()) return emptyMap()

        val sanitized = html
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<!--([\\s\\S]*?)-->"), " ")

        // Extract isolated table rows, list rows, or compact blocks
        val rowBlocks = mutableListOf<String>()
        Regex("<tr\\b[^>]*>([\\s\\S]*?)</tr>", RegexOption.IGNORE_CASE).findAll(sanitized).forEach {
            rowBlocks.add(it.groupValues[1])
        }
        Regex("<li\\b[^>]*>([\\s\\S]*?)</li>", RegexOption.IGNORE_CASE).findAll(sanitized).forEach {
            rowBlocks.add(it.groupValues[1])
        }
        // Fallback for div-based currency rows/cards when no <tr> matches
        if (rowBlocks.isEmpty()) {
            Regex("<div\\b[^>]*>([\\s\\S]{10,420}?)</div>", RegexOption.IGNORE_CASE).findAll(sanitized).forEach {
                if (!it.groupValues[1].contains("<div", ignoreCase = true)) {
                    rowBlocks.add(it.groupValues[1])
                }
            }
        }

        val numRegex = Regex("(?<!\\d)(\\d{1,5}(?:[.,]\\d{1,2})?)(?!\\d)")
        val thousandSpaceRegex = Regex("(?<![\\d.,])(\\d{2,3})\\s+(\\d{3})(?![\\d.,])")
        val thousandCommaRegex = Regex("(?<![\\d.,])(\\d{2,3}),(\\d{3})(?![\\d.,])")
        val results = LinkedHashMap<String, ParallelPairQuote>()

        for (rawBlock in rowBlocks) {
            val plain = rawBlock
                .replace(Regex("<[^>]+>"), " ")
                .replace("&nbsp;", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (plain.length < 4 || plain.length > 260) continue

            // Identify which tracked currencies appear in this single row; skip multi-currency headers
            val matchedCodes = currencyMatchSpecs.mapNotNull { spec ->
                if (spec.regexes.any { it.containsMatchIn(plain) }) spec.code else null
            }
            if (matchedCodes.size != 1) continue
            val code = matchedCodes.first()
            if (results.containsKey(code)) continue

            val bank = bankRates[code] ?: continue
            val minPlausible = bank * 1.15
            val maxPlausible = bank * 2.45

            // Normalize "25 800" or "25,800" into "25800", remove percentages & calendar years & "100 EUR"
            val normalizedText = plain
                .replace(thousandSpaceRegex, "$1$2")
                .replace(thousandCommaRegex, "$1$2")
                .replace(Regex("\\b20[2-3]\\d\\b"), " ")
                .replace(Regex("\\d+(?:[.,]\\d+)?\\s*%"), " ")
                .replace(Regex("\\b(?:1|10|100)\\s*(?:${Regex.escape(code)}|EUR|USD|GBP|CAD|CHF|TRY|AED|SAR|TND|MAD)\\b", RegexOption.IGNORE_CASE), " ")

            val candidates = numRegex.findAll(normalizedText)
                .mapNotNull { m ->
                    val rawStr = m.groupValues[1].replace(',', '.')
                    rawStr.toDoubleOrNull()
                }
                .map { raw ->
                    // Handle 100-unit quotes commonly displayed in Algiers (e.g. 25800 for 100 EUR -> 258.0)
                    if (raw > maxPlausible && raw / 100.0 in minPlausible..maxPlausible) {
                        raw / 100.0
                    } else {
                        raw
                    }
                }
                .filter { it in minPlausible..maxPlausible }
                .distinct()
                .toList()

            if (candidates.size >= 2) {
                val low = minOf(candidates[0], candidates[1])
                val high = maxOf(candidates[0], candidates[1])
                if ((high - low) <= high * 0.12) {
                    results[code] = ParallelPairQuote(buyDzd = low, sellDzd = high, source = sourceLabel)
                }
            } else if (candidates.size == 1) {
                val v = candidates[0]
                results[code] = ParallelPairQuote(
                    buyDzd = (v - 2.0).coerceAtLeast(minPlausible),
                    sellDzd = v,
                    source = sourceLabel
                )
            }
        }
        return results
    }
}

object SquareMarketCodec {
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val snapshotAdapter by lazy {
        moshi.adapter(SquareMarketSnapshot::class.java)
    }

    private val baselineAdapter by lazy {
        moshi.adapter(SquareDailyBaselineState::class.java)
    }

    private val stringSetType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter by lazy { moshi.adapter<List<String>>(stringSetType) }

    val DEFAULT_PINNED_IDS = setOf("XAU_SPOT", "XAU_24K", "XAU_18K", "EUR", "USD")
    const val DEFAULT_PINNED_JSON = "[\"XAU_SPOT\",\"XAU_24K\",\"XAU_18K\",\"EUR\",\"USD\"]"

    fun parsePinnedIds(json: String): Set<String> {
        if (json.isBlank()) return DEFAULT_PINNED_IDS
        return try {
            stringListAdapter.fromJson(json)
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: DEFAULT_PINNED_IDS
        } catch (_: Throwable) {
            DEFAULT_PINNED_IDS
        }
    }

    fun encodePinnedIds(ids: Set<String>): String {
        return try {
            stringListAdapter.toJson(ids.toList())
        } catch (_: Throwable) {
            DEFAULT_PINNED_JSON
        }
    }

    fun parseEnabledSourceIds(json: String): Set<String> {
        if (json.isBlank()) return SquareMarketCatalog.ALL_SOURCE_IDS
        return try {
            val parsed = stringListAdapter.fromJson(json)
                ?.map { it.trim() }
                ?.filter { it in SquareMarketCatalog.ALL_SOURCE_IDS }
                ?.toSet()
                .orEmpty()
            if (parsed.isEmpty()) SquareMarketCatalog.ALL_SOURCE_IDS else parsed
        } catch (_: Throwable) {
            SquareMarketCatalog.ALL_SOURCE_IDS
        }
    }

    fun encodeEnabledSourceIds(ids: Set<String>): String {
        val clean = ids.filter { it in SquareMarketCatalog.ALL_SOURCE_IDS }.toSet()
        if (clean.isEmpty() || clean.size == SquareMarketCatalog.ALL_SOURCE_IDS.size) return ""
        return try {
            stringListAdapter.toJson(clean.toList())
        } catch (_: Throwable) {
            ""
        }
    }

    fun encodeSnapshot(snapshot: SquareMarketSnapshot): String {
        return try {
            snapshotAdapter.toJson(snapshot)
        } catch (_: Throwable) {
            ""
        }
    }

    fun parseSnapshot(json: String): SquareMarketSnapshot? {
        if (json.isBlank()) return null
        return try {
            val parsed = snapshotAdapter.fromJson(json) ?: return null
            if (parsed.goldKarats.isEmpty() && parsed.currencies.isEmpty()) null else parsed
        } catch (_: Throwable) {
            null
        }
    }

    fun encodeBaselineState(state: SquareDailyBaselineState): String {
        return try {
            baselineAdapter.toJson(state)
        } catch (_: Throwable) {
            ""
        }
    }

    fun parseBaselineState(json: String): SquareDailyBaselineState? {
        if (json.isBlank()) return null
        return try {
            baselineAdapter.fromJson(json)
        } catch (_: Throwable) {
            null
        }
    }
}

object SquareMarketFetcher {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getString(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json, text/html, */*")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Vaultix/1.0")
            .build()
        return client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} for $url")
            resp.body?.string().orEmpty()
        }
    }

    private fun postJson(url: String, bodyJson: String): String {
        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody(jsonMediaType))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Vaultix/1.0")
            .build()
        return client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} for $url")
            resp.body?.string().orEmpty()
        }
    }

    suspend fun fetchLiveSnapshot(
        enabledSourceIds: Set<String> = SquareMarketCatalog.ALL_SOURCE_IDS
    ): Result<SquareMarketSnapshot> = withContext(Dispatchers.IO) {
        val activeSources = if (enabledSourceIds.isEmpty()) {
            SquareMarketCatalog.ALL_SOURCE_IDS
        } else {
            enabledSourceIds.filterTo(mutableSetOf()) { it in SquareMarketCatalog.ALL_SOURCE_IDS }
                .ifEmpty { SquareMarketCatalog.ALL_SOURCE_IDS }
        }
        val now = System.currentTimeMillis()
        coroutineScope {
            val goldDeferred = async { fetchGoldSpotMultiSource(activeSources) }
            val bankDeferred = async { fetchOfficialBankMultiSource(activeSources) }
            val binanceP2pDeferred = async {
                if (SquareMarketCatalog.SOURCE_BINANCE_P2P in activeSources) fetchAllBinanceP2pDzd()
                else emptyList()
            }
            val okxP2pDeferred = async {
                if (SquareMarketCatalog.SOURCE_OKX_MARKET in activeSources) listOfNotNull(fetchOkxP2pDzd())
                else emptyList()
            }
            val bybitP2pDeferred = async {
                if (SquareMarketCatalog.SOURCE_BYBIT_MARKET in activeSources) listOfNotNull(fetchBybitP2pDzd())
                else emptyList()
            }

            val bankConsensus = bankDeferred.await()
            val bankRates = bankConsensus?.first.orEmpty()
            val bankSource = bankConsensus?.second.orEmpty()
            val bankSourceCount = bankConsensus?.third ?: 0

            val goldConsensus = goldDeferred.await()
            val goldQuote = goldConsensus?.first
            val goldSourceCount = goldConsensus?.second ?: 0

            val boundsForParsing = bankRates.ifEmpty { SquareMarketCatalog.DEFAULT_REFERENCE_BANK_RATES }
            // Query selected direct Algerian Square websites concurrently
            val portalQuotesBySource = fetchAllSquarePortals(boundsForParsing, activeSources)

            val p2pQuotes = binanceP2pDeferred.await() + okxP2pDeferred.await() + bybitP2pDeferred.await()

            val hasBank = bankRates.isNotEmpty()
            val hasParallel = portalQuotesBySource.isNotEmpty() || p2pQuotes.isNotEmpty()
            if (goldQuote == null && !hasBank && !hasParallel) {
                return@coroutineScope Result.failure(
                    IllegalStateException("All selected market websites are currently unreachable")
                )
            }

            val usdBank = bankRates["USD"] ?: boundsForParsing["USD"] ?: 134.0

            // 1. Multi-site consensus for USD Parallel (direct Square sites + Binance/OKX/Bybit P2P)
            val usdSamples = mutableListOf<ParallelPairQuote>()
            portalQuotesBySource.forEach { (siteName, map) ->
                map["USD"]?.let { usdSamples.add(it.copy(source = siteName)) }
            }
            usdSamples.addAll(p2pQuotes)

            val usdConsensus: ParallelPairQuote? = if (usdSamples.isNotEmpty()) {
                val filteredSell = SquareMarketCatalog.filterOutliersAroundMedian(
                    usdSamples.map { it.sellDzd },
                    maxDeviationRatio = 0.10
                )
                val filteredBuy = SquareMarketCatalog.filterOutliersAroundMedian(
                    usdSamples.map { it.buyDzd },
                    maxDeviationRatio = 0.10
                )
                val medSell = SquareMarketCatalog.medianOf(filteredSell)
                val medBuy = minOf(SquareMarketCatalog.medianOf(filteredBuy), medSell)
                val srcNames = usdSamples.map { it.source }.filter { it.isNotBlank() }.distinct()
                ParallelPairQuote(
                    buyDzd = medBuy,
                    sellDzd = medSell,
                    source = srcNames.joinToString(" + ")
                )
            } else null

            val usdParallelRef = usdConsensus?.sellDzd ?: 0.0

            // 2. Per-currency parallel consensus across direct Square sites + USD cross-parity verification
            val allParallelSources = mutableSetOf<String>()
            usdSamples.map { it.source }.filter { it.isNotBlank() }.forEach { allParallelSources.add(it) }
            portalQuotesBySource.forEach { (siteName, _) -> allParallelSources.add(siteName) }

            val currencies = if (hasBank || hasParallel) {
                SquareMarketCatalog.trackedCurrencies.mapNotNull { spec ->
                    val actualBankRate = bankRates[spec.code] ?: 0.0
                    val parityBankRef = if (actualBankRate > 0.0) actualBankRate else (boundsForParsing[spec.code] ?: 0.0)

                    val parallelResult: Pair<ParallelPairQuote, Int> = if (spec.code == "USD") {
                        if (usdConsensus != null) {
                            usdConsensus to usdSamples.map { it.source }.distinct().size
                        } else {
                            ParallelPairQuote(0.0, 0.0) to 0
                        }
                    } else {
                        val crossParity = if (usdBank > 0.0 && parityBankRef > 0.0 && usdConsensus != null) {
                            val ratio = parityBankRef / usdBank
                            ParallelPairQuote(
                                buyDzd = usdConsensus.buyDzd * ratio,
                                sellDzd = usdConsensus.sellDzd * ratio,
                                source = "FX Cross (${usdSamples.size} P2P/Square)"
                            )
                        } else null

                        val directSamples = portalQuotesBySource.mapNotNull { (siteName, map) ->
                            map[spec.code]?.takeIf { q ->
                                // Reject rogue scrape if it deviates >18% from live international cross parity
                                crossParity == null || abs(q.sellDzd - crossParity.sellDzd) / crossParity.sellDzd <= 0.18
                            }?.copy(source = siteName)
                        }

                        when {
                            directSamples.size >= 2 -> {
                                val buys = SquareMarketCatalog.filterOutliersAroundMedian(directSamples.map { it.buyDzd }, 0.10)
                                val sells = SquareMarketCatalog.filterOutliersAroundMedian(directSamples.map { it.sellDzd }, 0.10)
                                val sellMed = SquareMarketCatalog.medianOf(sells)
                                val buyMed = minOf(SquareMarketCatalog.medianOf(buys), sellMed)
                                val srcNames = directSamples.map { it.source }.distinct()
                                ParallelPairQuote(
                                    buyDzd = buyMed,
                                    sellDzd = sellMed,
                                    source = srcNames.joinToString(" + ")
                                ) to srcNames.size
                            }
                            directSamples.size == 1 && crossParity != null -> {
                                val single = directSamples.first()
                                val sellBlend = (single.sellDzd + crossParity.sellDzd) / 2.0
                                val buyBlend = minOf((single.buyDzd + crossParity.buyDzd) / 2.0, sellBlend)
                                ParallelPairQuote(
                                    buyDzd = buyBlend,
                                    sellDzd = sellBlend,
                                    source = "${single.source} + FX Cross"
                                ) to (1 + usdSamples.map { it.source }.distinct().size)
                            }
                            directSamples.size == 1 -> {
                                directSamples.first() to 1
                            }
                            crossParity != null -> {
                                crossParity to usdSamples.map { it.source }.distinct().size
                            }
                            else -> ParallelPairQuote(0.0, 0.0) to 0
                        }
                    }

                    if (actualBankRate <= 0.0 && parallelResult.first.sellDzd <= 0.0) {
                        return@mapNotNull null
                    }

                    CurrencySquareRate(
                        code = spec.code,
                        nameEn = spec.nameEn,
                        nameAr = spec.nameAr,
                        flag = spec.flag,
                        symbol = spec.symbol,
                        bankRateDzd = actualBankRate,
                        parallelBuyDzd = parallelResult.first.buyDzd,
                        parallelSellDzd = parallelResult.first.sellDzd,
                        parallelSource = parallelResult.first.source,
                        sourceCount = parallelResult.second
                    )
                }
            } else {
                emptyList()
            }

            val goldKarats = if (goldQuote != null) {
                SquareMarketCatalog.buildGoldKarats(
                    goldOzUsd = goldQuote.priceOzUsd,
                    usdBankDzd = bankRates["USD"] ?: 0.0,
                    usdParallelDzd = usdParallelRef
                )
            } else {
                emptyList()
            }

            if (goldKarats.isEmpty() && currencies.isEmpty()) {
                return@coroutineScope Result.failure(IllegalStateException("Empty market snapshot"))
            }

            val parallelSourceCount = allParallelSources.size
            val parallelSourceLabel = if (parallelSourceCount > 0) {
                "${parallelSourceCount} sites (${allParallelSources.take(3).joinToString(", ")})"
            } else ""

            Result.success(
                SquareMarketSnapshot(
                    version = SQUARE_SNAPSHOT_VERSION,
                    goldOzUsd = goldQuote?.priceOzUsd ?: 0.0,
                    gold24hChangePercent = goldQuote?.change24hPercent ?: 0.0,
                    goldSource = goldQuote?.source.orEmpty(),
                    bankSource = bankSource,
                    parallelSource = parallelSourceLabel,
                    goldSourceCount = goldSourceCount,
                    bankSourceCount = bankSourceCount,
                    parallelSourceCount = parallelSourceCount,
                    goldKarats = goldKarats,
                    currencies = currencies,
                    updatedAtMillis = now
                )
            )
        }
    }

    private suspend fun fetchGoldSpotMultiSource(
        enabledSourceIds: Set<String>
    ): Pair<GoldSpotQuote, Int>? = coroutineScope {
        val jobs = buildList {
            if (SquareMarketCatalog.SOURCE_GOLDPRICE_ORG in enabledSourceIds) {
                add(async {
                    runCatching {
                        SquareMarketCatalog.parseGoldPriceOrgResponse(
                            getString("https://data-asg.goldprice.org/dbXRates/USD")
                        )
                    }.getOrNull()
                })
            }
            if (SquareMarketCatalog.SOURCE_GOLD_API in enabledSourceIds) {
                add(async {
                    runCatching {
                        SquareMarketCatalog.parseGoldApiResponse(
                            getString("https://api.gold-api.com/price/XAU")
                        )
                    }.getOrNull()
                })
            }
            if (SquareMarketCatalog.SOURCE_YAHOO_FINANCE in enabledSourceIds) {
                add(async {
                    runCatching {
                        SquareMarketCatalog.parseYahooGoldChartResponse(
                            getString("https://query1.finance.yahoo.com/v8/finance/chart/GC=F?interval=1d&range=2d")
                        )
                    }.getOrNull()
                })
            }
            if (SquareMarketCatalog.SOURCE_FAWAZ_CURRENCY in enabledSourceIds) {
                add(async {
                    runCatching {
                        SquareMarketCatalog.parseFawazXauResponse(
                            getString("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/xau.json")
                        )
                    }.getOrNull() ?: runCatching {
                        SquareMarketCatalog.parseFawazXauResponse(
                            getString("https://latest.currency-api.pages.dev/v1/currencies/xau.json")
                        )
                    }.getOrNull()
                })
            }
            if (SquareMarketCatalog.SOURCE_BINANCE_PAXG in enabledSourceIds) {
                add(async {
                    runCatching {
                        SquareMarketCatalog.parseBinancePaxgResponse(
                            getString("https://api.binance.com/api/v3/ticker/24hr?symbol=PAXGUSDT")
                        )
                    }.getOrNull()
                })
            }
            if (SquareMarketCatalog.SOURCE_COINGECKO in enabledSourceIds) {
                add(async {
                    runCatching {
                        SquareMarketCatalog.parseCoinGeckoGoldResponse(
                            getString("https://api.coingecko.com/api/v3/simple/price?ids=pax-gold,tether-gold&vs_currencies=usd&include_24hr_change=true")
                        )
                    }.getOrNull()
                })
            }
            if (SquareMarketCatalog.SOURCE_KRAKEN_GOLD in enabledSourceIds) {
                add(async {
                    runCatching {
                        SquareMarketCatalog.parseKrakenPaxgResponse(
                            getString("https://api.kraken.com/0/public/Ticker?pair=PAXGUSD")
                        )
                    }.getOrNull()
                })
            }
            if (SquareMarketCatalog.SOURCE_BYBIT_MARKET in enabledSourceIds) {
                add(async {
                    runCatching {
                        SquareMarketCatalog.parseBybitPaxgResponse(
                            getString("https://api.bybit.com/v5/market/tickers?category=spot&symbol=PAXGUSDT")
                        )
                    }.getOrNull()
                })
            }
            if (SquareMarketCatalog.SOURCE_OKX_MARKET in enabledSourceIds) {
                add(async {
                    runCatching {
                        SquareMarketCatalog.parseOkxXautResponse(
                            getString("https://www.okx.com/api/v5/market/ticker?instId=XAUT-USDT")
                        )
                    }.getOrNull()
                })
            }
        }
        val quotes = jobs.awaitAll().filterNotNull()
        SquareMarketCatalog.computeGoldConsensus(quotes)
    }

    private suspend fun fetchOfficialBankMultiSource(
        enabledSourceIds: Set<String>
    ): Triple<Map<String, Double>, String, Int>? = coroutineScope {
        val jobs = buildList {
            if (SquareMarketCatalog.SOURCE_OPEN_ER_API in enabledSourceIds) {
                add(async {
                    "Open ER-API" to runCatching {
                        SquareMarketCatalog.parseOpenErRatesResponse(
                            getString("https://open.er-api.com/v6/latest/USD")
                        )
                    }.getOrDefault(emptyMap())
                })
            }
            if (SquareMarketCatalog.SOURCE_FAWAZ_CURRENCY in enabledSourceIds) {
                add(async {
                    "Currency-API" to runCatching {
                        SquareMarketCatalog.parseFawazCurrencyResponse(
                            getString("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json")
                        )
                    }.getOrDefault(emptyMap())
                })
                add(async {
                    "Currency-Pages" to runCatching {
                        SquareMarketCatalog.parseFawazCurrencyResponse(
                            getString("https://latest.currency-api.pages.dev/v1/currencies/usd.json")
                        )
                    }.getOrDefault(emptyMap())
                })
            }
            if (SquareMarketCatalog.SOURCE_FLOATRATES in enabledSourceIds) {
                add(async {
                    "FloatRates" to runCatching {
                        SquareMarketCatalog.parseFloatRatesResponse(
                            getString("https://www.floatrates.com/daily/usd.json")
                        )
                    }.getOrDefault(emptyMap())
                })
            }
            if (SquareMarketCatalog.SOURCE_EXCHANGERATE_API_V4 in enabledSourceIds) {
                add(async {
                    "ExchangeRate-API" to runCatching {
                        SquareMarketCatalog.parseExchangeRateApiV4Response(
                            getString("https://api.exchangerate-api.com/v4/latest/USD")
                        )
                    }.getOrDefault(emptyMap())
                })
            }
            if (SquareMarketCatalog.SOURCE_FXRATES_API in enabledSourceIds) {
                add(async {
                    "FXRatesAPI" to runCatching {
                        SquareMarketCatalog.parseFxRatesApiResponse(
                            getString("https://api.fxratesapi.com/latest?base=USD")
                        )
                    }.getOrDefault(emptyMap())
                })
            }
        }
        val results = jobs.awaitAll()
        SquareMarketCatalog.computeBankRatesConsensus(results)
    }

    private suspend fun fetchAllBinanceP2pDzd(): List<ParallelPairQuote> = coroutineScope {
        fun queryAsset(asset: String, label: String): ParallelPairQuote? {
            fun querySide(tradeType: String): Double? {
                val payload = """{"fiat":"DZD","page":1,"rows":20,"tradeType":"$tradeType","asset":"$asset","countries":[],"proMerchantAds":false,"shieldMerchantAds":false,"publisherType":null,"payTypes":[]}"""
                return runCatching {
                    val body = postJson("https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search", payload)
                    SquareMarketCatalog.parseBinanceP2pMedianDzd(body)
                }.getOrNull()
            }

            // tradeType=BUY returns taker-buy / maker-sell ads (ask / street sell)
            // tradeType=SELL returns taker-sell / maker-buy ads (bid / street buy)
            val sellSide = querySide("BUY")
            val buySide = querySide("SELL")
            return when {
                buySide != null && sellSide != null -> ParallelPairQuote(
                    buyDzd = minOf(buySide, sellSide),
                    sellDzd = maxOf(buySide, sellSide),
                    source = label
                )
                sellSide != null -> ParallelPairQuote(
                    buyDzd = sellSide - 2.0,
                    sellDzd = sellSide,
                    source = label
                )
                buySide != null -> ParallelPairQuote(
                    buyDzd = buySide,
                    sellDzd = buySide + 2.0,
                    source = label
                )
                else -> null
            }
        }

        val usdtDeferred = async { queryAsset("USDT", "Binance P2P USDT") }
        val usdcDeferred = async { queryAsset("USDC", "Binance P2P USDC") }
        listOfNotNull(usdtDeferred.await(), usdcDeferred.await())
    }

    private fun fetchOkxP2pDzd(): ParallelPairQuote? {
        fun querySide(side: String): Double? {
            return runCatching {
                val url = "https://www.okx.com/v3/c2c/tradingOrders/books?quoteCurrency=dzd&baseCurrency=usdt&side=$side&paymentMethod=all&userType=all&showTrade=false&receivingAds=false&noAds=false&limit=20"
                val body = getString(url)
                SquareMarketCatalog.parseOkxP2pMedianDzd(body, side)
            }.getOrNull()
        }
        val sellSide = querySide("sell")
        val buySide = querySide("buy")
        return when {
            buySide != null && sellSide != null -> ParallelPairQuote(
                buyDzd = minOf(buySide, sellSide),
                sellDzd = maxOf(buySide, sellSide),
                source = "OKX P2P USDT"
            )
            sellSide != null -> ParallelPairQuote(
                buyDzd = sellSide - 2.0,
                sellDzd = sellSide,
                source = "OKX P2P USDT"
            )
            buySide != null -> ParallelPairQuote(
                buyDzd = buySide,
                sellDzd = buySide + 2.0,
                source = "OKX P2P USDT"
            )
            else -> null
        }
    }

    private fun fetchBybitP2pDzd(): ParallelPairQuote? {
        fun querySide(sideCode: String): Double? {
            val payload = """{"userId":"","tokenId":"USDT","currencyId":"DZD","payment":[],"side":"$sideCode","size":"20","page":"1","amount":"","authMaker":false,"canTrade":false}"""
            return runCatching {
                val body = postJson("https://api2.bybit.com/fiat/otc/item/online", payload)
                SquareMarketCatalog.parseBybitP2pMedianDzd(body)
            }.getOrNull()
        }
        val sellSide = querySide("1")
        val buySide = querySide("0")
        return when {
            buySide != null && sellSide != null -> ParallelPairQuote(
                buyDzd = minOf(buySide, sellSide),
                sellDzd = maxOf(buySide, sellSide),
                source = "Bybit P2P USDT"
            )
            sellSide != null -> ParallelPairQuote(
                buyDzd = sellSide - 2.0,
                sellDzd = sellSide,
                source = "Bybit P2P USDT"
            )
            buySide != null -> ParallelPairQuote(
                buyDzd = buySide,
                sellDzd = buySide + 2.0,
                source = "Bybit P2P USDT"
            )
            else -> null
        }
    }

    private suspend fun fetchAllSquarePortals(
        bankRates: Map<String, Double>,
        enabledSourceIds: Set<String>
    ): List<Pair<String, Map<String, ParallelPairQuote>>> = coroutineScope {
        val portals = listOf(
            Triple(SquareMarketCatalog.SOURCE_DEVISESQUARE, "https://devisesquare.com/", "DeviseSquare"),
            Triple(SquareMarketCatalog.SOURCE_FOREXALGERIE, "https://www.forexalgerie.com/", "ForexAlgérie"),
            Triple(SquareMarketCatalog.SOURCE_EURODZ, "https://eurodz.com/", "EuroDZ"),
            Triple(SquareMarketCatalog.SOURCE_ALGERIANDINAR, "https://www.algerian-dinar.com/", "AlgerianDinar")
        ).filter { it.first in enabledSourceIds }
        portals.map { (_, url, label) ->
            async {
                val parsed = runCatching {
                    val html = getString(url)
                    SquareMarketCatalog.parseSquareHtmlQuotes(html, bankRates, label)
                }.getOrDefault(emptyMap())
                label to parsed
            }
        }.awaitAll().filter { it.second.isNotEmpty() }
    }
}
