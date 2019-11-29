/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*

open class StripeRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "stripe", *columnNames) {

    open var id: String by DbPropertyString("idStripe")
    open var description: String by DbPropertyString("description")
    open var sellerMessage: String by DbPropertyString("sellerMessage")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var amount: Double by DbPropertyDouble("amount")
    open var refund: Double by DbPropertyDouble("refund")
    open var currency: String by DbPropertyString("currency")
    open var amountConverted: Double by DbPropertyDouble("amountConverted")
    open var refundConverted: Double by DbPropertyDouble("refundConverted")
    open var fee: Double by DbPropertyDouble("fee")
    open var tax: Double by DbPropertyDouble("tax")
    open var currencyConverted: String by DbPropertyString("currencyConverted")
    open var status: String by DbPropertyString("status")
    open var statementDescriptor: String by DbPropertyString("statementDescriptor")
    open var customerId: String by DbPropertyString("customerId")
    open var customerDescription: String by DbPropertyString("customerDescription")
    open var customerEmail: String by DbPropertyString("customerEmail")
    open var captured: String by DbPropertyString("captured")
    open var cardId: String by DbPropertyString("cardId")
    open var cardBrand: String by DbPropertyString("cardBrand")
    open var cardLast4: String by DbPropertyString("cardLast4")
    open var invoiceId: String by DbPropertyString("InvoiceId")
    open var transfer: String by DbPropertyString("Transfer")
    open var idAccount: Int by DbPropertyInt("idAccount")
    open var idLedger: Int by DbPropertyInt("idLedger")

}

class Stripe(vararg columnNames: String) : StripeRaw<Stripe>(null, *columnNames) {

    constructor(idStripe: Int) : this() {
        find(idStripe)
    }

    companion object {
        fun import() {
            dbExecute("""
                LOAD DATA INFILE '/var/lib/mysql-files/unified_payments_base.csv' 
                IGNORE INTO TABLE stripe FIELDS TERMINATED BY ',' ENCLOSED BY '"'LINES TERMINATED BY '\n' IGNORE 1 ROWS
                (`idStripe`, `description`, `sellerMessage`, `dateCreated`, `amount`, `refund`, `currency`, 
                `amountConverted`, `refundConverted`, `fee`, `tax`, `currencyConverted`, `status`, 
                `statementDescriptor`, `customerId`, `customerDescription`, `customerEmail`, `captured`, 
                `cardId`, `cardLast4`, `cardBrand`, `InvoiceId`, `Transfer`, `idAccount`)
            """.trimIndent())
            dbExecute("""
                LOAD DATA INFILE '/var/lib/mysql-files/unified_payments.csv' 
                IGNORE INTO TABLE stripe FIELDS TERMINATED BY ',' ENCLOSED BY '"'LINES TERMINATED BY '\n' IGNORE 1 ROWS
                (`idStripe`, `description`, `sellerMessage`, `dateCreated`, `amount`, `refund`, `currency`, 
                `amountConverted`, `refundConverted`, `fee`, `tax`, `currencyConverted`, `status`, 
                `statementDescriptor`, `customerId`, `customerDescription`, `customerEmail`, `captured`, 
                `cardId`, `cardLast4`, `cardBrand`, `InvoiceId`, `Transfer`, `idAccount`)
            """.trimIndent())
            dbExecute("UPDATE ledger JOIN stripe ON stripe.idStripe = ledger.source SET stripe.idLedger = ledger.idLedger WHERE ledger.type = $LEDGER_STRIPE_RECEIPT")
            Stripe().where("status='paid' and idLedger=0") {
                val amount = Math.round(amountConverted * 100).toInt()
                val fee = Math.round(this.fee * 100).toInt()
                val net = amount - fee
                val idStripe = id
                val date = dateCreated
                Account().where("idAccount=$idAccount") {
                    println("Ledger.addStripePayment(Account.codeToId(${code.quoted}), $amount, ${date.softwareDate.quoted}.toDate(), ${idStripe.quoted}, ${cardId.quoted}, ${cardBrand.quoted}, ${cardLast4.quoted}, $fee, $net, ${date.addDays(7).softwareDate.quoted}.toDate())")
                }.otherwise {
                    println("Ledger.addStripePayment(Account.codeToId(\"\"), $amount, ${date.softwareDate.quoted}.toDate(), ${idStripe.quoted}, ${cardId.quoted}, \"\", \"\", $fee, $net, ${date.addDays(7).softwareDate.quoted}.toDate())")
                }
            }
            
        }
    }
}