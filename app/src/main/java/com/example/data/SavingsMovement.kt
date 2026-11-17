package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class SavingsMovementType {
    DEPOSIT,
    WITHDRAW,
    SPEND
}

data class SavingsMovementEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: SavingsMovementType,
    val amount: Double,
    val title: String = "",
    val note: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

object SavingsHistoryCodec {
    private const val MAX_ENTRIES = 40

    fun parse(jsonStr: String): List<SavingsMovementEntry> {
        if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val result = ArrayList<SavingsMovementEntry>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val typeStr = obj.optString("type", "DEPOSIT")
                val type = runCatching { SavingsMovementType.valueOf(typeStr) }
                    .getOrDefault(SavingsMovementType.DEPOSIT)
                val amount = obj.optDouble("amount", 0.0)
                if (amount <= 0.0) continue
                result.add(
                    SavingsMovementEntry(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        type = type,
                        amount = amount,
                        title = obj.optString("title", ""),
                        note = obj.optString("note", ""),
                        timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis())
                    )
                )
            }
            result.sortedByDescending { it.timestampMillis }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun encode(entries: List<SavingsMovementEntry>): String {
        val arr = JSONArray()
        for (entry in entries.take(MAX_ENTRIES)) {
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("type", entry.type.name)
            obj.put("amount", entry.amount)
            obj.put("title", entry.title)
            obj.put("note", entry.note)
            obj.put("timestampMillis", entry.timestampMillis)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun prepend(existingJson: String, entry: SavingsMovementEntry): String {
        val current = parse(existingJson)
        return encode(listOf(entry) + current)
    }
}
