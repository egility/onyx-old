/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*

open class BankPaymentRequestRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) :
    DbTable<T>(_connection, "bankPaymentRequest", *columnNames) {
    open var id: Int by DbPropertyInt("idBankPaymentRequest")
    open var idAccount: Int by DbPropertyInt("idAccount")
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var idBankTransaction: Int by DbPropertyInt("idBankTransaction")
    open var debitAccount: Int by DbPropertyInt("debitAccount")
    open var accountName: String by DbPropertyString("accountName")
    open var accountNumber: String by DbPropertyString("accountNumber")
    open var sortCode: String by DbPropertyString("sortCode")
    open var transactionReference: String by DbPropertyString("transactionReference")
    open var amount: Int by DbPropertyInt("amount")
    open var sourceId: String by DbPropertyString("sourceId")
    open var confirmationRequired: Boolean by DbPropertyBoolean("confirmationRequired")
    open var extra: Json by DbPropertyJson("extra")
    open var dateConfirmed: Date by DbPropertyDate("dateConfirmed")
    open var datePaid: Date by DbPropertyDate("datePaid")
    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")

    var beneficiary: String by DbPropertyJsonString("extra", "beneficiary")
}

class BankPaymentRequest(vararg columnNames: String) : BankPaymentRequestRaw<BankPaymentRequest>(null, *columnNames) {

    constructor(idBankPaymentRequest: Int) : this() {
        find(idBankPaymentRequest)
    }

    val isExpired: Boolean
        get() = confirmationRequired && dateConfirmed.isEmpty() && dateCreated.before(now.addHours(-1))

    enum class ConfirmState { OK, ALREADY_CONFIRMED, EXPIRED, FUNDS_UNAVAILABLE }

    fun confirm(): ConfirmState {
        if (!confirmationRequired || dateConfirmed.isNotEmpty()) return ConfirmState.ALREADY_CONFIRMED
        if (isExpired) return ConfirmState.EXPIRED
        if (idAccount > 0 && Account.maxRefund(idAccount) < 0) return ConfirmState.FUNDS_UNAVAILABLE
        dbTransaction {
            dateConfirmed = now
            post()
            Starling.findOrAddPayee(accountName, accountNumber, sortCode)
            PlazaMessage.refundRequested(accountName, amount, "Refund")
        }
        return ConfirmState.OK
    }

    companion object {

        fun checkStarling() {
            BankPaymentRequest().where("datePaid=0 AND dateConfirmed>0 AND accountName<>'Miss D E Weave'") {
                if (sortCode.length==6) {
                    Starling.findOrAddPayee(accountName, accountNumber, sortCode)
                }
            }
        }
        
        fun addRequest(
            idAccount: Int,
            idCompetition: Int,
            debitAccount: Int,
            accountName: String,
            sortCode: String,
            accountNumber: String,
            amount: Int,
            confirmationRequired: Boolean,
            reference: String,
            beneficiary: String=""
        ): Int {
            val bankPaymentRequest = BankPaymentRequest()
            bankPaymentRequest.append()
            bankPaymentRequest.idAccount = idAccount
            bankPaymentRequest.idCompetition = idCompetition
            bankPaymentRequest.debitAccount = debitAccount
            bankPaymentRequest.accountName = accountName
            bankPaymentRequest.sortCode = sortCode
            bankPaymentRequest.accountNumber = accountNumber
            bankPaymentRequest.amount = amount
            bankPaymentRequest.confirmationRequired = confirmationRequired
            bankPaymentRequest.transactionReference = reference
            bankPaymentRequest.beneficiary = beneficiary
            if (!confirmationRequired) bankPaymentRequest.dateConfirmed = now
            bankPaymentRequest.post()
            if (!confirmationRequired && sortCode.length==6) {
                Starling.findOrAddPayee(accountName, accountNumber, sortCode)
                PlazaMessage.refundRequested(accountName, amount, reference)
            }
            return bankPaymentRequest.id
        }

        fun refundAccount(
            idAccount: Int,
            accountName: String,
            sortCode: String,
            accountNumber: String,
            amount: Int,
            confirmationRequired: Boolean
        ): Int {
            var result = 0
            Account().seek(idAccount) {
                result = addRequest(
                    idAccount,
                    0,
                    ACCOUNT_USER,
                    accountName,
                    sortCode,
                    accountNumber,
                    amount,
                    confirmationRequired,
                    "Refund"
                )
            }
            return result
        }

        fun accountDonation(
            idAccount: Int,
            accountName: String,
            sortCode: String,
            accountNumber: String,
            amount: Int,
            beneficiary: String
        ): Int {
            var result = 0
            Account().seek(idAccount) {
                result = addRequest(
                    idAccount,
                    0,
                    ACCOUNT_USER,
                    accountName,
                    sortCode,
                    accountNumber,
                    amount,
                    false,
                    "Donation (${competitor.fullName})",
                    beneficiary
                )
            }
            return result
        }
        
        fun showFees(idCompetition: Int, amount: Int): Int {
            var result = 0
            Competition().seek(idCompetition) {
                when (idOrganization) {
                    ORGANIZATION_UKA, ORGANIZATION_UK_OPEN -> {
                        result = addRequest(
                            0,
                            idCompetition,
                            ACCOUNT_SHOW_HOLDING,
                            "UK AGILITY",
                            "309311",
                            "00487001",
                            amount,
                            false,
                            uniqueName
                        )
                    }
                    else -> {
                        result = addRequest(
                            0,
                            idCompetition,
                            ACCOUNT_SHOW_HOLDING,
                            bankAccountName,
                            bankAccountSort,
                            bankAccountNumber,
                            amount,
                            false,
                            "Entry Fees $uniqueName"
                        )
                    }
                }
            }
            return result
        }

        fun showAdvance(idCompetition: Int, amount: Int): Int {
            var result = 0
            if (amount > 0) {
                Competition().seek(idCompetition) {
                    when (idOrganization) {
                        ORGANIZATION_UKA, ORGANIZATION_UK_OPEN -> {
                            result = addRequest(
                                0,
                                idCompetition,
                                ACCOUNT_SHOW_HOLDING,
                                "UK AGILITY",
                                "309311",
                                "00487001",
                                amount,
                                false,
                                "$uniqueName Advance"
                            )
                        }
                        else -> {
                            result = addRequest(
                                0,
                                idCompetition,
                                ACCOUNT_SHOW_HOLDING,
                                bankAccountName,
                                bankAccountSort,
                                bankAccountNumber,
                                amount,
                                false,
                                "$uniqueName Advance"
                            )
                        }
                    }
                }
            }
            return result
        }

        fun showThirdPartyAdvance(idCompetition: Int, amount: Int, bankAccountName: String, bankAccountSort: String, bankAccountNumber: String, reference: String): Int {
            var result = 0
            if (amount > 0) {
                Competition().seek(idCompetition) {
                    result = addRequest(
                        0,
                        idCompetition,
                        ACCOUNT_SHOW_HOLDING,
                        bankAccountName,
                        bankAccountSort,
                        bankAccountNumber,
                        amount,
                        false,
                        reference
                    )
                }
            }
            return result
        }

        fun ukaRegistrationFees(amount: Int): Int {
            return addRequest(0, 0, ACCOUNT_UKA_HOLDING, "UK AGILITY", "309311", "00487001", amount, false, "Registration Fes")
        }

        fun SwapFees(amount: Int): Int {
            return addRequest(0, 0, ACCOUNT_SWAP, "SWAP", "090128", "48302327", amount, false, "Admin Fes")
        }

    }
}