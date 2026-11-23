package com.example.crypto

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit

const val CRYPTO_PRICE_REFRESH_INTERVAL_MILLIS = 3_600_000L // 1 hour

data class CryptoAssetSpec(
    val symbol: String,
    val nameEn: String,
    val nameAr: String,
    val binancePair: String,
    val coingeckoId: String,
    val accentHex: Long,
    val isStablecoin: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CryptoHoldingEntry(
    val symbol: String,
    val quantity: Double,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val id: String = "",
    val note: String = ""
)

@JsonClass(generateAdapter = true)
data class CryptoMarketPriceEntry(
    val symbol: String,
    val priceUsd: Double,
    val change24hPercent: Double = 0.0,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val source: String = "Binance"
)

object BinanceCryptoCatalog {
    // Major cryptocurrencies with XRP explicitly highlighted first
    val majorAssets: List<CryptoAssetSpec> = listOf(
        CryptoAssetSpec(
            symbol = "XRP",
            nameEn = "XRP (Ripple)",
            nameAr = "ريبل",
            binancePair = "XRPUSDT",
            coingeckoId = "ripple",
            accentHex = 0xFF00AAE4
        ),
        CryptoAssetSpec(
            symbol = "BTC",
            nameEn = "Bitcoin",
            nameAr = "بيتكوين",
            binancePair = "BTCUSDT",
            coingeckoId = "bitcoin",
            accentHex = 0xFFF7931A
        ),
        CryptoAssetSpec(
            symbol = "ETH",
            nameEn = "Ethereum",
            nameAr = "إيثيريوم",
            binancePair = "ETHUSDT",
            coingeckoId = "ethereum",
            accentHex = 0xFF627EEA
        ),
        CryptoAssetSpec(
            symbol = "BNB",
            nameEn = "BNB",
            nameAr = "عملة بينانس",
            binancePair = "BNBUSDT",
            coingeckoId = "binancecoin",
            accentHex = 0xFFF0B90B
        ),
        CryptoAssetSpec(
            symbol = "SOL",
            nameEn = "Solana",
            nameAr = "سولانا",
            binancePair = "SOLUSDT",
            coingeckoId = "solana",
            accentHex = 0xFF14F195
        ),
        CryptoAssetSpec(
            symbol = "USDT",
            nameEn = "Tether USD",
            nameAr = "تيثر الدولارية",
            binancePair = "USDTUSDT",
            coingeckoId = "tether",
            accentHex = 0xFF26A17B,
            isStablecoin = true
        ),
        CryptoAssetSpec(
            symbol = "TON",
            nameEn = "Toncoin",
            nameAr = "تون كوين",
            binancePair = "TONUSDT",
            coingeckoId = "the-open-network",
            accentHex = 0xFF0098EA
        ),
        CryptoAssetSpec(
            symbol = "DOGE",
            nameEn = "Dogecoin",
            nameAr = "دوجكوين",
            binancePair = "DOGEUSDT",
            coingeckoId = "dogecoin",
            accentHex = 0xFFC2A633
        ),
        CryptoAssetSpec(
            symbol = "ADA",
            nameEn = "Cardano",
            nameAr = "كاردانو",
            binancePair = "ADAUSDT",
            coingeckoId = "cardano",
            accentHex = 0xFF3B82F6
        ),
        CryptoAssetSpec(
            symbol = "TRX",
            nameEn = "TRON",
            nameAr = "ترون",
            binancePair = "TRXUSDT",
            coingeckoId = "tron",
            accentHex = 0xFFEF0027
        ),
        CryptoAssetSpec(
            symbol = "LTC",
            nameEn = "Litecoin",
            nameAr = "لايتكوين",
            binancePair = "LTCUSDT",
            coingeckoId = "litecoin",
            accentHex = 0xFF94A3B8
        ),
        CryptoAssetSpec(
            symbol = "AVAX",
            nameEn = "Avalanche",
            nameAr = "أفالانش",
            binancePair = "AVAXUSDT",
            coingeckoId = "avalanche-2",
            accentHex = 0xFFE84142
        ),
        CryptoAssetSpec(
            symbol = "LINK",
            nameEn = "Chainlink",
            nameAr = "تشين لينك",
            binancePair = "LINKUSDT",
            coingeckoId = "chainlink",
            accentHex = 0xFF2A5ADA
        ),
        CryptoAssetSpec(
            symbol = "SUI",
            nameEn = "Sui",
            nameAr = "سوي",
            binancePair = "SUIUSDT",
            coingeckoId = "sui",
            accentHex = 0xFF4DA2FF
        ),
        CryptoAssetSpec(
            symbol = "PEPE",
            nameEn = "Pepe",
            nameAr = "بيبي",
            binancePair = "PEPEUSDT",
            coingeckoId = "pepe",
            accentHex = 0xFF4C9540
        )
    )

    private val bySymbol = majorAssets.associateBy { it.symbol.uppercase(Locale.US) }
    private val byPair = majorAssets.associateBy { it.binancePair.uppercase(Locale.US) }

    fun findBySymbol(symbol: String): CryptoAssetSpec? =
        bySymbol[symbol.trim().uppercase(Locale.US)]

    fun shouldRefreshMarketPrices(
        lastSyncMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (lastSyncMillis <= 0L) return true
        return (nowMillis - lastSyncMillis) >= CRYPTO_PRICE_REFRESH_INTERVAL_MILLIS
    }

    fun calculatePortfolioTotalUsd(
        holdings: List<CryptoHoldingEntry>,
        prices: Map<String, CryptoMarketPriceEntry>
    ): Double {
        var total = 0.0
        for (h in holdings) {
            if (h.quantity <= 0.0) continue
            val sym = h.symbol.uppercase(Locale.US)
            val unitPrice = when {
                sym == "USDT" -> prices[sym]?.priceUsd ?: 1.0
                else -> prices[sym]?.priceUsd ?: 0.0
            }
            total += h.quantity * unitPrice
        }
        return total
    }

    private data class BinanceTickerRaw(
        val symbol: String = "",
        val lastPrice: String = "",
        val priceChangePercent: String = "0"
    )

    private val jsonMoshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val binanceTickerListAdapter by lazy {
        val type = Types.newParameterizedType(List::class.java, BinanceTickerRaw::class.java)
        jsonMoshi.adapter<List<BinanceTickerRaw>>(type)
    }

    private val coinGeckoMapAdapter by lazy {
        val innerMapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val outerMapType = Types.newParameterizedType(Map::class.java, String::class.java, innerMapType)
        jsonMoshi.adapter<Map<String, Map<String, Any>>>(outerMapType)
    }

    private val symbolArrayAdapter by lazy {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        jsonMoshi.adapter<List<String>>(type)
    }

    fun encodeBinanceSymbolsParam(pairs: List<String>): String =
        symbolArrayAdapter.toJson(pairs)

    fun parseBinance24hrResponse(
        jsonBody: String,
        timestampMillis: Long = System.currentTimeMillis()
    ): Map<String, CryptoMarketPriceEntry> {
        val result = LinkedHashMap<String, CryptoMarketPriceEntry>()
        val list = binanceTickerListAdapter.fromJson(jsonBody).orEmpty()
        for (item in list) {
            val pair = item.symbol.uppercase(Locale.US)
            val spec = byPair[pair] ?: continue
            val lastPrice = item.lastPrice.toDoubleOrNull() ?: continue
            if (lastPrice <= 0.0) continue
            val changePct = item.priceChangePercent.toDoubleOrNull() ?: 0.0
            result[spec.symbol] = CryptoMarketPriceEntry(
                symbol = spec.symbol,
                priceUsd = lastPrice,
                change24hPercent = changePct,
                updatedAtMillis = timestampMillis,
                source = "Binance"
            )
        }
        if (result.isNotEmpty()) {
            result["USDT"] = CryptoMarketPriceEntry(
                symbol = "USDT",
                priceUsd = 1.0,
                change24hPercent = 0.0,
                updatedAtMillis = timestampMillis,
                source = "Binance"
            )
        }
        return result
    }

    fun parseCoinGeckoResponse(
        jsonBody: String,
        timestampMillis: Long = System.currentTimeMillis()
    ): Map<String, CryptoMarketPriceEntry> {
        val result = LinkedHashMap<String, CryptoMarketPriceEntry>()
        val root = coinGeckoMapAdapter.fromJson(jsonBody).orEmpty()
        for (spec in majorAssets) {
            val coinObj = root[spec.coingeckoId] ?: continue
            val usd = (coinObj["usd"] as? Number)?.toDouble() ?: continue
            if (usd <= 0.0 || usd.isNaN()) continue
            val change = ((coinObj["usd_24h_change"] as? Number)?.toDouble() ?: 0.0).let {
                if (it.isNaN()) 0.0 else it
            }
            result[spec.symbol] = CryptoMarketPriceEntry(
                symbol = spec.symbol,
                priceUsd = usd,
                change24hPercent = change,
                updatedAtMillis = timestampMillis,
                source = "CoinGecko"
            )
        }
        return result
    }
}

object BinanceCryptoCodec {
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val holdingsAdapter by lazy {
        val type = Types.newParameterizedType(List::class.java, CryptoHoldingEntry::class.java)
        moshi.adapter<List<CryptoHoldingEntry>>(type)
    }

    private val pricesAdapter by lazy {
        val type = Types.newParameterizedType(List::class.java, CryptoMarketPriceEntry::class.java)
        moshi.adapter<List<CryptoMarketPriceEntry>>(type)
    }

    fun parseHoldings(json: String): List<CryptoHoldingEntry> {
        if (json.isBlank()) return emptyList()
        return try {
            val seenIds = HashSet<String>()
            holdingsAdapter.fromJson(json)
                ?.filter { it.symbol.isNotBlank() && it.quantity > 0.0 }
                ?.mapIndexed { index, entry ->
                    val cleanSymbol = entry.symbol.trim().uppercase(Locale.US)
                    val baseId = entry.id.trim().ifBlank {
                        "legacy_${cleanSymbol}_${entry.updatedAtMillis}_$index"
                    }
                    val uniqueId = if (seenIds.add(baseId)) baseId else "${baseId}_$index"
                    entry.copy(
                        id = uniqueId,
                        symbol = cleanSymbol,
                        note = entry.note.trim()
                    )
                }
                ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun encodeHoldings(list: List<CryptoHoldingEntry>): String {
        return try {
            val seenIds = HashSet<String>()
            val cleaned = list
                .filter { it.symbol.isNotBlank() && it.quantity > 0.0 }
                .mapIndexed { index, item ->
                    val cleanSymbol = item.symbol.trim().uppercase(Locale.US)
                    val baseId = item.id.trim().ifBlank {
                        java.util.UUID.randomUUID().toString()
                    }
                    val uniqueId = if (seenIds.add(baseId)) baseId else "${baseId}_$index"
                    item.copy(
                        id = uniqueId,
                        symbol = cleanSymbol,
                        note = item.note.trim()
                    )
                }
            holdingsAdapter.toJson(cleaned)
        } catch (_: Throwable) {
            "[]"
        }
    }

    fun parsePrices(json: String): Map<String, CryptoMarketPriceEntry> {
        if (json.isBlank()) return emptyMap()
        return try {
            pricesAdapter.fromJson(json)
                ?.filter { it.symbol.isNotBlank() && it.priceUsd > 0.0 }
                ?.associateBy { it.symbol.uppercase(Locale.US) }
                ?: emptyMap()
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    fun encodePrices(map: Map<String, CryptoMarketPriceEntry>): String {
        return try {
            pricesAdapter.toJson(map.values.toList())
        } catch (_: Throwable) {
            "[]"
        }
    }
}

object BinanceMarketPriceFetcher {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    suspend fun fetchLivePrices(): Result<Map<String, CryptoMarketPriceEntry>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1. Primary: Official Binance public market 24hr ticker endpoint (no API key required)
        val binanceResult = runCatching {
            val symbolsJson = BinanceCryptoCatalog.encodeBinanceSymbolsParam(
                BinanceCryptoCatalog.majorAssets
                    .filter { !it.isStablecoin }
                    .map { it.binancePair }
            )

            val url = "https://api.binance.com/api/v3/ticker/24hr".toHttpUrl()
                .newBuilder()
                .addQueryParameter("symbols", symbolsJson)
                .build()

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Binance HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                val parsed = BinanceCryptoCatalog.parseBinance24hrResponse(body, now)
                if (parsed.isEmpty()) error("Empty Binance ticker payload")
                parsed
            }
        }
        if (binanceResult.isSuccess) {
            return@withContext binanceResult
        }

        // 2. Fallback: CoinGecko public simple price endpoint (no API key required)
        val cgResult = runCatching {
            val ids = BinanceCryptoCatalog.majorAssets.joinToString(",") { it.coingeckoId }
            val url = "https://api.coingecko.com/api/v3/simple/price".toHttpUrl()
                .newBuilder()
                .addQueryParameter("ids", ids)
                .addQueryParameter("vs_currencies", "usd")
                .addQueryParameter("include_24hr_change", "true")
                .build()

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("CoinGecko HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                val parsed = BinanceCryptoCatalog.parseCoinGeckoResponse(body, now)
                if (parsed.isEmpty()) error("Empty CoinGecko payload")
                parsed
            }
        }
        if (cgResult.isSuccess) {
            return@withContext cgResult
        }

        Result.failure(
            cgResult.exceptionOrNull()
                ?: binanceResult.exceptionOrNull()
                ?: IllegalStateException("Market price fetch failed")
        )
    }
}
