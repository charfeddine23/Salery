package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Permanent, append-only archive of every ALG Poste / BaridiMob SMS ever seen by the app.
 * No deletion or purge operations are permitted on this table so that even if messages
 * are removed from the device SMS inbox, the app retains the unbroken history to study
 * formatting patterns and improve balance & tax-detection accuracy.
 */
@Entity(tableName = "archived_alg_post_sms")
data class ArchivedAlgPosteSmsEntity(
    @PrimaryKey
    val id: String,
    val sender: String,
    val rawBody: String,
    val receivedAtMillis: Long,
    val capturedAtMillis: Long = System.currentTimeMillis(),
    val captureSource: String = "INBOX", // "RECEIVER", "INBOX", "MANUAL", "BACKUP"
    val extractedBalance: Double? = null,
    val extractedTxAmount: Double? = null,
    val extractedIsExpense: Boolean? = null,
    val hasExplicitFeeKeyword: Boolean = false,
    val matchedRule: String = "UNRECOGNIZED",
    val studyStatus: String = "NEEDS_STUDY" // "FULL_MATCH", "BALANCE_ONLY", "AMOUNT_ONLY", "NEEDS_STUDY"
)
