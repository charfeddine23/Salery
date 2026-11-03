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
    val salaryEmployer: String
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("appName", "SaleryPelse")
        root.put("appVersion", appVersion)
        root.put("developer", developer)
        root.put("exportTimestamp", exportTimestamp)

        // Salary Settings
        val salaryObj = JSONObject()
        salaryObj.put("amount", salaryAmount)
        salaryObj.put("payday", salaryPayday)
        salaryObj.put("employer", salaryEmployer)
        root.put("salarySettings", salaryObj)

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

        return root.toString(2)
    }

    companion object {
        fun fromJsonString(jsonStr: String): BackupPayload? {
            return runCatching {
                val root = JSONObject(jsonStr)
                val appVersion = root.optString("appVersion", "1.0.0")
                val developer = root.optString("developer", "Kahlessenane Charfeddine")
                val exportTimestamp = root.optLong("exportTimestamp", System.currentTimeMillis())

                val salaryObj = root.optJSONObject("salarySettings")
                val salaryAmount = salaryObj?.optDouble("amount", 85000.0) ?: 85000.0
                val salaryPayday = salaryObj?.optInt("payday", 25) ?: 25
                val salaryEmployer = salaryObj?.optString("employer", "Enterprise Algiers") ?: "Enterprise Algiers"

                val transList = mutableListOf<TransactionEntity>()
                val transArray = root.optJSONArray("transactions")
                if (transArray != null) {
                    for (i in 0 until transArray.length()) {
                        val obj = transArray.getJSONObject(i)
                        transList.add(
                            TransactionEntity(
                                id = 0, // Auto-generate new IDs on restore
                                type = TransactionType.valueOf(obj.getString("type")),
                                amount = obj.getDouble("amount"),
                                category = obj.getString("category"),
                                title = obj.getString("title"),
                                note = obj.optString("note", ""),
                                dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                                isSalaryCredit = obj.optBoolean("isSalaryCredit", false)
                            )
                        )
                    }
                }

                val debtsList = mutableListOf<DebtEntity>()
                val debtsArray = root.optJSONArray("debts")
                if (debtsArray != null) {
                    for (i in 0 until debtsArray.length()) {
                        val obj = debtsArray.getJSONObject(i)
                        debtsList.add(
                            DebtEntity(
                                id = 0,
                                type = DebtType.valueOf(obj.getString("type")),
                                personName = obj.getString("personName"),
                                amount = obj.getDouble("amount"),
                                note = obj.optString("note", ""),
                                dueDateMillis = obj.getLong("dueDateMillis"),
                                autoPayOnDueDate = obj.optBoolean("autoPayOnDueDate", true),
                                isPaid = obj.optBoolean("isPaid", false),
                                paidDateMillis = if (obj.has("paidDateMillis")) obj.getLong("paidDateMillis") else null,
                                createdDateMillis = obj.optLong("createdDateMillis", System.currentTimeMillis()),
                                isRecurringMonthlyInstallment = obj.optBoolean("isRecurringMonthlyInstallment", false),
                                monthlyInstallmentAmount = obj.optDouble("monthlyInstallmentAmount", 0.0),
                                installmentDayOfMonth = obj.optInt("installmentDayOfMonth", 1),
                                remainingAmount = obj.optDouble("remainingAmount", obj.getDouble("amount")),
                                lastInstallmentCreditedMonth = obj.optString("lastInstallmentCreditedMonth", "")
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
                    salaryEmployer = salaryEmployer
                )
            }.getOrNull()
        }
    }
}
