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
    val classicCardHolderName: String = "CARDHOLDER",
    val classicCardNumber: String = "",
    val classicCardExpiry: String = "11/29",
    val classicCardCvc: String = "",
    val archivedAlgPosteSms: List<ArchivedAlgPosteSmsEntity> = emptyList(),
    val classicCardBankStyle: String = "EDAHABIA",
    val visaCardProvider: String = "DEFAULT",
    val visaCardHolderName: String = "",
    val visaCardNumber: String = "",
    val visaCardExpiry: String = "08/28",
    val visaCardCvc: String = "",
    val thirdCardEnabled: Boolean = false,
    val thirdCardProvider: String = "BINANCE",
    val thirdCardHolderName: String = "",
    val thirdCardNumber: String = "",
    val thirdCardExpiry: String = "08/28",
    val thirdCardCvc: String = "",
    val vaultKeysJson: String = "[]",
    val ccpAccountsJson: String = "[]",
    val savingsWalletBalance: Double? = null,
    val savingsGoalAmount: Double? = null,
    val savingsHistoryJson: String? = null,
    val includeAssetsInSavingsTotal: Boolean? = null,
    val goldAssetHoldingsJson: String? = null,
    val binanceHoldingsJson: String? = null,
    val cardHubOrderJson: String? = null,
    val localUsername: String? = null,
    val localWilayaCode: String? = null,
    val localWilayaName: String? = null,
    val localCommuneName: String? = null
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
        cardObj.put("bankStyle", classicCardBankStyle)
        cardObj.put("holderName", classicCardHolderName)
        cardObj.put("cardNumber", if (includeSensitiveCardFields) classicCardNumber else "")
        cardObj.put("expiryDate", classicCardExpiry)
        cardObj.put("cvc", if (includeSensitiveCardFields) classicCardCvc else "")
        root.put("classicCardSettings", cardObj)

        // Visa / Crypto Card Details
        val visaObj = JSONObject()
        visaObj.put("provider", visaCardProvider)
        visaObj.put("holderName", visaCardHolderName)
        visaObj.put("cardNumber", if (includeSensitiveCardFields) visaCardNumber else "")
        visaObj.put("expiryDate", visaCardExpiry)
        visaObj.put("cvc", if (includeSensitiveCardFields) visaCardCvc else "")
        root.put("visaCardSettings", visaObj)

        // Third Card Details
        val thirdObj = JSONObject()
        thirdObj.put("enabled", thirdCardEnabled)
        thirdObj.put("provider", thirdCardProvider)
        thirdObj.put("holderName", thirdCardHolderName)
        thirdObj.put("cardNumber", if (includeSensitiveCardFields) thirdCardNumber else "")
        thirdObj.put("expiryDate", thirdCardExpiry)
        thirdObj.put("cvc", if (includeSensitiveCardFields) thirdCardCvc else "")
        root.put("thirdCardSettings", thirdObj)

        // Encrypted-Only Secrets (Keys Vault & CCP Accounts — always redacted in plaintext backups)
        val vaultSecretsObj = JSONObject()
        vaultSecretsObj.put("vaultKeysJson", if (includeSensitiveCardFields) vaultKeysJson else "[]")
        vaultSecretsObj.put("ccpAccountsJson", if (includeSensitiveCardFields) ccpAccountsJson else "[]")
        root.put("vaultSecrets", vaultSecretsObj)

        // Savings & Assets Settings
        if (
            savingsWalletBalance != null ||
            savingsGoalAmount != null ||
            savingsHistoryJson != null ||
            includeAssetsInSavingsTotal != null
        ) {
            val savingsObj = JSONObject()
            if (savingsWalletBalance != null) savingsObj.put("walletBalance", savingsWalletBalance)
            if (savingsGoalAmount != null) savingsObj.put("goalAmount", savingsGoalAmount)
            if (savingsHistoryJson != null) savingsObj.put("historyJson", savingsHistoryJson)
            if (includeAssetsInSavingsTotal != null) savingsObj.put("includeAssetsInTotal", includeAssetsInSavingsTotal)
            root.put("savingsSettings", savingsObj)
        }

        if (goldAssetHoldingsJson != null || binanceHoldingsJson != null) {
            val assetsObj = JSONObject()
            if (goldAssetHoldingsJson != null) assetsObj.put("goldAssetHoldingsJson", goldAssetHoldingsJson)
            if (binanceHoldingsJson != null) assetsObj.put("binanceHoldingsJson", binanceHoldingsJson)
            root.put("assetHoldings", assetsObj)
        }

        if (cardHubOrderJson != null) {
            root.put("cardHubOrderJson", cardHubOrderJson)
        }

        if (
            localUsername != null ||
            localWilayaCode != null ||
            localWilayaName != null ||
            localCommuneName != null
        ) {
            val localAccountObj = JSONObject()
            if (localUsername != null) localAccountObj.put("username", localUsername)
            if (localWilayaCode != null) localAccountObj.put("wilayaCode", localWilayaCode)
            if (localWilayaName != null) localAccountObj.put("wilayaName", localWilayaName)
            if (localCommuneName != null) localAccountObj.put("communeName", localCommuneName)
            root.put("localAccount", localAccountObj)
        }

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
                val classicCardBankStyle = cardObj?.optString("bankStyle", "EDAHABIA") ?: "EDAHABIA"
                val classicCardHolderName = cardObj?.optString("holderName", "CARDHOLDER") ?: "CARDHOLDER"
                val classicCardNumber = cardObj?.optString("cardNumber", "") ?: ""
                val classicCardExpiry = cardObj?.optString("expiryDate", "11/29") ?: "11/29"
                val classicCardCvc = cardObj?.optString("cvc", "") ?: ""

                val visaObj = root.optJSONObject("visaCardSettings")
                val visaCardProvider = visaObj?.optString("provider", "DEFAULT") ?: "DEFAULT"
                val visaCardHolderName = visaObj?.optString("holderName", "") ?: ""
                val visaCardNumber = visaObj?.optString("cardNumber", "") ?: ""
                val visaCardExpiry = visaObj?.optString("expiryDate", "08/28") ?: "08/28"
                val visaCardCvc = visaObj?.optString("cvc", "") ?: ""

                val thirdObj = root.optJSONObject("thirdCardSettings")
                val thirdCardEnabled = thirdObj?.optBoolean("enabled", false) ?: false
                val thirdCardProvider = thirdObj?.optString("provider", "BINANCE") ?: "BINANCE"
                val thirdCardHolderName = thirdObj?.optString("holderName", "") ?: ""
                val thirdCardNumber = thirdObj?.optString("cardNumber", "") ?: ""
                val thirdCardExpiry = thirdObj?.optString("expiryDate", "08/28") ?: "08/28"
                val thirdCardCvc = thirdObj?.optString("cvc", "") ?: ""

                val vaultSecretsObj = root.optJSONObject("vaultSecrets")
                val vaultKeysJson = vaultSecretsObj?.optString("vaultKeysJson", "[]") ?: "[]"
                val ccpAccountsJson = vaultSecretsObj?.optString("ccpAccountsJson", "[]") ?: "[]"

                val savingsObj = root.optJSONObject("savingsSettings")
                val savingsWalletBalance = savingsObj?.takeIf { it.has("walletBalance") && !it.isNull("walletBalance") }
                    ?.optDouble("walletBalance", Double.NaN)
                    ?.takeIf { it.isFinite() && it >= 0.0 }
                val savingsGoalAmount = savingsObj?.takeIf { it.has("goalAmount") && !it.isNull("goalAmount") }
                    ?.optDouble("goalAmount", Double.NaN)
                    ?.takeIf { it.isFinite() && it >= 0.0 }
                val savingsHistoryJson = savingsObj?.takeIf { it.has("historyJson") && !it.isNull("historyJson") }
                    ?.optString("historyJson", "[]")
                val includeAssetsInSavingsTotal = savingsObj?.takeIf {
                    it.has("includeAssetsInTotal") && !it.isNull("includeAssetsInTotal")
                }?.optBoolean("includeAssetsInTotal")

                val assetsObj = root.optJSONObject("assetHoldings")
                val goldAssetHoldingsJson = assetsObj?.takeIf {
                    it.has("goldAssetHoldingsJson") && !it.isNull("goldAssetHoldingsJson")
                }?.optString("goldAssetHoldingsJson", "[]")
                val binanceHoldingsJson = assetsObj?.takeIf {
                    it.has("binanceHoldingsJson") && !it.isNull("binanceHoldingsJson")
                }?.optString("binanceHoldingsJson", "[]")

                val cardHubOrderJson = if (root.has("cardHubOrderJson") && !root.isNull("cardHubOrderJson")) {
                    root.optString("cardHubOrderJson", "")
                } else null

                val localAccountObj = root.optJSONObject("localAccount")
                val localUsername = localAccountObj?.takeIf { it.has("username") && !it.isNull("username") }
                    ?.optString("username", "")
                val localWilayaCode = localAccountObj?.takeIf { it.has("wilayaCode") && !it.isNull("wilayaCode") }
                    ?.optString("wilayaCode", "")
                val localWilayaName = localAccountObj?.takeIf { it.has("wilayaName") && !it.isNull("wilayaName") }
                    ?.optString("wilayaName", "")
                val localCommuneName = localAccountObj?.takeIf { it.has("communeName") && !it.isNull("communeName") }
                    ?.optString("communeName", "")

                val transList = mutableListOf<TransactionEntity>()
                val transArray = root.optJSONArray("transactions")
                if (transArray != null) {
                    for (i in 0 until transArray.length()) {
                        val obj = transArray.optJSONObject(i) ?: continue
                        val txType = runCatching { TransactionType.valueOf(obj.optString("type", "")) }.getOrNull()
                            ?: continue
                        val txAmount = obj.optDouble("amount", Double.NaN)
                        if (!txAmount.isFinite() || txAmount < 0.0) continue
                        val category = obj.optString("category", "")
                        val title = obj.optString("title", "")
                        if (category.isBlank() && title.isBlank()) continue
                        transList.add(
                            TransactionEntity(
                                id = 0, // Auto-generate new IDs on restore
                                type = txType,
                                amount = txAmount,
                                category = category.ifBlank { "other" },
                                title = title.ifBlank { category.ifBlank { "Transaction" } },
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
                        val obj = debtsArray.optJSONObject(i) ?: continue
                        val debtType = runCatching { DebtType.valueOf(obj.optString("type", "")) }.getOrNull()
                            ?: continue
                        val debtAmount = obj.optDouble("amount", Double.NaN)
                        if (!debtAmount.isFinite() || debtAmount < 0.0) continue
                        val personName = obj.optString("personName", "").trim()
                        if (personName.isBlank()) continue
                        val rawRemaining = obj.optDouble("remainingAmount", debtAmount)
                        val safeRemaining = if (rawRemaining.isFinite() && rawRemaining >= 0.0) rawRemaining else debtAmount
                        val rawMonthly = obj.optDouble("monthlyInstallmentAmount", 0.0)
                        val safeMonthly = if (rawMonthly.isFinite() && rawMonthly >= 0.0) rawMonthly else 0.0
                        debtsList.add(
                            DebtEntity(
                                id = 0,
                                type = debtType,
                                personName = personName,
                                amount = debtAmount,
                                note = obj.optString("note", ""),
                                dueDateMillis = obj.optLong("dueDateMillis", System.currentTimeMillis()),
                                autoPayOnDueDate = obj.optBoolean("autoPayOnDueDate", true),
                                isPaid = obj.optBoolean("isPaid", false),
                                paidDateMillis = if (obj.has("paidDateMillis") && !obj.isNull("paidDateMillis")) obj.optLong("paidDateMillis") else null,
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
                        val obj = smsArchiveArray.optJSONObject(i) ?: continue
                        val smsId = obj.optString("id", "")
                        val rawBody = obj.optString("rawBody", "")
                        if (smsId.isBlank() || rawBody.isBlank()) continue
                        val extractedBalance = if (obj.has("extractedBalance") && !obj.isNull("extractedBalance")) {
                            obj.optDouble("extractedBalance", Double.NaN).takeIf { it.isFinite() }
                        } else null
                        val extractedTxAmount = if (obj.has("extractedTxAmount") && !obj.isNull("extractedTxAmount")) {
                            obj.optDouble("extractedTxAmount", Double.NaN).takeIf { it.isFinite() }
                        } else null
                        val extractedIsExpense = if (obj.has("extractedIsExpense") && !obj.isNull("extractedIsExpense")) {
                            obj.optBoolean("extractedIsExpense")
                        } else null
                        smsArchiveList.add(
                            ArchivedAlgPosteSmsEntity(
                                id = smsId,
                                sender = obj.optString("sender", "ALG Poste"),
                                rawBody = rawBody,
                                receivedAtMillis = obj.optLong("receivedAtMillis", System.currentTimeMillis()),
                                capturedAtMillis = obj.optLong("capturedAtMillis", System.currentTimeMillis()),
                                captureSource = obj.optString("captureSource", "BACKUP"),
                                extractedBalance = extractedBalance,
                                extractedTxAmount = extractedTxAmount,
                                extractedIsExpense = extractedIsExpense,
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
                    archivedAlgPosteSms = smsArchiveList,
                    classicCardBankStyle = classicCardBankStyle,
                    visaCardProvider = visaCardProvider,
                    visaCardHolderName = visaCardHolderName,
                    visaCardNumber = visaCardNumber,
                    visaCardExpiry = visaCardExpiry,
                    visaCardCvc = visaCardCvc,
                    thirdCardEnabled = thirdCardEnabled,
                    thirdCardProvider = thirdCardProvider,
                    thirdCardHolderName = thirdCardHolderName,
                    thirdCardNumber = thirdCardNumber,
                    thirdCardExpiry = thirdCardExpiry,
                    thirdCardCvc = thirdCardCvc,
                    vaultKeysJson = vaultKeysJson,
                    ccpAccountsJson = ccpAccountsJson,
                    savingsWalletBalance = savingsWalletBalance,
                    savingsGoalAmount = savingsGoalAmount,
                    savingsHistoryJson = savingsHistoryJson,
                    includeAssetsInSavingsTotal = includeAssetsInSavingsTotal,
                    goldAssetHoldingsJson = goldAssetHoldingsJson,
                    binanceHoldingsJson = binanceHoldingsJson,
                    cardHubOrderJson = cardHubOrderJson,
                    localUsername = localUsername,
                    localWilayaCode = localWilayaCode,
                    localWilayaName = localWilayaName,
                    localCommuneName = localCommuneName
                )
            }.getOrNull()
        }
    }
}
