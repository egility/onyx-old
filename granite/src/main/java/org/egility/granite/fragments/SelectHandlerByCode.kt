/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.fragment_select_handler_by_code.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.AndroidUtils.goneIf
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import org.egility.library.dbobject.Competitor
import org.egility.library.dbobject.Dog
import org.egility.library.general.doNothing
import org.egility.library.general.quoted


class SelectHandlerByCode : BaseFragment(R.layout.fragment_select_handler_by_code) {

    enum class Need { UNDEFINED, ALPHA, DIGIT, NOTHING, INVALID }

    var need = Need.ALPHA
    var dog = Dog()
    var idCompetitor = -1
    var newHandlerName = ""

    var guideText=""
    var confirmText=""

    override fun whenInitialize() {
        tvPageHeader.text = title
        tvHandlerName.text = ""
        tvGuide.text = guideText
        AndroidUtils.hideSoftKeyboard(edHandlerCode)
        edHandlerCode.requestFocus()
        edHandlerCode.setText("")
        checkButtons()
        goneIf(guideText.isEmpty(), loGuide)
        goneIf(true, tvConfirm)
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                bt0 -> sendSignal(SignalCode.CHAR, '0')
                bt1 -> sendSignal(SignalCode.CHAR, '1')
                bt2 -> sendSignal(SignalCode.CHAR, '2')
                bt3 -> sendSignal(SignalCode.CHAR, '3')
                bt4 -> sendSignal(SignalCode.CHAR, '4')
                bt5 -> sendSignal(SignalCode.CHAR, '5')
                bt6 -> sendSignal(SignalCode.CHAR, '6')
                bt7 -> sendSignal(SignalCode.CHAR, '7')
                bt8 -> sendSignal(SignalCode.CHAR, '8')
                bt9 -> sendSignal(SignalCode.CHAR, '9')
                btA -> sendSignal(SignalCode.CHAR, 'A')
                btB -> sendSignal(SignalCode.CHAR, 'B')
                btC -> sendSignal(SignalCode.CHAR, 'C')
                btD -> sendSignal(SignalCode.CHAR, 'D')
                btE -> sendSignal(SignalCode.CHAR, 'E')
                btF -> sendSignal(SignalCode.CHAR, 'F')
                btG -> sendSignal(SignalCode.CHAR, 'G')
                btH -> sendSignal(SignalCode.CHAR, 'H')
                btI -> sendSignal(SignalCode.CHAR, 'I')
                btJ -> sendSignal(SignalCode.CHAR, 'J')
                btK -> sendSignal(SignalCode.CHAR, 'K')
                btL -> sendSignal(SignalCode.CHAR, 'L')
                btM -> sendSignal(SignalCode.CHAR, 'M')
                btN -> sendSignal(SignalCode.CHAR, 'N')
                btO -> sendSignal(SignalCode.CHAR, 'O')
                btP -> sendSignal(SignalCode.CHAR, 'P')
                btQ -> sendSignal(SignalCode.CHAR, 'Q')
                btR -> sendSignal(SignalCode.CHAR, 'R')
                btS -> sendSignal(SignalCode.CHAR, 'S')
                btT -> sendSignal(SignalCode.CHAR, 'T')
                btU -> sendSignal(SignalCode.CHAR, 'U')
                btV -> sendSignal(SignalCode.CHAR, 'V')
                btW -> sendSignal(SignalCode.CHAR, 'W')
                btX -> sendSignal(SignalCode.CHAR, 'X')
                btY -> sendSignal(SignalCode.CHAR, 'Y')
                btZ -> sendSignal(SignalCode.CHAR, 'Z')
                btDel -> sendSignal(SignalCode.DELETE)
                btClear -> sendSignal(SignalCode.DELETE_ALL)
                btBack -> sendSignal(SignalCode.BACK)
                btOK -> sendSignal(SignalCode.OK)
            }
        }
    }

    private fun checkButtons() {
        bt0.isEnabled = need == Need.DIGIT
        bt1.isEnabled = need == Need.DIGIT
        bt2.isEnabled = need == Need.DIGIT
        bt3.isEnabled = need == Need.DIGIT
        bt4.isEnabled = need == Need.DIGIT
        bt5.isEnabled = need == Need.DIGIT
        bt6.isEnabled = need == Need.DIGIT
        bt7.isEnabled = need == Need.DIGIT
        bt8.isEnabled = need == Need.DIGIT
        bt9.isEnabled = need == Need.DIGIT

        btA.isEnabled = need == Need.ALPHA
        btB.isEnabled = need == Need.ALPHA
        btC.isEnabled = need == Need.ALPHA
        btD.isEnabled = need == Need.ALPHA
        btE.isEnabled = need == Need.ALPHA
        btF.isEnabled = need == Need.ALPHA
        btG.isEnabled = need == Need.ALPHA
        btH.isEnabled = need == Need.ALPHA
        btI.isEnabled = need == Need.ALPHA
        btJ.isEnabled = need == Need.ALPHA
        btK.isEnabled = need == Need.ALPHA
        btL.isEnabled = need == Need.ALPHA
        btM.isEnabled = need == Need.ALPHA
        btN.isEnabled = need == Need.ALPHA
        btO.isEnabled = need == Need.ALPHA
        btP.isEnabled = need == Need.ALPHA
        btQ.isEnabled = need == Need.ALPHA
        btR.isEnabled = need == Need.ALPHA
        btS.isEnabled = need == Need.ALPHA
        btT.isEnabled = need == Need.ALPHA
        btU.isEnabled = need == Need.ALPHA
        btV.isEnabled = need == Need.ALPHA
        btW.isEnabled = need == Need.ALPHA
        btX.isEnabled = need == Need.ALPHA
        btY.isEnabled = need == Need.ALPHA
        btZ.isEnabled = need == Need.ALPHA


        btDel.isEnabled = edHandlerCode.text.toString().isNotEmpty()
        goneIf(idCompetitor <= 0, btOK)
        goneIf(edHandlerCode.text.toString().isEmpty(), btClear)
        goneIf(need!=Need.NOTHING, btOK)
        btOK.isEnabled = idCompetitor > 0
        tvHint.text = when (need) {
            Need.NOTHING -> "Press 'Confirm' button to complete"
            Need.INVALID -> "Press 'del' or 'Clear' and correct"
            else -> "Enter code"
        }
        if (confirmText.isNotEmpty()) {
            val confirm = need==Need.NOTHING
            AndroidUtils.goneIf(confirm, loTop)
            AndroidUtils.goneIf(confirm, loKeyboard)
            AndroidUtils.goneIf(!confirm, tvConfirm)
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {

            }
            SignalCode.CHAR -> {
                val char = signal._payload as Char?
                if (char != null) {
                    val text = edHandlerCode.text.toString() + char
                    edHandlerCode.setText(text)
                    sendSignal(SignalCode.HANDLER_CODE_UPDATED, text, queued = true)
                    signal.consumed()
                }
            }
            SignalCode.DELETE -> {
                var text = edHandlerCode.text.toString()
                if (!text.isEmpty()) {
                    text = text.substring(0, text.length - 1)
                    edHandlerCode.setText(text)
                    sendSignal(SignalCode.HANDLER_CODE_UPDATED, text, queued = true)
                }
                signal.consumed()
            }
            SignalCode.DELETE_ALL -> {
                var text = edHandlerCode.text.toString()
                if (!text.isEmpty()) {
                    edHandlerCode.setText("")
                    sendSignal(SignalCode.HANDLER_CODE_UPDATED, "", queued = true)
                }
                signal.consumed()
            }
            SignalCode.HANDLER_CODE_UPDATED -> {
                val text = signal._payload as String?
                if (text != null) {
                    when (text.length) {
                        in 0..1 -> need = Need.ALPHA
                        in 2..5 -> need = Need.DIGIT
                        else -> need = Need.NOTHING
                    }
                    if (text.length == 6) {
                        idCompetitor = -1
                        newHandlerName = "*** Invalid Code ***"
                        Competitor().where("competitorCode=${text.quoted}") {
                            idCompetitor = id
                            newHandlerName = fullName
                            if (confirmText.isNotEmpty()) {
                                tvConfirm.text = confirmText.replace("%name", newHandlerName)
                            }
                        }
                        tvHandlerName.text = newHandlerName
                        if (idCompetitor <= 0) need = Need.INVALID
                    } else {
                        tvHandlerName.text = ""
                    }
                    checkButtons()
                }
            }

            SignalCode.OK -> {
                if (idCompetitor > 0 && newHandlerName.isNotEmpty()) {
                    sendSignal(SignalCode.HAVE_HANDLER, idCompetitor, newHandlerName)
                }
            }
            else -> {
                doNothing()
            }

        }
    }

}