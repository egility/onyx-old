package org.egility.library.general

import org.egility.library.dbobject.BankTransaction
import org.egility.library.transport.ApiClient
import java.util.*

object Starling {


    val KEY = "z2U4DLDx35NYt8ktqU8ynF89j0qZFcr6oT56ivfskgzpLmldEuVpf5xWf2B1qwL3"
    val SANDBOX_KEY = "159dEnqjB3DeWSWZ3Dv5vfu1erVbtVJpRBMPibaxp8fur7rMCFX3ajMsqj2U8TGj"
    val HOST = "https://api.starlingbank.com"
    val SANDBOX_HOST = "https://api-sandbox.starlingbank.com"

    val GET_TRANSACTIONS = "api/v1/transactions"
    val GET_FEED = "api/v2/feed"
    val GET_ACCOUNTS = "api/v2/accounts"

    var host = HOST
    var key = KEY

    var sandbox = false
        set(value) {
            if (value != field) {
                field = value
                host = if (field) SANDBOX_HOST else HOST
                key = if (field) SANDBOX_KEY else KEY
            }
        }


    fun test(resource: String) {
        println("=====================")
        println(resource)
        println("=====================")
        val json = getResource(resource)
        println(json.toJson(pretty = true))
    }

    fun sandbox(resource: String) {
        sandbox = true
        println("=====================")
        println(resource)
        println("=====================")
        val json = getResource(resource)
        println(json.toJson(pretty = true))
    }

    fun getResource(resource: String): JsonNode {
        var result = Json.nullNode()
        val url = "$host/$resource"
        val response = ApiClient.get(url) { request ->
            request.addHeader("Authorization", "Bearer $key")
        }
        if (response.code != 200) {
            println("HTTP error code : ${response.code} (${response.message})")
        } else {
            result = response.body
        }
        return result
    }


    fun putResource(resource: String, body: JsonNode): JsonNode {
        var result = Json.nullNode()
        val url = "$host/$resource"
        val response = ApiClient.put(url, body) { request ->
            request.addHeader("Authorization", "Bearer $key")
        }
        if (response.code != 200) {
            println("HTTP error code : ${response.code} (${response.message})")
            println("Failed : Body : " + body.toJson(pretty = true))
        } else {
            result = response.body
        }
        return result
    }

    fun processTransactions2(from: Date) {
        val query = if (from.isNotEmpty()) "?from=${from.softwareDate}" else ""
        val json = getResource(GET_TRANSACTIONS + query)
        if (json.has("_embedded.transactions")) {
            for (index in json["_embedded.transactions"].count() - 1 downTo 0) {
                val transaction = json["_embedded.transactions"][index]
                val id = transaction["id"].asString
                val currency = transaction["currency"].asString
                val amount = transaction["amount"].asDouble.pence
                val direction = transaction["direction"].asString
                val created = transaction["created"].asDate
                val narrative = transaction["narrative"].asString
                val source = transaction["source"].asString
                val balance = transaction["balance"].asDouble.pence

                val paidOut = if (amount < 0) -amount else 0
                val paidIn = if (amount >= 0) amount else 0

                val counterParty = "via API"

                BankTransaction.add(
                    id,
                    created,
                    source.toLowerCase(),
                    narrative,
                    counterParty,
                    paidOut,
                    paidIn,
                    balance
                )
            }
        }
    }

    fun processTransactions(from: Date) {
        val accountsNode = getResource(GET_ACCOUNTS)
        if (accountsNode.has("accounts")) {
            val accountUid = accountsNode["accounts.0.accountUid"].asString
            val defaultCategory = accountsNode["accounts.0.defaultCategory"].asString
            val query = GET_FEED + "/account/$accountUid/category/$defaultCategory"
            val feedNode = getResource(query)
            if (feedNode.has("feedItems")) {
                for (index in feedNode["feedItems"].count() - 1 downTo 0) {
                    val transaction = feedNode["feedItems"][index]
                    val id = transaction["feedItemUid"].asString
                    val currency = transaction["amount.currency"].asString
                    val amount = transaction["amount.minorUnits"].asInt
                    val direction = transaction["direction"].asString
                    val created = transaction["transactionTime"].asDate
                    val narrative = transaction["reference"].asString
                    val source = transaction["source"].asString
                    val balance = transaction["balance"].asDouble.pence
                    val counterParty = transaction["counterPartyName"].asString

                    val paidOut = if (amount < 0) -amount else 0
                    val paidIn = if (amount >= 0) amount else 0

                    if (created>=from) {

                        BankTransaction.add(
                            id,
                            created,
                            source.toLowerCase(),
                            narrative,
                            counterParty,
                            paidOut,
                            paidIn,
                            balance
                        )
                    }
                }
            }
        }
    }



    fun findOrAddPayee(accountName: String, accountNumber: String, sortCode: String): String {
        val response = getResource("api/v2/payees")
        for (payee in response["payees"]) {
            val payeeAccountName = payee["payeeName"].asString.trim()
            val payeeAccountNumber = payee["accounts.0.accountIdentifier"].asString
            val payeeSortCode = payee["accounts.0.bankIdentifier"].asString
            val payeeAccountUid = payee["accounts.0.payeeAccountUid"].asString
            if (payeeAccountName == accountName && payeeAccountNumber == accountNumber && payeeSortCode == sortCode) {
                return payeeAccountUid
            }
        }
        return createPayee(accountName, accountNumber, sortCode)
    }

    fun createPayee(accountName: String, accountNumber: String, sortCode: String): String {
        val body = Json.nullNode()

        body["payeeName"] = accountName.trim()
        body["payeeType"] = "BUSINESS"
        body["businessName"] = accountName
        val account = body["accounts"].addElement()
        account["description"] = "main"
        account["defaultAccount"] = true
        account["countryCode"] = "GB"
        account["accountIdentifier"] = accountNumber
        account["bankIdentifier"] = sortCode
        account["bankIdentifierType"] = "SORT_CODE"

        val response = putResource("api/v2/payees", body)
        if (response["success"].asBoolean) {
            val payeeUid = response["payeeUid"].asString
            val payeesNode = getResource("/api/v2/payees")
            for (payee in payeesNode["payees"]) {
                if (payee["payeeUid"].asString == payeeUid) {
                    return payee["accounts.0.payeeAccountUid"].asString
                }
            }
        }
        return ""
    }
}