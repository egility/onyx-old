/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.tools

import com.stripe.Stripe
import com.stripe.model.BalanceTransaction
import com.stripe.model.Charge
import org.egility.library.general.Global
import org.egility.library.general.Json
import org.egility.library.general.JsonNode
import org.egility.library.general.doNothing


object PaymentCard {

    private val secretKey = if (Global.live) "sk_live_dbfRsbOkwh7J5C8N7v5OZkWR" else "sk_test_B1cgQmZ0sq1VFR4CWpsI0WTh"
    val publicKey = if (Global.live) "pk_live_DdyKeXqGIeF1lFZe3HMwVPe1" else "pk_test_9PflZV7uZVWLs1qOnlNxbpro"

    fun chargeStripe(idCompetitor: Int, token: JsonNode, amount: Int, description: String = "Example charge"): JsonNode {
        try {
            Stripe.apiKey = secretKey

            val metadata = HashMap<String, String>()
            metadata.put("idCompetitor", idCompetitor.toString())

            val params = HashMap<String, Any>()
            params.put("amount", amount)
            params.put("currency", "gbp")
            params.put("description", description)
            params.put("source", token["id"].asString)
            params.put("metadata", metadata)

            val charge = Charge.create(params)
            val result = Json(charge.toJson())
            try {
                val transaction = BalanceTransaction.retrieve(charge.balanceTransaction)
                result["transaction"] = Json(transaction.toJson())
            }  catch (e: Throwable) {
                doNothing()
            }
            return result
        } catch (e: Throwable) {
            val result = Json()
            result["status"]="fatal"
            result["message"]=e.message?:"No Error Message"
            return result
        }
    }


}