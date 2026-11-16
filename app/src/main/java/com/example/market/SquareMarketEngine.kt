package com.example.market

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

const val SQUARE_REFRESH_INTERVAL_MILLIS = 3_600_000L // 1 hour
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
    val pricePerGramDzdBank: Double
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
    val parallelSource: String = ""
)

@JsonClass(generateAdapter = true)
data class SquareMarketSnapshot(
    val goldOzUsd: Double = 0.0,
    val gold24hChangePercent: Double = 0.0,
    val goldSource: String = "",
    val bankSource: String = "",
    val parallelSource: String = "",
    val goldKarats: List<GoldKaratPrice> = emptyList(),
    val currencies: List<CurrencySquareRate> = emptyList(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class GoldSpotQuote(
    val priceOzUsd: Double,
    val change24hPercent: Double,
    val source: String
)

data class ParallelPairQuote(
    val buyDzd: Double,
    val sellDzd: Double
)

object SquareMarketCatalog {
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
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (lastSyncMillis <= 0L) return true
        return (nowMillis - lastSyncMillis) >= SQUARE_REFRESH_INTERVAL_MILLIS
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
        is String -> this.toDoubleOrNull()?.takeIf { !it.isNaN() && it.isFinite() }
        else -> null
    }

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asStringAnyMap(): Map<String, Any?>? = this as? Map<String, Any?>

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asList(): List<Any?>? = this as? List<Any?>

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
        return GoldSpotQuote(priceOzUsd = price, change24hPercent = change, source = "Spot PAXG/XAU")
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
        return GoldSpotQuote(priceOzUsd = price, change24hPercent = change, source = "CoinGecko XAU")
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

    fun parseBinanceP2pMedianDzd(jsonBody: String): Double? {
        val obj = parseJsonMap(jsonBody) ?: return null
        val data = obj["data"].asList() ?: return null
        val prices = mutableListOf<Double>()
        for (row in data) {
            val rowMap = row.asStringAnyMap() ?: continue
            val adv = rowMap["adv"].asStringAnyMap() ?: continue
            val p = adv["price"].asDouble() ?: continue
            if (p in 150.0..450.0) {
                prices.add(p)
            }
        }
        if (prices.isEmpty()) return null
        prices.sort()
        val mid = prices.size / 2
        return if (prices.size % 2 == 1) prices[mid] else (prices[mid - 1] + prices[mid]) / 2.0
    }

    /**
     * Extracts Square Port-Saïd parallel DZD buy/sell numbers from HTML market pages when reachable.
     * Validates strictly against Bank rates so spurious HTML numbers are never accepted.
     */
    fun parseSquareHtmlQuotes(
        html: String,
        bankRates: Map<String, Double>
    ): Map<String, ParallelPairQuote> {
        if (html.isBlank()) return emptyMap()
        val cleanText = html
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")

        val aliases: Map<String, List<String>> = mapOf(
            "EUR" to listOf("EUR", "Euro", "اليورو"),
            "USD" to listOf("USD", "Dollar", "الدولار"),
            "GBP" to listOf("GBP", "Livre", "Pound", "الإسترليني"),
            "CAD" to listOf("CAD", "Canadien", "الكندي"),
            "CHF" to listOf("CHF", "Suisse", "السويسري"),
            "TRY" to listOf("TRY", "Turque", "الليرة التركية"),
            "AED" to listOf("AED", "Émirats", "الدرهم الإماراتي"),
            "SAR" to listOf("SAR", "Saoudien", "الريال السعودي"),
            "TND" to listOf("TND", "Tunisien", "الدينار التونسي"),
            "MAD" to listOf("MAD", "Marocain", "الدرهم المغربي")
        )

        val numRegex = Regex("(\\d{1,3}(?:[.,]\\d{1,2})?)")
        val results = LinkedHashMap<String, ParallelPairQuote>()

        for ((code, tokens) in aliases) {
            val bank = bankRates[code] ?: continue
            val minPlausible = bank * 1.15
            val maxPlausible = bank * 3.20

            for (token in tokens) {
                val idx = cleanText.indexOf(token, ignoreCase = true)
                if (idx == -1) continue
                val windowEnd = (idx + 140).coerceAtMost(cleanText.length)
                val slice = cleanText.substring(idx, windowEnd)
                val candidates = numRegex.findAll(slice)
                    .mapNotNull { it.groupValues[1].replace(',', '.').toDoubleOrNull() }
                    .map { raw ->
                        // Handle 100-unit quotes commonly shown in Algiers (e.g., 24500 for 100 EUR -> 245.0)
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
                    if (abs(high - low) <= bank * 0.35) {
                        results[code] = ParallelPairQuote(buyDzd = low, sellDzd = high)
                        break
                    }
                } else if (candidates.size == 1) {
                    val v = candidates[0]
                    results[code] = ParallelPairQuote(buyDzd = (v - 2.0).coerceAtLeast(minPlausible), sellDzd = v)
                    break
                }
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
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) SaleryPulse/1.0")
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
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) SaleryPulse/1.0")
            .build()
        return client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} for $url")
            resp.body?.string().orEmpty()
        }
    }

    suspend fun fetchLiveSnapshot(): Result<SquareMarketSnapshot> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        coroutineScope {
            val goldDeferred = async { fetchGoldSpotQuote() }
            val bankDeferred = async { fetchOfficialBankRates() }
            val p2pDeferred = async { fetchBinanceP2pUsdDzd() }

            val bankResult = bankDeferred.await()
            val bankPair = bankResult.getOrNull()
            val bankRates = bankPair?.first.orEmpty()
            val bankSource = bankPair?.second.orEmpty()

            val goldResult = goldDeferred.await()
            val goldQuote = goldResult.getOrNull()

            // Fetch direct Square HTML quotes if reachable, and Binance P2P USDT/DZD in parallel fallback
            val directSquareQuotes = if (bankRates.isNotEmpty()) {
                fetchDirectSquareHtmlQuotes(bankRates)
            } else {
                Pair(emptyMap<String, ParallelPairQuote>(), "")
            }

            val p2pQuotes = p2pDeferred.await()

            val hasBank = bankRates.isNotEmpty()
            val hasParallel = directSquareQuotes.first.isNotEmpty() || p2pQuotes != null
            if (goldQuote == null && !hasBank && !hasParallel) {
                return@coroutineScope Result.failure(
                    goldResult.exceptionOrNull()
                        ?: bankResult.exceptionOrNull()
                        ?: IllegalStateException("Market data unavailable")
                )
            }

            val usdBank = bankRates["USD"] ?: 0.0
            val p2pBuy = p2pQuotes?.buyDzd ?: 0.0
            val p2pSell = p2pQuotes?.sellDzd ?: 0.0
            val directUsd = directSquareQuotes.first["USD"]
            val usdParallelRef = when {
                directUsd != null -> directUsd.sellDzd
                p2pSell > 0.0 -> p2pSell
                p2pBuy > 0.0 -> p2pBuy
                else -> 0.0
            }

            val parallelSourceLabel = when {
                directSquareQuotes.first.isNotEmpty() && p2pQuotes != null ->
                    "${directSquareQuotes.second} + Binance P2P"
                directSquareQuotes.first.isNotEmpty() -> directSquareQuotes.second
                p2pQuotes != null -> "Square / Binance P2P DZD"
                else -> ""
            }

            val currencies = if (hasBank) {
                SquareMarketCatalog.trackedCurrencies.mapNotNull { spec ->
                    val bankRate = bankRates[spec.code] ?: return@mapNotNull null
                    val direct = directSquareQuotes.first[spec.code]
                    val parallelPair: ParallelPairQuote = when {
                        direct != null -> direct
                        spec.code == "USD" && p2pQuotes != null -> p2pQuotes
                        usdBank > 0.0 && p2pQuotes != null -> {
                            val crossRatio = bankRate / usdBank
                            ParallelPairQuote(
                                buyDzd = p2pQuotes.buyDzd * crossRatio,
                                sellDzd = p2pQuotes.sellDzd * crossRatio
                            )
                        }
                        else -> ParallelPairQuote(buyDzd = 0.0, sellDzd = 0.0)
                    }
                    CurrencySquareRate(
                        code = spec.code,
                        nameEn = spec.nameEn,
                        nameAr = spec.nameAr,
                        flag = spec.flag,
                        symbol = spec.symbol,
                        bankRateDzd = bankRate,
                        parallelBuyDzd = parallelPair.buyDzd,
                        parallelSellDzd = parallelPair.sellDzd,
                        parallelSource = if (direct != null) directSquareQuotes.second else "Binance P2P DZD"
                    )
                }
            } else {
                emptyList()
            }

            val goldKarats = if (goldQuote != null) {
                SquareMarketCatalog.buildGoldKarats(
                    goldOzUsd = goldQuote.priceOzUsd,
                    usdBankDzd = usdBank,
                    usdParallelDzd = usdParallelRef
                )
            } else {
                emptyList()
            }

            if (goldKarats.isEmpty() && currencies.isEmpty()) {
                return@coroutineScope Result.failure(IllegalStateException("Empty market snapshot"))
            }

            Result.success(
                SquareMarketSnapshot(
                    goldOzUsd = goldQuote?.priceOzUsd ?: 0.0,
                    gold24hChangePercent = goldQuote?.change24hPercent ?: 0.0,
                    goldSource = goldQuote?.source.orEmpty(),
                    bankSource = bankSource,
                    parallelSource = parallelSourceLabel,
                    goldKarats = goldKarats,
                    currencies = currencies,
                    updatedAtMillis = now
                )
            )
        }
    }

    private fun fetchGoldSpotQuote(): Result<GoldSpotQuote> {
        val paxg = runCatching {
            SquareMarketCatalog.parseBinancePaxgResponse(
                getString("https://api.binance.com/api/v3/ticker/24hr?symbol=PAXGUSDT")
            )
        }.getOrNull()

        val goldApi = runCatching {
            SquareMarketCatalog.parseGoldApiResponse(
                getString("https://api.gold-api.com/price/XAU")
            )
        }.getOrNull()

        if (goldApi != null) {
            return Result.success(
                if (goldApi.change24hPercent == 0.0 && paxg != null) {
                    goldApi.copy(change24hPercent = paxg.change24hPercent)
                } else {
                    goldApi
                }
            )
        }
        if (paxg != null) return Result.success(paxg)

        val goldPriceOrg = runCatching {
            SquareMarketCatalog.parseGoldPriceOrgResponse(
                getString("https://data-asg.goldprice.org/dbXRates/USD")
            )
        }.getOrNull()
        if (goldPriceOrg != null) return Result.success(goldPriceOrg)

        val cg = runCatching {
            SquareMarketCatalog.parseCoinGeckoGoldResponse(
                getString("https://api.coingecko.com/api/v3/simple/price?ids=pax-gold,tether-gold&vs_currencies=usd&include_24hr_change=true")
            )
        }.getOrNull()
        if (cg != null) return Result.success(cg)

        return Result.failure(IllegalStateException("Gold spot price unavailable"))
    }

    private fun fetchOfficialBankRates(): Result<Pair<Map<String, Double>, String>> {
        val openEr = runCatching {
            SquareMarketCatalog.parseOpenErRatesResponse(
                getString("https://open.er-api.com/v6/latest/USD")
            )
        }.getOrNull()
        if (!openEr.isNullOrEmpty()) {
            return Result.success(openEr to "Open ER-API (Bank DZD)")
        }

        val fawaz = runCatching {
            SquareMarketCatalog.parseFawazCurrencyResponse(
                getString("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json")
            )
        }.getOrNull()
        if (!fawaz.isNullOrEmpty()) {
            return Result.success(fawaz to "Exchange-API (Bank DZD)")
        }

        return Result.failure(IllegalStateException("Official Bank DZD rates unavailable"))
    }

    private fun fetchBinanceP2pUsdDzd(): ParallelPairQuote? {
        fun querySide(tradeType: String): Double? {
            val payload = """{"fiat":"DZD","page":1,"rows":10,"tradeType":"$tradeType","asset":"USDT","countries":[],"proMerchantAds":false,"shieldMerchantAds":false,"publisherType":null,"payTypes":[]}"""
            return runCatching {
                val body = postJson("https://p2p.binance.com/bapi/c2c/v2/friendly/c2c/adv/search", payload)
                SquareMarketCatalog.parseBinanceP2pMedianDzd(body)
            }.getOrNull()
        }

        // In Binance P2P, tradeType=BUY means user buys USDT (ask/sell rate on street),
        // tradeType=SELL means user sells USDT (bid/buy rate on street).
        val streetSell = querySide("BUY")
        val streetBuy = querySide("SELL")
        return when {
            streetBuy != null && streetSell != null ->
                ParallelPairQuote(
                    buyDzd = minOf(streetBuy, streetSell),
                    sellDzd = maxOf(streetBuy, streetSell)
                )
            streetSell != null -> ParallelPairQuote(buyDzd = streetSell - 2.0, sellDzd = streetSell)
            streetBuy != null -> ParallelPairQuote(buyDzd = streetBuy, sellDzd = streetBuy + 2.0)
            else -> null
        }
    }

    private fun fetchDirectSquareHtmlQuotes(
        bankRates: Map<String, Double>
    ): Pair<Map<String, ParallelPairQuote>, String> {
        val portals = listOf(
            "https://devisesquare.com/" to "Square Port-Saïd",
            "https://www.forexalgerie.com/" to "Forex Algérie Square"
        )
        for ((url, label) in portals) {
            val parsed = runCatching {
                val html = getString(url)
                SquareMarketCatalog.parseSquareHtmlQuotes(html, bankRates)
            }.getOrNull()
            if (!parsed.isNullOrEmpty()) {
                return parsed to label
            }
        }
        return emptyMap<String, ParallelPairQuote>() to ""
    }
}
