/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.fragment_score.*
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.AndroidUtils.goneIf
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R


class ScoreFragment : BaseFragment(R.layout.fragment_score) {

    override fun whenInitialize() {
        with(ringPartyData) {
            btEnterTime.text = if (editMode) "Edit Time" else "Enter Time"
            goneIf(!editMode, tvHint)
            goneIf(!editMode, btOK)
            goneIf(editMode || reRunMode, btBack)
            goneIf(!(editMode || reRunMode), btCancel)
            if (entry.isRelay) {
                btClear.text="B"
            }
            if (agilityClass.isIfcs) {
                btHandling.text="P"
            }
        }
    }

    private fun checkButtons() {
        if (hasView) {
            with(ringPartyData.entry) {
                btRefusal.isEnabled = isFaultAllowed
                btHandling.isEnabled = isFaultAllowed
                btElimination.isEnabled = isEliminationAllowed
                btFault.isEnabled = isFaultAllowed
                btScoreDelete.isEnabled = isScoreDeletable
                btClear.isEnabled = if (isRelay) isBatonFaultAllowed else isClearAllowed
                btBatonChange.isEnabled= isBatonChangeAllowed
                btNFC.isEnabled = isNFCAllowed
                AndroidUtils.goneIf(!classAllowsNFC, btNFC)
                AndroidUtils.goneIf(!isRelay, btBatonChange)
                btEnterTime.isEnabled = isTimeEntryAllowed
                btBack.isEnabled = isBackAllowed
                btOK.isEnabled = isResultConsistent && isModified
            }
        }
    }

    override fun whenClick(view: View) {
        when (view) {
            btRefusal -> sendSignal(SignalCode.SCORE_CODE, SCORE_REFUSAL)
            btHandling -> sendSignal(SignalCode.SCORE_CODE, if (ringPartyData.agilityClass.isIfcs) SCORE_POLE else SCORE_HANDLING)
            btElimination -> sendSignal(SignalCode.SCORE_CODE, if(ringPartyData.entry.agilityClass.isRelay) SCORE_ELIMINATE_FAULT else SCORE_ELIMINATE)
            btFault -> sendSignal(SignalCode.SCORE_CODE, SCORE_FAULT)
            btScoreDelete -> sendSignal(SignalCode.SCORE_CODE, SCORE_DELETE)
            btClear -> if (ringPartyData.entry.isRelay) sendSignal(SignalCode.SCORE_CODE, SCORE_BATON_FAULT) else sendSignal(SignalCode.SCORE_CODE, SCORE_CLEAR)
            btBatonChange -> sendSignal(SignalCode.SCORE_CODE, SCORE_BATON_CHANGE)
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
            /*
            SignalCode.PAGE_DOWN -> {
                if (ringPartyData.entry.isFaultAllowed) {
                    sendSignal(SignalCode.SCORE_CODE, SCORE_FAULT)
                }
                signal.consumed()
            }
            SignalCode.PAGE_UP -> {
                if (ringPartyData.entry.isFaultAllowed) {
                    sendSignal(SignalCode.SCORE_CODE, SCORE_REFUSAL)
                }
                signal.consumed()
            }
            */
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