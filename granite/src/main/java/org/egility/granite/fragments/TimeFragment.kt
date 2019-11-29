/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.fragment_time.*
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils.goneIf
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R


class TimeFragment : BaseFragment(R.layout.fragment_time) {
    
    var start = 0L

    override fun whenInitialize() {
        with(ringPartyData) {
            tvHint.text = if (editMode) "Edit Time" else "Enter Time"
            //            btBack.text = if (editMode) "Edit Score" else "Back"
            goneIf(editMode, btDone)
            goneIf(!editMode && !reRunTimeMode, btCancel)
            goneIf(reRunTimeMode, btBack)
            goneIf(!editMode, btOK)
            start = now.time

            /*
                        prepareButton(btZero, SignalCode.TIME_CODE, 0)
                        prepareButton(btOne, SignalCode.TIME_CODE, 1)
                        prepareButton(btTwo, SignalCode.TIME_CODE, 2)
                        prepareButton(btThree, SignalCode.TIME_CODE, 3)
                        prepareButton(btFour, SignalCode.TIME_CODE, 4)
                        prepareButton(btFive, SignalCode.TIME_CODE, 5)
                        prepareButton(btSix, SignalCode.TIME_CODE, 6)
                        prepareButton(btSeven, SignalCode.TIME_CODE, 7)
                        prepareButton(btEight, SignalCode.TIME_CODE, 8)
                        prepareButton(btNine, SignalCode.TIME_CODE, 9)
                        prepareButton(btDel, SignalCode.TIME_CODE, TIME_DELETE)
                        prepareButton(btDelAll, SignalCode.TIME_CODE, TIME_RESET)
                        prepareButton(btNoTime, SignalCode.TIME_CODE, TIME_NO_TIME)
            */
        }
    }

    override fun whenClick(view: View) {
        debug("timeFragment", "when Click")
        when (view) {
            btZero -> sendSignal(SignalCode.TIME_CODE, 0)
            btOne -> sendSignal(SignalCode.TIME_CODE, 1)
            btTwo -> sendSignal(SignalCode.TIME_CODE, 2)
            btThree -> sendSignal(SignalCode.TIME_CODE, 3)
            btFour -> sendSignal(SignalCode.TIME_CODE, 4)
            btFive -> sendSignal(SignalCode.TIME_CODE, 5)
            btSix -> sendSignal(SignalCode.TIME_CODE, 6)
            btSeven -> sendSignal(SignalCode.TIME_CODE, 7)
            btEight -> sendSignal(SignalCode.TIME_CODE, 8)
            btNine -> sendSignal(SignalCode.TIME_CODE, 9)
            btDel -> sendSignal(SignalCode.TIME_CODE, TIME_DELETE)
            btDelAll -> sendSignal(SignalCode.TIME_CODE, TIME_RESET)
            btNoTime -> sendSignal(SignalCode.TIME_CODE, TIME_NO_TIME)
            btDone -> sendSignal(SignalCode.RUN_COMPLETE)
            btBack -> sendSignal(SignalCode.BACK)
            btOK -> sendSignal(SignalCode.ACCEPT_EDIT)
            btCancel -> sendSignal(if (ringPartyData.reRunMode) SignalCode.CANCEL_RUN else SignalCode.CANCEL_EDIT)
        }
    }

    fun checkButtons() {
        if (hasView) {
            with(ringPartyData.entry) {
                btOne.isEnabled = isDigitAllowed
                btTwo.isEnabled = isDigitAllowed
                btThree.isEnabled = isDigitAllowed
                btFour.isEnabled = isDigitAllowed
                btFive.isEnabled = isDigitAllowed
                btSix.isEnabled = isDigitAllowed
                btSeven.isEnabled = isDigitAllowed
                btEight.isEnabled = isDigitAllowed
                btNine.isEnabled = isDigitAllowed
                btZero.isEnabled = isDigitAllowed

                btNoTime.isEnabled = isTimerNoTimeAllowed
                btDel.isEnabled = isTimeDeleteAllowed
                btDelAll.isEnabled = isTimeDeleteAllowed
                btDone.isEnabled = isTimeComplete
                btOK.isEnabled = isTimeComplete
                btBack.isEnabled = isTimeConsistent
            }
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                checkButtons()
                signal.consumed()
            }
            SignalCode.TIME_CODE -> {
                ringPartyData.entry.addTimeDigit(signal._payload as Int)
                sendSignal(SignalCode.UPDATE_STATUS)
                signal.consumed()
            }
            SignalCode.BACK -> {
                if ((ringPartyData.editMode || btBack.isEnabled) && !ringPartyData.reRunTimeMode) {
                    sendSignal(SignalCode.ENTER_SCORE)
                }
                signal.consumed()
            }
            SignalCode.UPDATE_STATUS -> {
                checkButtons()
            }
            SignalCode.RUN_COMPLETE -> {
                ringPartyData.entry.scrimeTime = (now.time - start).toInt()
            }
            else -> {
                doNothing()
            }
        }
    }
}