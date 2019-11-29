/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_pay.*
import org.egility.library.dbobject.Competition
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R


open class CheckoutFragment : BaseFragment(R.layout.fragment_pay) {
    
    enum class Mode {REGULAR, COMPLIMENTARY, EDIT }

    var data=MemberServicesData
    var shoppingList = data.shoppingList
    var mode = Mode.REGULAR
    private var costItems = 0
    var forceChequeRefund = true

    init {
        isBackable = false
    }

    override fun whenInitialize() {
        tvPageHeader.text = Competition.current.uniqueName + " - Checkout"
        forceChequeRefund = true
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btCancel -> sendSignal(SignalCode.CANCEL)
                btBack -> sendSignal(SignalCode.BACK)
            }
        }
    }

    private fun addItem(label: String, value: Int) {
        val fragmentActivity = activity
        if (fragmentActivity!=null) {
            val view = fragmentActivity.layoutInflater.inflate(R.layout.template_pay_item, null)
            val tvLabel = view.findViewById(R.id.tvLabel) as TextView
            val tvItem = view.findViewById(R.id.tvItem) as TextView
            tvLabel.text = label
            tvItem.text = value.money
            loLines.addView(view)
            if (value != 0) {
                costItems++
            }
        }
    }

    private fun addTotal(value: Int) {
        val fragmentActivity = activity
        if (fragmentActivity!=null) {
            val view = fragmentActivity.layoutInflater.inflate(R.layout.template_pay_item_total, null)
            val tvItem = view.findViewById(R.id.tvItem) as TextView
            val tvLabel = view.findViewById(R.id.tvLabel) as TextView
            tvLabel.text = if (value < 0) "Refund required" else "Total to Pay"
            tvItem.text = value.money
            loLines.addView(view)
        }
    }

    private fun populateComplimentary() {
        btCancel.visibility = View.VISIBLE
        tvInfo.text = "Confirm transaction as follows:"
        addItem("${data.freeCredits} x Late Entry (Complimentary)", 0)
        addMenuButton(loMenu, "Staff", signalCode = SignalCode.STAFF_CREDIT)
        addMenuButton(loMenu, "Competitor", signalCode = SignalCode.DISCRETIONARY_CREDIT)
    }

    private fun populateEdit() {
        btCancel.visibility = View.GONE
        tvInfo.text = "Update transaction as follows:"
        val editLedger = data._editLedger
        if (editLedger != null) {
            val quantity = editLedger.quantity
            val description = editLedger.description
            val amount = editLedger.amount
            if (quantity != 0) {
                addItem("${quantity.absolute} x $description", amount)
            } else {
                addItem(description, amount)
            }

            when {
                editLedger.getInt("cash") > 0 -> {
                    tvHint.text = "Change from CASH to:"
                    addMenuButton(loMenu, "CHEQUE", signalCode = SignalCode.CHANGE_TO_CHEQUE)
                }
                editLedger.getInt("cheque") > 0 -> {
                    tvHint.text = "Change from CHEQUE to:"
                    addMenuButton(loMenu, "CASH", signalCode = SignalCode.CHANGE_TO_CASH)
                }
            }
        }
    }

    private fun populatePaid() {
        btCancel.visibility = View.VISIBLE
        tvInfo.text = "Confirm transaction as follows:"
        costItems = 0
        val chequesToday = data.chequesToday
        val cashToday = data.cashToday
        val rebate = -shoppingList.totalAmount
        if (shoppingList.totalAmount > 0) {
            if (shoppingList.hasReturnCheque) {
                addMenuButton(loMenu, "Returned cheque for ${chequesToday.money} and collected ${shoppingList.totalAmount.money} in CASH", signalCode = SignalCode.PAID_CASH, buttonWidth = 400)
                addMenuButton(loMenu, "Returned cheque for ${chequesToday.money} and collected ${shoppingList.totalAmount.money} by CHEQUE", signalCode = SignalCode.PAID_CHEQUE, buttonWidth = 400)
                if (cashToday >= rebate + chequesToday) {
                    addMenuButton(loMenu, "Do all cash refund instead", signalCode = SignalCode.CANCEL_RETURN_CHEQUE, buttonWidth = 400)
                }
            } else {
                val totalCredits = shoppingList.totalCredits
                val totalCreditsAvailable = shoppingList.totalCreditsBought  + data.getCreditsAvailable(Competition.current.id)
                if (totalCredits > 0 && totalCredits <= totalCreditsAvailable) {
                    addMenuButton(loMenu, "Use CREDITS ($totalCredits of $totalCreditsAvailable)", signalCode = SignalCode.PAID_CREDITS, buttonWidth = 400)
                }
                addMenuButton(loMenu, "Received ${shoppingList.totalAmount.money} in CASH", signalCode = SignalCode.PAID_CASH, buttonWidth = 400)
                addMenuButton(loMenu, "Received ${shoppingList.totalAmount.money} by CHEQUE", signalCode = SignalCode.PAID_CHEQUE, buttonWidth = 400)
            }
        } else if (shoppingList.totalAmount < 0) {
            if (shoppingList.hasReturnCheque) {
                addMenuButton(loMenu, "Returned CHEQUE for ${chequesToday.money} and refunded ${shoppingList.totalAmount.absolute.money} in CASH", signalCode = SignalCode.PAID_CASH, buttonWidth = 400)
                if (cashToday >= rebate + chequesToday) {
                    addMenuButton(loMenu, "Do CASH all refund instead", signalCode = SignalCode.CANCEL_RETURN_CHEQUE, buttonWidth = 400)
                }
            } else {
                if (chequesToday == 0) {
                    addMenuButton(loMenu, "Refunded ${shoppingList.totalAmount.absolute.money} in CASH", signalCode = SignalCode.PAID_CASH, buttonWidth = 400)
                } else if (cashToday < rebate || forceChequeRefund) {
                    shoppingList.returnCheque(chequesToday)
                    populate()
                    return
                } else {
                    addMenuButton(loMenu, "Refunded ${shoppingList.totalAmount.absolute.money} in CASH", signalCode = SignalCode.PAID_CASH, buttonWidth = 400)
                    addMenuButton(loMenu, "Return cheque instead", SignalCode.RETURN_CHEQUE, chequesToday, buttonWidth = 400)
                }
            }
        } else {
            if (shoppingList.hasReturnCheque) {
                addMenuButton(loMenu, "Returned CHEQUE for ${chequesToday.money}", signalCode = SignalCode.PAID_CASH, buttonWidth = 400)
            } else {
                addMenuButton(loMenu, "Confirm transaction", signalCode = SignalCode.PAID_CASH, buttonWidth = 400)
            }
        }
        addMenuButton(loMenu, "Continue shopping", signalCode = SignalCode.CONTINUE_SHOPPING, buttonWidth = 400)

        shoppingList.withFinancialItems { item ->
            if (item.quantity != 1) {
                addItem("${item.quantity.absolute} x ${item.description}", item.amount)
            } else {
                addItem("${item.description}", item.amount)
            }
        }
        if (costItems > 1) {
            addTotal(shoppingList.totalAmount)
        }
    }

    private fun populate() {
        tvHint.text = ""
        loLines.removeAllViews()
        loMenu.removeAllViews()
        when (mode) {
            Mode.REGULAR -> populatePaid()
            Mode.COMPLIMENTARY -> populateComplimentary()
            Mode.EDIT -> populateEdit()
        }
        AndroidUtils.goneIf(tvHint.text.isEmpty(), tvHint)
    }

    override fun whenSignal(signal: Signal) {

        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                tvAccountCode.text = data.account.code
                populate()
                signal.consumed()
            }
            SignalCode.PAID_CREDITS -> {
                data.postTransaction(PAYMENT_CREDITS)
                sendSignal(SignalCode.MEMBER_MENU)
                signal.consumed()
            }
            SignalCode.PAID_CASH -> {
                data.postTransaction(PAYMENT_CASH)
                sendSignal(SignalCode.MEMBER_MENU)
                signal.consumed()
            }
            SignalCode.PAID_CHEQUE -> {
                data.postTransaction(PAYMENT_CHEQUE)
                sendSignal(SignalCode.MEMBER_MENU)
                signal.consumed()
            }
            SignalCode.PAID_CHEQUE_IN_POST -> {
                data.postTransaction(PAYMENT_IN_POST)
                sendSignal(SignalCode.MEMBER_MENU)
                signal.consumed()
            }
            SignalCode.STAFF_CREDIT -> {
                data.postComplimentary(LATE_ENTRY_STAFF)
                sendSignal(SignalCode.MEMBER_MENU)
            }
            SignalCode.DISCRETIONARY_CREDIT -> {
                data.postComplimentary(LATE_ENTRY_DISCRETIONARY)
                sendSignal(SignalCode.MEMBER_MENU)
            }
            SignalCode.CONTINUE_SHOPPING -> {
                sendSignal(SignalCode.MEMBER_MENU)
            }
            SignalCode.CANCEL -> {
                val exitMemberServices = shoppingList.cancel()
                shoppingList.clear()
                sendSignal(if (exitMemberServices) SignalCode.RESET else SignalCode.MEMBER_MENU)
            }
            SignalCode.CHANGE_TO_CASH -> {
                val editLedger = data._editLedger
                if (editLedger != null) {
                    editLedger.cash = editLedger.amount
                    editLedger.cheque = 0
                    editLedger.promised = 0
                    editLedger.post()
                }
                sendSignal(SignalCode.BACK)
            }
            SignalCode.CHANGE_TO_CHEQUE -> {
                val editLedger = data._editLedger
                if (editLedger != null) {
                    editLedger.cash = 0
                    editLedger.cheque = editLedger.amount
                    editLedger.promised = 0
                    editLedger.post()
                }
                sendSignal(SignalCode.BACK)
            }
            SignalCode.CHANGE_TO_IN_POST -> {
                val editLedger = data._editLedger
                if (editLedger != null) {
                    editLedger.cash = 0
                    editLedger.cheque = 0
                    editLedger.promised = editLedger.amount
                    editLedger.post()
                }
                sendSignal(SignalCode.BACK)
            }
            SignalCode.RETURN_CHEQUE -> {
                val chequesToday = signal._payload as Int?
                if (chequesToday != null) {
                    shoppingList.returnCheque(chequesToday)
                }
                populate()
                signal.consumed()
            }
            SignalCode.CANCEL_RETURN_CHEQUE -> {
                forceChequeRefund = false
                shoppingList.cancelReturnCheque()
                populate()
                signal.consumed()
            }
            SignalCode.BACK -> {
                when (mode) {
                    Mode.REGULAR -> {
                        when (shoppingList.lastItemLedgerType) {
                            ITEM_LATE_ENTRY_PAID -> sendSignal(SignalCode.BUY_LATE_ENTRY)
                            else -> sendSignal(SignalCode.MEMBER_MENU)
                        }
                        signal.consumed()
                    }
                    Mode.COMPLIMENTARY -> {
                        sendSignal(SignalCode.COMPLIMENTARY_ENTRY)
                        signal.consumed()
                    }
                    Mode.EDIT -> {
                        /* just go back */
                    }
                }
            }
            else -> {
                doNothing()
            }

        }
    }


}

