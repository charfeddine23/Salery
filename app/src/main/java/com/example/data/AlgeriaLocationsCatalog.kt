package com.example.data

import android.content.Context
import org.json.JSONArray
import java.util.Locale

data class AlgerianCommune(
    val nameAr: String,
    val nameEn: String
) {
    fun displayName(isArabic: Boolean): String = if (isArabic) nameAr else nameEn
}

data class AlgerianWilaya(
    val code: String,
    val nameAr: String,
    val nameEn: String,
    val communes: List<AlgerianCommune>
) {
    fun displayName(isArabic: Boolean): String =
        if (isArabic) "$code - $nameAr" else "$code - $nameEn"
}

data class AlgeriaCommuneMatch(
    val wilaya: AlgerianWilaya,
    val commune: AlgerianCommune
)

object AlgeriaLocationsCatalog {
    private const val ASSET_FILE_NAME = "algeria_locations.json"

    @Volatile
    private var cachedWilayas: List<AlgerianWilaya>? = null

    fun loadFromAssets(context: Context): List<AlgerianWilaya> {
        cachedWilayas?.let { return it }
        synchronized(this) {
            cachedWilayas?.let { return it }
            val jsonText = context.applicationContext.assets
                .open(ASSET_FILE_NAME)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            val parsed = parseJson(jsonText)
            cachedWilayas = parsed
            return parsed
        }
    }

    fun parseJson(jsonText: String): List<AlgerianWilaya> {
        if (jsonText.isBlank()) return emptyList()
        val array = JSONArray(jsonText)
        val result = ArrayList<AlgerianWilaya>(array.length())
        for (i in 0 until array.length()) {
            val wObj = array.getJSONObject(i)
            val code = wObj.optString("code", "").trim()
            val nameAr = wObj.optString("ar", "").trim()
            val nameEn = wObj.optString("en", "").trim()
            val cArray = wObj.optJSONArray("communes") ?: JSONArray()
            val communes = ArrayList<AlgerianCommune>(cArray.length())
            for (j in 0 until cArray.length()) {
                val cObj = cArray.getJSONObject(j)
                val cAr = cObj.optString("ar", "").trim()
                val cEn = cObj.optString("en", "").trim()
                if (cAr.isNotEmpty() || cEn.isNotEmpty()) {
                    communes.add(
                        AlgerianCommune(
                            nameAr = cAr.ifEmpty { cEn },
                            nameEn = cEn.ifEmpty { cAr }
                        )
                    )
                }
            }
            if (code.isNotEmpty() && (nameAr.isNotEmpty() || nameEn.isNotEmpty())) {
                result.add(
                    AlgerianWilaya(
                        code = code,
                        nameAr = nameAr.ifEmpty { nameEn },
                        nameEn = nameEn.ifEmpty { nameAr },
                        communes = communes
                    )
                )
            }
        }
        return result
    }

    fun filterWilayas(wilayas: List<AlgerianWilaya>, query: String): List<AlgerianWilaya> {
        val norm = normalizeQuery(query)
        if (norm.isEmpty()) return wilayas
        return wilayas.filter { w ->
            w.code.contains(norm) ||
                w.code.trimStart('0').contains(norm.trimStart('0').ifEmpty { norm }) ||
                normalizeQuery(w.nameAr).contains(norm) ||
                normalizeQuery(w.nameEn).contains(norm)
        }
    }

    fun filterCommunes(communes: List<AlgerianCommune>, query: String): List<AlgerianCommune> {
        val norm = normalizeQuery(query)
        if (norm.isEmpty()) return communes
        return communes.filter { c ->
            normalizeQuery(c.nameAr).contains(norm) ||
                normalizeQuery(c.nameEn).contains(norm)
        }
    }

    fun searchAllCommunes(
        wilayas: List<AlgerianWilaya>,
        query: String
    ): List<AlgeriaCommuneMatch> {
        val norm = normalizeQuery(query)
        if (norm.isEmpty()) {
            return wilayas.flatMap { w ->
                w.communes.map { c -> AlgeriaCommuneMatch(w, c) }
            }
        }
        return wilayas.flatMap { w ->
            w.communes.mapNotNull { c ->
                val matches = normalizeQuery(c.nameAr).contains(norm) ||
                    normalizeQuery(c.nameEn).contains(norm) ||
                    normalizeQuery(w.nameAr).contains(norm) ||
                    normalizeQuery(w.nameEn).contains(norm) ||
                    w.code == norm
                if (matches) AlgeriaCommuneMatch(w, c) else null
            }
        }
    }

    internal fun normalizeQuery(input: String): String {
        if (input.isBlank()) return ""
        val sb = StringBuilder(input.length)
        for (ch in input.trim().lowercase(Locale.ROOT)) {
            when (ch) {
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                'ة' -> sb.append('ه')
                'ى' -> sb.append('ي')
                'ؤ' -> sb.append('و')
                'ئ' -> sb.append('ي')
                'é', 'è', 'ê', 'ë' -> sb.append('e')
                'à', 'â', 'ä' -> sb.append('a')
                'î', 'ï' -> sb.append('i')
                'ô', 'ö' -> sb.append('o')
                'ù', 'û', 'ü' -> sb.append('u')
                'ç' -> sb.append('c')
                // Strip Arabic harakat / diacritics & apostrophe variance
                in '\u064B'..'\u065F', '\u0670', '\u0640' -> Unit
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
