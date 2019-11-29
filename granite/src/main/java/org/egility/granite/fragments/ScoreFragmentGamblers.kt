/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import kotlinx.android.synthetic.main.fragment_score_gamblers.*
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.AndroidUtils.goneIf
import org.egility.android.views.QuickButton
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import org.egility.library.dbobject.Competition

class ScoreFragmentGamblers : BaseFragment(R.layout.fragment_score_gamblers) {

    val isFab = Competition.isFab
    
    override fun whenInitialize() {
        with(ringPartyData) {
            tvHint.text = if (editMode) "Edit Score" else ""
            btEnterTime.text = if (editMode) "Edit Time" else "Enter Time"
            goneIf(!editMode, btOK)
            goneIf(editMode || reRunMode, btBack)
            goneIf(!(editMode || reRunMode), btCancel)
            val obstacles=agilityClass.obstaclePoints.length
            goneIf(obstacles<20, btTwenty)
            goneIf(obstacles<19, btNineteen)
            goneIf(obstacles<18, btEighteen)

/*
            btBonus.text="B${agilityClass.template.gambleBonus}"
            btGamble1.text="G${agilityClass.template.gamble1}"
            btGamble2.text="G${agilityClass.template.gamble2}"
            goneIf(agilityClass.template.gambleBonus==0 || agilityClass.gambleBonusObstacles.isNotEmpty(), btBonus)

 */

            btGamble1.text="G${agilityClass.gamble1}"
            btGamble2.text="G${agilityClass.gamble2}"
            btBonus.text="B${agilityClass.gambleBonusScore}"
            goneIf(agilityClass.gambleBonusScore==0 || agilityClass.gambleBonusObstacles.isNotEmpty(), btBonus)


            goneIf(agilityClass.gamble1==0, btGamble1)
            goneIf(agilityClass.gamble2==0, btGamble2)
            goneIf(!isFab , btScoreClose)
            goneIf(isFab , btGamble0)
        }

    }

    private fun checkButtons() {

        fun checkButton(button: QuickButton, letter: Char) {
            val data = ringPartyData.entry.getGamblersButtonData(letter)
            button.isEnabled = data.enabled || ringPartyData.entry.isClosing
            val content = SpannableString(data.caption)
            if (data.underlined && !ringPartyData.entry.isClosing) {
                content.setSpan(UnderlineSpan(), 0, content.length, 0)
            }
            button.text = content
        }

        if (hasView) {
            checkButton(btOne, OBSTACLE_1)
            checkButton(btTwo, OBSTACLE_2)
            checkButton(btThree, OBSTACLE_3)
            checkButton(btFour, OBSTACLE_4)
            checkButton(btFive, OBSTACLE_5)
            checkButton(btSix, OBSTACLE_6)
            checkButton(btSeven, OBSTACLE_7)
            checkButton(btEight, OBSTACLE_8)
            checkButton(btNine, OBSTACLE_9)
            checkButton(btTen, OBSTACLE_10)
            checkButton(btEleven, OBSTACLE_11)
            checkButton(btTwelve, OBSTACLE_12)
            checkButton(btThirteen, OBSTACLE_13)
            checkButton(btFourteen, OBSTACLE_14)
            checkButton(btFifteen, OBSTACLE_15)
            checkButton(btSixteen, OBSTACLE_16)
            checkButton(btSeventeen, OBSTACLE_17)
            checkButton(btEighteen, OBSTACLE_18)
            checkButton(btNineteen, OBSTACLE_19)
            checkButton(btTwenty, OBSTACLE_20)

            with(ringPartyData.entry) {
                btScoreDelete.isEnabled = isScoreDeletable
                btElimination.isEnabled = isEliminationAllowed
                btGamble0.isEnabled = isGambleAllowed && (!isFab || isClosing) 
                btGamble1.isEnabled = isGambleAllowed && (!isFab || isClosing)
                btGamble2.isEnabled = isGambleAllowed && (!isFab || isClosing)
                btBonus.isEnabled=isBonusAllowed
                btNFC.isEnabled = isNFCAllowed
                AndroidUtils.goneIf(!classAllowsNFC, btNFC)
                btEnterTime.isEnabled = isTimeEntryAllowed || isClosing
                btBack.isEnabled = isBackAllowed
                btOK.isEnabled = isResultConsistent && isModified
                btScoreClose.isEnabled = !isClosing
            }
        }
    }

    override fun whenClick(view: View) {
        when (view) {
            btOne -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_1)
            btTwo -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_2)
            btThree -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_3)
            btFour -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_4)
            btFive -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_5)
            btSix -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_6)
            btSeven -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_7)
            btEight -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_8)
            btNine -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_9)
            btTen -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_10)
            btEleven -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_11)
            btTwelve -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_12)
            btThirteen -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_13)
            btFourteen -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_14)
            btFifteen -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_15)
            btSixteen -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_16)
            btSeventeen -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_17)
            btEighteen -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_18)
            btNineteen -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_19)
            btTwenty -> sendSignal(SignalCode.SCORE_CODE, OBSTACLE_20)

            btBonus -> sendSignal(SignalCode.SCORE_CODE, SCORE_BONUS)

            btScoreClose -> sendSignal(SignalCode.SCORE_CODE, SCORE_CLOSING_SEQUENCE)

            btScoreDelete -> sendSignal(SignalCode.SCORE_CODE, SCORE_DELETE)
            btElimination -> sendSignal(SignalCode.SCORE_CODE, SCORE_ELIMINATE)
            btGamble0 -> sendSignal(SignalCode.SCORE_CODE, SCORE_NO_GAMBLE)
            btGamble1 -> sendSignal(SignalCode.SCORE_CODE, SCORE_GAMBLE_1)
            btGamble2 -> sendSignal(SignalCode.SCORE_CODE, SCORE_GAMBLE_2)

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


