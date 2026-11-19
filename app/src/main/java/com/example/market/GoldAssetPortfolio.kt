package com.example.market

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.UUID

@JsonClass(generateAdapter = true)
data class GoldAssetHolding(
    val id: String = UUID.randomUUID().toString(),
    val karat: Int = 18,
    val grams: Double = 0.0,
    val label: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class GoldValuationItem(
    val holding: GoldAssetHolding,
    val karatSpec: GoldKaratPrice?,
    val pricePerGramDzdParallel: Double,
    val pricePerGramDzdBank: Double,
    val pricePerGramUsd: Double,
    val totalValueDzdParallel: Double,
    val totalValueDzdBank: Double,
    val totalValueUsd: Double
)

data class GoldPortfolioSummary(
    val items: List<GoldValuationItem> = emptyList(),
    val totalGrams: Double = 0.0,
    val totalValueDzdParallel: Double = 0.0,
    val totalValueDzdBank: Double = 0.0,
    val totalValueUsd: Double = 0.0,
    val hasLivePrices: Boolean = false
)

object GoldAssetCodec {
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val listType = Types.newParameterizedType(List::class.java, GoldAssetHolding::class.java)
    private val adapter by lazy { moshi.adapter<List<GoldAssetHolding>>(listType) }

    fun parseHoldings(json: String): List<GoldAssetHolding> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            adapter.fromJson(json)
                ?.filter { it.karat > 0 && it.grams > 0.0 && it.grams.isFinite() }
                ?.sortedByDescending { it.updatedAtMillis }
                ?: emptyList()
        }.getOrElse { emptyList() }
    }

    fun encodeHoldings(list: List<GoldAssetHolding>): String {
        return runCatching {
            val cleaned = list
                .filter { it.karat > 0 && it.grams > 0.0 && it.grams.isFinite() }
                .map {
                    it.copy(
                        id = it.id.ifBlank { UUID.randomUUID().toString() },
                        label = it.label.trim()
                    )
                }
            adapter.toJson(cleaned)
        }.getOrElse { "[]" }
    }
}

object GoldValuationEngine {

    fun findKaratPrice(karat: Int, snapshot: SquareMarketSnapshot?): GoldKaratPrice? {
        if (snapshot == null) return null
        return snapshot.goldKarats.firstOrNull { it.karat == karat }
    }

    fun evaluateSingle(
        holding: GoldAssetHolding,
        snapshot: SquareMarketSnapshot?
    ): GoldValuationItem {
        val price = findKaratPrice(holding.karat, snapshot)
        val perGramPar = price?.pricePerGramDzdParallel ?: 0.0
        val perGramBank = price?.pricePerGramDzdBank ?: 0.0
        val perGramUsd = price?.pricePerGramUsd ?: 0.0
        val safeGrams = holding.grams.coerceAtLeast(0.0)
        return GoldValuationItem(
            holding = holding,
            karatSpec = price,
            pricePerGramDzdParallel = perGramPar,
            pricePerGramDzdBank = perGramBank,
            pricePerGramUsd = perGramUsd,
            totalValueDzdParallel = safeGrams * perGramPar,
            totalValueDzdBank = safeGrams * perGramBank,
            totalValueUsd = safeGrams * perGramUsd
        )
    }

    fun evaluatePortfolio(
        holdings: List<GoldAssetHolding>,
        snapshot: SquareMarketSnapshot?
    ): GoldPortfolioSummary {
        val items = holdings
            .filter { it.grams > 0.0 && it.karat > 0 }
            .map { evaluateSingle(it, snapshot) }
        val totalGrams = items.sumOf { it.holding.grams }
        val totalPar = items.sumOf { it.totalValueDzdParallel }
        val totalBank = items.sumOf { it.totalValueDzdBank }
        val totalUsd = items.sumOf { it.totalValueUsd }
        val hasPrices = snapshot != null && snapshot.goldKarats.isNotEmpty()
        return GoldPortfolioSummary(
            items = items,
            totalGrams = totalGrams,
            totalValueDzdParallel = totalPar,
            totalValueDzdBank = totalBank,
            totalValueUsd = totalUsd,
            hasLivePrices = hasPrices
        )
    }

    /**
     * Converts a USD figure (such as Binance Crypto Portfolio USD total) to DZD using Square's
     * live USD parallel sell rate when available. Returns null if no live USD rate is cached.
     */
    fun convertUsdToSquareParallelDzd(
        usdAmount: Double,
        snapshot: SquareMarketSnapshot?
    ): Double? {
        if (usdAmount <= 0.0 || snapshot == null) return null
        val usdRate = snapshot.currencies
            .firstOrNull { it.code.equals("USD", ignoreCase = true) }
            ?.parallelSellDzd
            ?.takeIf { it > 0.0 }
            ?: return null
        return usdAmount * usdRate
    }
}
