/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.view.inputmethod.EditorInfo
import kotlinx.android.synthetic.main.fragment_change_handler_kc.*
import org.egility.android.BaseFragment
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import org.egility.library.dbobject.Team
import org.egility.library.general.*


class ChangeHandlerFragmentKc : BaseFragment(R.layout.fragment_change_handler_kc) {
    
    var entry = ringPartyData.entry

    override fun whenInitialize() {
        tvPageHeader.text = title
        tvDogName.text = entry.dogName
        tvCompetitorName.text = entry.competitorName
        edOtherHandler.setText("")
        edOtherHandler.setOnEditorActionListener { view, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                sendSignal(SignalCode.CHANGE_HANDLER_TO_NAME, edOtherHandler.text.toString())
                true
            }
            false
        }
        loOtherHandler.visibility = View.GONE
        loMenu.columnCount = 1
        val team = Team()
        val selected = LinkedHashMap<String, Int>()
        val current = entry.team.getCompetitorName(1)
        team.select("idDog=${entry.team.idDog} AND idTeam<>${entry.idTeam} AND teamType IN ($TEAM_SINGLE_HANDLER, $TEAM_LINKED_OTHER, $TEAM_NAMED_HANDLER)", "teamType")
        while (team.next()) {
            val name = team.getCompetitorName(1)
            if (name neq current && !selected.containsKey(name)) {
                selected.put(name, team.cursor)
            }
        }
        loMenu.columnCount = if (selected.size > 6) 2 else 1
        val buttonWidth = if (selected.size > 6) 260 else 340

        if (selected.size > 0) {
            loMenu.removeAllViews()
            loMenu.visibility = View.VISIBLE
            for (item in selected) {
                team.cursor = item.value
                addMenuButton(
                    loMenu,
                    team.getCompetitorName(1),
                    SignalCode.CHANGE_HANDLER_TO_ID_TEAM,
                    team.id,
                    team.getCompetitorName(1),
                    buttonWidth = buttonWidth
                )
            }
            addMenuButton(loMenu, "Other", SignalCode.CHANGE_HANDLER_OTHER, buttonWidth = buttonWidth)
        } else {
            loMenu.visibility = View.GONE
            sendSignal(SignalCode.CHANGE_HANDLER_OTHER)
        }
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btBack -> sendSignal(SignalCode.BACK)
            }
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.CHANGE_HANDLER_TO_NAME -> {
                val name = signal._payload as? String
                if (name != null && name.isNotEmpty()) {
                    whenYes(
                        "Confirm",
                        "Are you sure you want to change the handler name from ${entry.competitorName} to ${name.naturalCase}?"
                    ) {
                        val idTeam = Team.getIndividualNamedId(entry.team.idDog, name.naturalCase)
                        entry.changeEntryTeam(idTeam)
                        sendSignal(SignalCode.HANDLER_CHANGED)
                    }
                }
            }
            SignalCode.CHANGE_HANDLER_TO_ID_TEAM -> {
                val idTeam = signal._payload as? Int
                val name = signal._payload2 as? String
                if (idTeam != null && name != null && name.isNotEmpty()) {
                    whenYes(
                        "Confirm",
                        "Are you sure you want to change the handler name from ${entry.competitorName} to ${name.naturalCase}?"
                    ) {
                        entry.changeEntryTeam(idTeam)
                        sendSignal(SignalCode.HANDLER_CHANGED)
                    }
                }
            }
            SignalCode.CHANGE_HANDLER_OTHER -> {
                loMenu.visibility = View.GONE
                loOtherHandler.visibility = View.VISIBLE
                edOtherHandler.requestFocus()
                showKeyboard()
            }
            else -> {
                doNothing()
            }

        }
    }

    private fun doSelect() {
    }

}