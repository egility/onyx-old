/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.fragment_score_time_out.*
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.AndroidUtils.goneIf
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

class ScoreFragmentTimeOut : BaseFragment(R.layout.fragment_score_time_out) {

    override fun whenInitialize() {
        with(ringPartyData) {
            tvHint.text = if (editMode) "Edit Score" else "Enter Score"
            btEnterTime.text = if (editMode) "Edit Time" else "Enter Time"
            goneIf(!editMode, btOK)
            goneIf(editMode || reRunMode, btBack)
            goneIf(!(editMode || reRunMode), btCancel)
        }
    }

    private fun checkButtons() {
        if (hasView) {
            with(ringPartyData.entry) {
                btScoreDelete.isEnabled = isScoreDeletable
                btElimination.isEnabled = isEliminationAllowed

                AndroidUtils.goneIf(!isNFCAllowed, btNFC)
                btEnterTime.isEnabled = isTimeEntryAllowed
                btBack.isEnabled = isBackAllowed
                btOK.isEnabled = isResultConsistent && isModified
            }
        }
    }

    override fun whenClick(view: View) {
        when (view) {
            btZero -> sendSignal(SignalCode.SCORE_CODE, SCORE_0)
            btOne -> sendSignal(SignalCode.SCORE_CODE, SCORE_1)
            btTwo -> sendSignal(SignalCode.SCORE_CODE, SCORE_2)
            btThree -> sendSignal(SignalCode.SCORE_CODE, SCORE_3)
            btFour -> sendSignal(SignalCode.SCORE_CODE, SCORE_4)
            btFive -> sendSignal(SignalCode.SCORE_CODE, SCORE_5)
            btSix -> sendSignal(SignalCode.SCORE_CODE, SCORE_6)
            btSeven -> sendSignal(SignalCode.SCORE_CODE, SCORE_7)
            btEight -> sendSignal(SignalCode.SCORE_CODE, SCORE_8)
            btNine -> sendSignal(SignalCode.SCORE_CODE, SCORE_9)

            btScoreDelete -> sendSignal(SignalCode.SCORE_CODE, SCORE_DELETE)
            btElimination -> sendSignal(SignalCode.SCORE_CODE, SCORE_ELIMINATE)

            btNFC -> sendSignal(SignalCode.SCORE_CODE, SCORE_NFC)
            btEnterTime -> sendSignal(SignalCode.ENTER_TIME)
            btBack -> sendSignal(SignalCode.BACK)
            btOK -> sendSignal(SignalCode.ACCEPT_EDIT)
            btCancel -> sendSignal(if (ringPartyData.reRunMode) SignalCode.CANCEL_RUN else SignalCode.CANCEL_EDIT)
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                checkButtons()
                signal.consumed()
            }
            SignalCode.SCORE_CODE -> {
                val char = signal._payload as? Char
                if (char != null) {
                    ringPartyData.entry.addScoreCode(char)
                    sendSignal(SignalCode.SCORE_CHANGED)
                    signal.consumed()
                }
            }
            SignalCode.BACK -> {
                if (ringPartyData.editMode) {
                    sendSignal(SignalCode.CANCEL_EDIT)
                } else if (ringPartyData.entry.isBackAllowed) {
                    sendSignal(SignalCode.CANCEL_RUN)
                }
                signal.consumed()
            }
            SignalCode.SCORE_CHANGED -> {
                checkButtons()
            }
            else -> {
                doNothing()
            }
        }
    }
}


