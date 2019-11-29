/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*

open class BankTransactionRaw<T: DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "BankTransaction", *columnNames) {
    open var id: Int by DbPropertyInt("idBankTransaction")
    open var idSource: String by DbPropertyString("idSource")
    open var idLedger: Int by DbPropertyInt("idLedger")
    open var transactionDate: Date by DbPropertyDate("transactionDate")
    open var transactionType: String by DbPropertyString("transactionType")
    open var transactionReference: String by DbPropertyString("transactionReference")
    open var counterParty: String by DbPropertyString("counterParty")
    open var paidOut: Int by DbPropertyInt("paidOut")
    open var paidIn: Int by DbPropertyInt("paidIn")
    open var balance: Int by DbPropertyInt("balance")
    open var revisedReference: String by DbPropertyString("revisedReference")
    open var flag: Boolean by DbPropertyBoolean("flag")
}

class BankTransaction(vararg columnNames: String) : BankTransactionRaw<BankTransaction>(null, *columnNames) {

    constructor(idBankTransaction: Int) : this() {
        find(idBankTransaction)
    }

    companion object {
        fun isCompetition(competition: Competition, transactionReference: String): Boolean {
            var idText=""
            if (transactionReference.trim().startsWith("UK AGILITY")) {
                idText=transactionReference.substringAfter("UK AGILITY").trim()
            }
            if (transactionReference.trim().contains(" KC ")) {
                idText=transactionReference.substringAfter(" KC ").trim()
            }
            if (idText.isNotEmpty()) {
                return competition.find(idText.toIntDef(-1))
            } else {
                return false
            }
        }

        fun add(idSource: String, transactionDate: Date, transactionType: String, transactionReference: String, 
                counterParty: String, paidOut: Int, paidIn: Int, balance: Int): Int {
            val bankTransaction = BankTransaction()
            val account = Account()
            val competition = Competition()
            val bankPaymentRequest = BankPaymentRequest()
            var wrongReference = false
            var donation = false
            var beneficiary = ""

            if (!bankTransaction.find("idSource=${idSource.quoted}")) {
                dbTransaction {
                    bankTransaction.append()
                    bankTransaction.idSource = idSource
                    bankTransaction.transactionDate = transactionDate
                    bankTransaction.transactionType = transactionType
                    bankTransaction.transactionReference = transactionReference
                    bankTransaction.counterParty = counterParty
                    bankTransaction.paidOut = paidOut
                    bankTransaction.paidIn = paidIn
                    bankTransaction.balance = balance

                    if (paidOut>0) {
                        bankPaymentRequest.select("datePaid=0 AND accountName=${counterParty.quoted} AND amount=$paidOut AND (NOT confirmationRequired OR dateConfirmed<>0)").first()
                    }

                    if (bankPaymentRequest.isOnRow) {
                        bankPaymentRequest.datePaid = now
                        bankPaymentRequest.post()
                        donation = bankPaymentRequest.transactionReference.startsWith("Donation")
                        beneficiary = bankPaymentRequest.beneficiary
                        if (bankPaymentRequest.idAccount> 0) account.seek(bankPaymentRequest.idAccount)
                        if (bankPaymentRequest.idCompetition> 0) competition.seek(bankPaymentRequest.idCompetition)
                    } else {
                        val state=account.seekReference(transactionReference)
                        if (state!=0) {
                            bankTransaction.revisedReference = account.code
                            wrongReference = state < 0
                        } else if (BankTransaction.isCompetition(competition, transactionReference)) {
                            bankTransaction.revisedReference = competition.uniqueName
                        }
                    }
                    val idLedgerAccount = when {
                        competition.isOnRow -> ACCOUNT_SHOW_HOLDING
                        account.isOnRow -> ACCOUNT_USER
                        bankPaymentRequest.isOnRow -> bankPaymentRequest.debitAccount
                        transactionType.eq("interest_payment") -> ACCOUNT_BANK_FEES
                        counterParty.toLowerCase().contains("ald bank sme dep") -> ACCOUNT_BANK_FEES
                        transactionReference.toLowerCase().startsWith("swap") -> ACCOUNT_SWAP_XFER
                        transactionReference.toLowerCase().contains("aldermore") -> ACCOUNT_ALDERMORE
                        counterParty.toLowerCase().contains("aldermore") -> ACCOUNT_ALDERMORE
                        transactionReference.toLowerCase().startsWith("stannp.com") -> ACCOUNT_PRINT
                        transactionReference.toLowerCase().contains("stripe") -> ACCOUNT_STRIPE_PEND
                        transactionReference.contains("UKA-TFERS") -> ACCOUNT_UKA_XFER
                        transactionReference.contains("UKA REGISTRATIONS") -> ACCOUNT_UKA_HOLDING
                        else -> ACCOUNT_UNKNOWN
                    }

                    val ledgerReference = "$counterParty $transactionReference".trim()
                    val type = if (donation) LEDGER_ELECTRONIC_DONATION else if (account.id > 0) LEDGER_ELECTRONIC_REFUND else LEDGER_ELECTRONIC_PAYMENT

                    bankTransaction.idLedger = if (paidOut > 0)
                        Ledger.addElectronicPayment(
                            idLedgerAccount,
                            account.id,
                            competition.id,
                            transactionDate,
                            paidOut,
                            ledgerReference,
                            type,
                            beneficiary
                        )
                    else
                        Ledger.addElectronicReceipt(
                            idLedgerAccount,
                            account.id,
                            competition.id,
                            transactionDate,
                            paidIn,
                            ledgerReference,
                            wrongReference,
                            ACCOUNT_STARLING
                        )

                    bankTransaction.post()
                }
            } else {
                bankTransaction.flag = true
                bankTransaction.post()
            }
            return bankTransaction.id
        }
    }
}