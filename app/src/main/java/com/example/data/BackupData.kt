package com.example.data

import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val exportTimestamp: Long,
    val appVersion: String,
    val developer: String,
    val transactions: List<TransactionEntity>,
    val debts: List<DebtEntity>,
    val salaryAmount: Double,
    val salaryPayday: Int,
    val salaryEmployer: String,
    val classicCardHolderName: String = "CHARFEDDINE KA",
    val classicCardNumber: String = "",
    val classicCardExpiry: String = "11/29",
    val classicCardCvc: String = "",
    val archivedAlgPosteSms: List<ArchivedAlgPosteSmsEntity> = emptyList()
) {
    fun toJsonString(includeSensitiveCardFields: Boolean = false): String {
        val root = JSONObject()
        root.put("appName", "Vaultix")
        root.put("appVersion", appVersion)
        root.put("developer", developer)
        root.put("exportTimestamp", exportTimestamp)

        // Salary Settings
        val salaryObj = JSONObject()
        salaryObj.put("amount", salaryAmount)
        salaryObj.put("payday", salaryPayday)
        salaryObj.put("employer", salaryEmployer)
        root.put("salarySettings", salaryObj)

        // Classic Card Details (sensitive PAN/CVC are excluded from plaintext JSON backups)
        val cardObj = JSONObject()
        cardObj.put("holderName", classicCardHolderName)
        cardObj.put("cardNumber", if (includeSensitiveCardFields) classicCardNumber else "")
        cardObj.put("expiryDate", classicCardExpiry)
        cardObj.put("cvc", if (includeSensitiveCardFields) classicCardCvc else "")
        root.put("classicCardSettings", cardObj)

        // Transactions
        val transArray = JSONArray()
        transactions.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("type", t.type.name)
            obj.put("amount", t.amount)
            obj.put("category", t.category)
            obj.put("title", t.title)
            obj.put("note", t.note)
            obj.put("dateMillis", t.dateMillis)
            obj.put("isSalaryCredit", t.isSalaryCredit)
            obj.put("isPaidFromSavings", t.isPaidFromSavings)
            transArray.put(obj)
        }
        root.put("transactions", transArray)

        // Debts
        val debtsArray = JSONArray()
        debts.forEach { d ->
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("type", d.type.name)
            obj.put("personName", d.personName)
            obj.put("amount", d.amount)
            obj.put("note", d.note)
            obj.put("dueDateMillis", d.dueDateMillis)
            obj.put("autoPayOnDueDate", d.autoPayOnDueDate)
            obj.put("isPaid", d.isPaid)
            if (d.paidDateMillis != null) obj.put("paidDateMillis", d.paidDateMillis)
            obj.put("createdDateMillis", d.createdDateMillis)
            obj.put("isRecurringMonthlyInstallment", d.isRecurringMonthlyInstallment)
            obj.put("monthlyInstallmentAmount", d.monthlyInstallmentAmount)
            obj.put("installmentDayOfMonth", d.installmentDayOfMonth)
            obj.put("remainingAmount", d.remainingAmount)
            obj.put("lastInstallmentCreditedMonth", d.lastInstallmentCreditedMonth)
            debtsArray.put(obj)
        }
        root.put("debts", debtsArray)

        // Permanent Non-Deletable ALG Poste SMS Archive
        val smsArchiveArray = JSONArray()
        archivedAlgPosteSms.forEach { sms ->
            val obj = JSONObject()
            obj.put("id", sms.id)
            obj.put("sender", sms.sender)
            obj.put("rawBody", sms.rawBody)
            obj.put("receivedAtMillis", sms.receivedAtMillis)
            obj.put("capturedAtMillis", sms.capturedAtMillis)
            obj.put("captureSource", sms.captureSource)
            if (sms.extractedBalance != null) obj.put("extractedBalance", sms.extractedBalance)
            if (sms.extractedTxAmount != null) obj.put("extractedTxAmount", sms.extractedTxAmount)
            if (sms.extractedIsExpense != null) obj.put("extractedIsExpense", sms.extractedIsExpense)
            obj.put("hasExplicitFeeKeyword", sms.hasExplicitFeeKeyword)
            obj.put("matchedRule", sms.matchedRule)
            obj.put("studyStatus", sms.studyStatus)
            smsArchiveArray.put(obj)
        }
        root.put("archivedAlgPosteSms", smsArchiveArray)

        return root.toString(2)
    }

    companion object {
        private const val MAX_BACKUP_JSON_CHARS = 10 * 1024 * 1024 // 10 MB safety cap

        fun fromJsonString(jsonStr: String): BackupPayload? {
            if (jsonStr.isBlank() || jsonStr.length > MAX_BACKUP_JSON_CHARS) return null
            return runCatching {
                val root = JSONObject(jsonStr)
                val appVersion = root.optString("appVersion", "1.0.0")
                val developer = root.optString("developer", "Kahlessenane Charfeddine")
                val exportTimestamp = root.optLong("exportTimestamp", System.currentTimeMillis())

                val salaryObj = root.optJSONObject("salarySettings")
                val rawSalary = salaryObj?.optDouble("amount", 85000.0) ?: 85000.0
                val salaryAmount = if (rawSalary.isFinite() && rawSalary >= 0.0) rawSalary else 85000.0
                val salaryPayday = (salaryObj?.optInt("payday", 25) ?: 25).coerceIn(1, 31)
                val salaryEmployer = salaryObj?.optString("employer", "Enterprise Algiers") ?: "Enterprise Algiers"

                val cardObj = root.optJSONObject("classicCardSettings")
                val classicCardHolderName = cardObj?.optString("holderName", "CHARFEDDINE KA") ?: "CHARFEDDINE KA"
                val classicCardNumber = cardObj?.optString("cardNumber", "") ?: ""
                val classicCardExpiry = cardObj?.optString("expiryDate", "11/29") ?: "11/29"
                val classicCardCvc = cardObj?.optString("cvc", "") ?: ""

                val transList = mutableListOf<TransactionEntity>()
                val transArray = root.optJSONArray("transactions")
                if (transArray != null) {
                    for (i in 0 until transArray.length()) {
                        val obj = transArray.getJSONObject(i)
                        val txAmount = obj.getDouble("amount")
                        if (!txAmount.isFinite() || txAmount < 0.0) continue
                        transList.add(
                            TransactionEntity(
                                id = 0, // Auto-generate new IDs on restore
                                type = TransactionType.valueOf(obj.getString("type")),
                                amount = txAmount,
                                category = obj.getString("category"),
                                title = obj.getString("title"),
                                note = obj.optString("note", ""),
                                dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                                isSalaryCredit = obj.optBoolean("isSalaryCredit", false),
                                isPaidFromSavings = obj.optBoolean("isPaidFromSavings", false)
                            )
                        )
                    }
                }

                val debtsList = mutableListOf<DebtEntity>()
                val debtsArray = root.optJSONArray("debts")
                if (debtsArray != null) {
                    for (i in 0 until debtsArray.length()) {
                        val obj = debtsArray.getJSONObject(i)
                        val debtAmount = obj.getDouble("amount")
                        if (!debtAmount.isFinite() || debtAmount < 0.0) continue
                        val rawRemaining = obj.optDouble("remainingAmount", debtAmount)
                        val safeRemaining = if (rawRemaining.isFinite() && rawRemaining >= 0.0) rawRemaining else debtAmount
                        val rawMonthly = obj.optDouble("monthlyInstallmentAmount", 0.0)
                        val safeMonthly = if (rawMonthly.isFinite() && rawMonthly >= 0.0) rawMonthly else 0.0
                        debtsList.add(
                            DebtEntity(
                                id = 0,
                                type = DebtType.valueOf(obj.getString("type")),
                                personName = obj.getString("personName"),
                                amount = debtAmount,
                                note = obj.optString("note", ""),
                                dueDateMillis = obj.getLong("dueDateMillis"),
                                autoPayOnDueDate = obj.optBoolean("autoPayOnDueDate", true),
                                isPaid = obj.optBoolean("isPaid", false),
                                paidDateMillis = if (obj.has("paidDateMillis")) obj.getLong("paidDateMillis") else null,
                                createdDateMillis = obj.optLong("createdDateMillis", System.currentTimeMillis()),
                                isRecurringMonthlyInstallment = obj.optBoolean("isRecurringMonthlyInstallment", false),
                                monthlyInstallmentAmount = safeMonthly,
                                installmentDayOfMonth = obj.optInt("installmentDayOfMonth", 1).coerceIn(1, 31),
                                remainingAmount = safeRemaining,
                                lastInstallmentCreditedMonth = obj.optString("lastInstallmentCreditedMonth", "")
                            )
                        )
                    }
                }

                val smsArchiveList = mutableListOf<ArchivedAlgPosteSmsEntity>()
                val smsArchiveArray = root.optJSONArray("archivedAlgPosteSms")
                if (smsArchiveArray != null) {
                    for (i in 0 until smsArchiveArray.length()) {
                        val obj = smsArchiveArray.getJSONObject(i)
                        val smsId = obj.optString("id", "")
                        val rawBody = obj.optString("rawBody", "")
                        if (smsId.isBlank() || rawBody.isBlank()) continue
                        smsArchiveList.add(
                            ArchivedAlgPosteSmsEntity(
                                id = smsId,
                                sender = obj.optString("sender", "ALG Poste"),
                                rawBody = rawBody,
                                receivedAtMillis = obj.optLong("receivedAtMillis", System.currentTimeMillis()),
                                capturedAtMillis = obj.optLong("capturedAtMillis", System.currentTimeMillis()),
                                captureSource = obj.optString("captureSource", "BACKUP"),
                                extractedBalance = if (obj.has("extractedBalance") && !obj.isNull("extractedBalance")) {
                                    obj.getDouble("extractedBalance")
                                } else null,
                                extractedTxAmount = if (obj.has("extractedTxAmount") && !obj.isNull("extractedTxAmount")) {
                                    obj.getDouble("extractedTxAmount")
                                } else null,
                                extractedIsExpense = if (obj.has("extractedIsExpense") && !obj.isNull("extractedIsExpense")) {
                                    obj.getBoolean("extractedIsExpense")
                                } else null,
                                hasExplicitFeeKeyword = obj.optBoolean("hasExplicitFeeKeyword", false),
                                matchedRule = obj.optString("matchedRule", "UNRECOGNIZED"),
                                studyStatus = obj.optString("studyStatus", "NEEDS_STUDY")
                            )
                        )
                    }
                }

                BackupPayload(
                    exportTimestamp = exportTimestamp,
                    appVersion = appVersion,
                    developer = developer,
                    transactions = transList,
                    debts = debtsList,
                    salaryAmount = salaryAmount,
                    salaryPayday = salaryPayday,
                    salaryEmployer = salaryEmployer,
                    classicCardHolderName = classicCardHolderName,
                    classicCardNumber = classicCardNumber,
                    classicCardExpiry = classicCardExpiry,
                    classicCardCvc = classicCardCvc,
                    archivedAlgPosteSms = smsArchiveList
                )
            }.getOrNull()
        }
    }
}
