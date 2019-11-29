/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.text.InputFilter
import android.view.MotionEvent
import android.view.View
import kotlinx.android.synthetic.main.fragment_change_handler_uka.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.AndroidUtils.goneIf
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import org.egility.library.dbobject.Account
import org.egility.library.dbobject.Competitor
import org.egility.library.dbobject.Entry
import org.egility.library.dbobject.Team
import org.egility.library.general.*


class ChangeHandlerFragmentUka : BaseFragment(R.layout.fragment_change_handler_uka), View.OnTouchListener {

    enum class Need { UNDEFINED, ALPHA, DIGIT, NOTHING, INVALID }

    var entry = ringPartyData.entry

    var need = Need.UNDEFINED
    var idCompetitor = -1
    var newHandlerName = ""
    var handlers = ""
    val account = Account()

    override fun whenInitialize() {
        tvPageHeader.text = title
        tvDogName.text = entry.dogName
        tvCompetitorName.text = entry.competitorName
        tvOtherHandlerName.text = ""
        edOtherHandler.setText("")
        edOtherHandler.filters = edOtherHandler.filters + InputFilter.AllCaps()
        hideKeyboardPanel()
        checkButtons()
        loOtherHandler.visibility = View.GONE

       // val idAccount = if (entry.idAccount > 0) entry.idAccount else entry.team.dog.idAccount
        account.find(entry.idAccount)
        handlers = account.allHandlersList

        if (handlers.split(",").size > 1) {
            loMenu.removeAllViews()
            loOptions.visibility = View.VISIBLE

            Competitor().where("idCompetitor IN ($handlers)") {
                if (id != entry.team.idCompetitor) {
                    addMenuButton(
                        loMenu,
                        fullName,
                        SignalCode.CHANGE_HANDLER_TO_ID_COMPETITOR,
                        id,
                        fullName,
                        buttonWidth = 340
                    )
                }
            }
            addMenuButton(loMenu, "Other", SignalCode.CHANGE_HANDLER_OTHER, buttonWidth = 340)
        } else {
            loOptions.visibility = View.GONE
            sendSignal(SignalCode.CHANGE_HANDLER_OTHER)
        }
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


        btDel.isEnabled = edOtherHandler.text.toString().isNotEmpty()
        goneIf(idCompetitor <= 0, btOK)
        goneIf(edOtherHandler.text.toString().isEmpty(), btClear)
        btOK.isEnabled = idCompetitor > 0
        tvHint.text = when (need) {
            Need.NOTHING -> "Press 'Confirm' button to complete"
            Need.INVALID -> "Press 'del' or 'Clear' and correct"
            else -> "Type handler code - initials + 4 digits"
//        btBack.isEnabled = teamCodeText.isEmpty()
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.CHAR -> {
                val char = signal._payload as Char?
                if (char != null) {
                    val text = edOtherHandler.text.toString() + char
                    edOtherHandler.setText(text)
                    sendSignal(SignalCode.HANDLER_CODE_UPDATED, text, queued = true)
                    signal.consumed()
                }
            }
            SignalCode.DELETE -> {
                var text = edOtherHandler.text.toString()
                if (!text.isEmpty()) {
                    text = text.substring(0, text.length - 1)
                    edOtherHandler.setText(text)
                    sendSignal(SignalCode.HANDLER_CODE_UPDATED, text, queued = true)
                }
                signal.consumed()
            }
            SignalCode.DELETE_ALL -> {
                var text = edOtherHandler.text.toString()
                if (!text.isEmpty()) {
                    edOtherHandler.setText("")
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
                        }
                        tvOtherHandlerName.text = newHandlerName
                        if (idCompetitor <= 0) need = Need.INVALID
                    } else {
                        tvOtherHandlerName.text = ""
                    }
                    checkButtons()
                }
            }

            SignalCode.RESET_FRAGMENT -> {
                edOtherHandler.setOnTouchListener(this)
                AndroidUtils.hideSoftKeyboard(edOtherHandler)
            }
            SignalCode.CHANGE_HANDLER_TO_ID_COMPETITOR -> {
                val idCompetitor = signal._payload as? Int
                val name = signal._payload2 as? String
                if (idCompetitor != null && name != null && name.isNotEmpty()) {
                    whenYes(
                        "Confirm",
                        "Are you sure you want to change the handler from '${entry.competitorName}' to '${name.naturalCase}'?"
                    ) {
                        val idTeam = Team.getIndividualId(idCompetitor, entry.team.idDog)
                        entry.changeEntryTeam(idTeam)
                        sendSignal(SignalCode.HANDLER_CHANGED)
                    }
                }
            }
            SignalCode.OK -> {
                if (idCompetitor > 0 && newHandlerName.isNotEmpty()) {
                    if (!handlers.split(",").contains(idCompetitor.toString())) {
                        account.addHandler(idCompetitor)
                    }
                    val idTeam = Team.getIndividualId(idCompetitor, entry.team.idDog)
                    entry.changeEntryTeam(idTeam)
                    sendSignal(SignalCode.HANDLER_CHANGED)
                }
            }
            SignalCode.CHANGE_HANDLER_OTHER -> {
                need = Need.ALPHA
                loOptions.visibility = View.GONE
                loOtherHandler.visibility = View.VISIBLE
                edOtherHandler.requestFocus()
                showKeyboardPanel()
                checkButtons()
            }
            else -> {
                doNothing()
            }

        }
    }

    private fun doSelect() {
    }


    private fun hideKeyboardPanel() {
        loKeyboard.visibility = View.GONE
    }

    private fun showKeyboardPanel() {
        loKeyboard.visibility = View.VISIBLE
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        showKeyboardPanel()
        return false
    }

}