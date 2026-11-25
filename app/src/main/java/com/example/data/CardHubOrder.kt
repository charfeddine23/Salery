package com.example.data

enum class CardHubSlot(val id: String) {
    CLASSIC("CLASSIC"),
    VISA("VISA"),
    THIRD("THIRD");

    companion object {
        fun fromId(raw: String?): CardHubSlot? {
            val normalized = raw?.trim()?.uppercase().orEmpty()
            return entries.firstOrNull { it.id == normalized }
        }
    }
}

object CardHubOrder {
    val DEFAULT_ORDER: List<CardHubSlot> = listOf(
        CardHubSlot.CLASSIC,
        CardHubSlot.VISA,
        CardHubSlot.THIRD
    )

    const val DEFAULT_JSON: String = "[\"CLASSIC\",\"VISA\",\"THIRD\"]"

    fun parse(raw: String?): List<CardHubSlot> {
        if (raw.isNullOrBlank()) return DEFAULT_ORDER
        val tokens = raw
            .removePrefix("[")
            .removeSuffix("]")
            .split(',')
            .map { token -> token.trim().trim('"', '\'') }
            .filter { it.isNotEmpty() }

        val result = ArrayList<CardHubSlot>(CardHubSlot.entries.size)
        for (token in tokens) {
            val slot = CardHubSlot.fromId(token)
            if (slot != null && slot !in result) {
                result.add(slot)
            }
        }
        for (fallback in DEFAULT_ORDER) {
            if (fallback !in result) {
                result.add(fallback)
            }
        }
        return result
    }

    fun encode(order: List<CardHubSlot>): String {
        val normalized = normalize(order)
        return normalized.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"${it.id}\"" }
    }

    fun normalize(order: List<CardHubSlot>): List<CardHubSlot> {
        val result = ArrayList<CardHubSlot>(CardHubSlot.entries.size)
        for (slot in order) {
            if (slot !in result) {
                result.add(slot)
            }
        }
        for (fallback in DEFAULT_ORDER) {
            if (fallback !in result) {
                result.add(fallback)
            }
        }
        return result
    }

    fun move(order: List<CardHubSlot>, fromIndex: Int, toIndex: Int): List<CardHubSlot> {
        val current = normalize(order)
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) {
            return current
        }
        val mutable = current.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable
    }

    fun moveUp(order: List<CardHubSlot>, slot: CardHubSlot): List<CardHubSlot> {
        val current = normalize(order)
        val index = current.indexOf(slot)
        return if (index > 0) move(current, index, index - 1) else current
    }

    fun moveDown(order: List<CardHubSlot>, slot: CardHubSlot): List<CardHubSlot> {
        val current = normalize(order)
        val index = current.indexOf(slot)
        return if (index in 0 until current.lastIndex) move(current, index, index + 1) else current
    }
}
