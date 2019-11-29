/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.buy_credit.*
import org.egility.library.dbobject.Competition
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils.goneIf
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

open class BuyCreditsFragmentBase : BaseFragment(R.layout.buy_credit) {
    
    var data = MemberServicesData
    var isComplimentary = this is BuyCreditsFragmentFree

    var sign=1
    var shoppingList=data.shoppingList

    init {
        isBackable = false
    }

    override fun whenInitialize() {
        sign=1
        tvPageHeader.text = Competition.current.uniqueName + " - Buy Credits"
        tvAccountCode.text = data.account.code
        tvLateEntries.text = data.getCreditsAvailableText(Competition.current.id)
    }

    override fun whenClick(view: View) {
        when (view) {
            btZero -> {
                if (btZero.text == "0") {
                    sendSignal(SignalCode.DIGIT, 0)
                } else {
                    sendSignal(SignalCode.NEGATE)
                }
            }

            btOne -> sendSignal(SignalCode.DIGIT, 1)
            btTwo -> sendSignal(SignalCode.DIGIT, 2)
            btThree -> sendSignal(SignalCode.DIGIT, 3)
            btFour -> sendSignal(SignalCode.DIGIT, 4)
            btFive -> sendSignal(SignalCode.DIGIT, 5)
            btSix -> sendSignal(SignalCode.DIGIT, 6)
            btSeven -> sendSignal(SignalCode.DIGIT, 7)
            btEight -> sendSignal(SignalCode.DIGIT, 8)
            btNine -> sendSignal(SignalCode.DIGIT, 9)
            btDel -> sendSignal(SignalCode.DELETE)
            btDelAll -> {
                if (btDelAll.text == "clear") {
                    sendSignal(SignalCode.DELETE_ALL)
                } else {
                    sendSignal(SignalCode.TOGGLE_LOCK)
                }
            }

            btBack -> sendSignal(SignalCode.BACK)
            btUsed -> sendSignal(SignalCode.CREDITS_USED, if(isComplimentary) SignalCode.COMPLIMENTARY_ENTRY else SignalCode.BUY_LATE_ENTRY)
            btOK -> sendSignal(SignalCode.OK)
        }
    }

    override fun whenSignal(signal: Signal) {

        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                btOK.isEnabled = data.credits != 0
                btDel.isEnabled = data.credits != 0
                btBack.isEnabled = data.credits == 0
                btDelAll.text = if (data.credits != 0) "clear" else if (data.creditsLock) "auto" else "lock"
                goneIf(!data.creditsLock, tvLock)

                if (data.credits == 0) {
                    if (sign==-1) {
                        btZero.text = "+"
                        tvCredits.text = "-0"
                        tvBalance.text = "-£0.00"
                    } else {
                        btZero.text = "-"
                        tvCredits.text = "0"
                        tvBalance.text = "£0.00"
                    }
                } else {
                    btZero.text = "0"
                    tvCredits.text = Integer.toString(data.credits)
                    tvBalance.text = (data.credits * Competition.current.lateEntryFee).money
                }
                if (isComplimentary) {
                    tvBalance.text = "Free"
                }
                signal.consumed()
            }
            SignalCode.DIGIT -> {
                val digit = signal._payload as? Int
                if (digit != null) {
                    val proposed=(data.credits.absolute * 10 + digit) * sign

                    if (proposed>0 || proposed.absolute<= data.getCreditsAvailable(Competition.current.id)) {

                        data.credits = proposed
                        if (data.creditsLock) {
                            sendSignal(SignalCode.RESET_FRAGMENT)
                        } else {
                            sendSignal(SignalCode.OK)
                        }
                    }
                    signal.consumed()
                }
            }
            SignalCode.DELETE -> {
                data.credits = (data.credits.absolute / 10) * sign
                sendSignal(SignalCode.RESET_FRAGMENT)
                signal.consumed()
            }
            SignalCode.DELETE_ALL -> {
                data.credits = 0
                sendSignal(SignalCode.RESET_FRAGMENT)
                signal.consumed()
            }
            SignalCode.BACK -> {
                if (data.credits != 0) {
                    signal.consumed()
                }
            }
            SignalCode.NEGATE -> {
                sign = -sign
                sendSignal(SignalCode.RESET_FRAGMENT)
                signal.consumed()
            }
            SignalCode.TOGGLE_LOCK -> {
                data.creditsLock = !data.creditsLock
                sendSignal(SignalCode.RESET_FRAGMENT)
                signal.consumed()
            }
            SignalCode.OK -> {
                if (isComplimentary) {
                    data.freeCredits = data.credits
                    sendSignal(SignalCode.CHECKOUT_FREE)
                } else {
                    shoppingList.addLateEntry(LATE_ENTRY_PAID, data.credits)
                    sendSignal(SignalCode.CHECKOUT)
                }
                signal.consumed()
            }
            else -> {
                doNothing()
            }
        }
    }

}


class BuyCreditsFragment : BuyCreditsFragmentBase()
class BuyCreditsFragmentFree : BuyCreditsFragmentBase()


