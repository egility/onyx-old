/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.fragment_close_class.*
import org.egility.library.api.Api
import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Entry
import org.egility.library.dbobject.Radio
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.tools.doBackground
import org.egility.granite.R


class CloseClassFragment : BaseFragment(R.layout.fragment_close_class) {

    private var entry = Entry("idEntry", "idAgilityClass", "idTeam", "teamMember", "jumpHeightCode", "clearRoundOnly", "progress")
    private var okMessage = ""

    private val agilityClass
        get() = ringPartyData.agilityClass

    override fun whenInitialize() {
    }

    override fun whenClick(view: View) {
        when (view) {
            btBack -> sendSignal(SignalCode.BACK)
            btClose -> sendSignal(SignalCode.CHECK_CLOSE_CLASS)
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                okMessage = ""
                tvPageHeader.text = agilityClass.name + " - Close Class"
                tvClassStatus.text = "Ring ${agilityClass.ringNumber} - ${agilityClass.name}"
                loWait.visibility = View.GONE
                tvWarning.text = ""
                loContent.visibility = View.VISIBLE
                entry.selectStillToRun(agilityClass.id)
                if (entry.rowCount == 0) {
                    loNotRunYet.visibility = View.GONE
                    if (AgilityClass.getRunnerCount(agilityClass.id) == 0) {
                        tvWarning.text = "NO DOGS HAVE RUN YET!"
                        btClose.text = "Close class anyway"
                        okMessage = "Are you really sure you want to close a class in which no dogs have run?"
                    } else {
                        btClose.text = "Close class & print results"
                    }
                } else {
                    loNotRunYet.visibility = View.VISIBLE
                    val lines = StringBuilder()
                    var count = 0
                    while (entry.next() && count < 10) {
                        var entryText = entry.teamDescription
                        entryText += " (${entry.jumpHeightText})"
                        entryText += " - ${entry.getStatusText(false)}"
                        lines.lineAppend(entryText)
                        count++
                    }
                    if (count < entry.rowCount) {
                        lines.lineAppend("plus ${entry.rowCount - count} others...")
                    }
                    tvRunners.text = lines.toString()
                    tvWarning.text = "NOT ALL DOGS HAVE RUN YET!"
                    btClose.text = "Close class anyway"
                    okMessage = "Are you really sure you want to close a class with ${if (entry.rowCount == 1) "1 dog" else "${entry.rowCount} dogs"} left to run?"
                }
                AndroidUtils.goneIfNoText(tvWarning)
                signal.consumed()
            }
            SignalCode.CHECK_CLOSE_CLASS -> {
                if (okMessage == "") {
                    sendSignal(SignalCode.DO_CLOSE_CLASS)
                } else {
                    whenYes("Question", okMessage) { sendSignal(SignalCode.DO_CLOSE_CLASS) }
                }
            }
            SignalCode.DO_CLOSE_CLASS -> {
                btBack.isEnabled = false
                loContent.visibility = View.GONE
                loWait.visibility = View.VISIBLE
                doBackground(
                        {
                            Api.closeClass(agilityClass.id)
                        },
                        {
                            val radio = Radio()
                            radio.append()
                            radio.idCompetition = agilityClass.idCompetition
                            radio.ringNumber = agilityClass.ringNumber
                            radio.idAgilityClass = agilityClass.id
                            radio.messageTemplate = RadioTemplate.CLOSED.code
                            radio.fullText = "Is now closed"
                            radio.post()

                            btBack.isEnabled = true
                            sendSignal(SignalCode.EXIT)
                        }
                )
                signal.consumed()
            }
            SignalCode.BACK -> {
                if (!btBack.isEnabled) {
                    signal.consumed()
                }
            }
            else -> {
                doNothing()
            }
        }
    }


}