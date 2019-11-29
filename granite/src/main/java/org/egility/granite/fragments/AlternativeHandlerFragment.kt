/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.fragment_alternative_handler.*
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Team
import org.egility.library.general.MemberServicesData
import org.egility.library.general.doNothing
import org.egility.android.BaseFragment
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

/**
 * Created by mbrickman on 12/03/16.
 */

class AlternativeHandlerFragment : BaseFragment(R.layout.fragment_alternative_handler) {

    var data = MemberServicesData
    val team=Team()

    override fun whenInitialize() {
        tvPageHeader.text = Competition.current.uniqueName + " - Member Services"
        tvCompetitor.text = data.memberNames
        tvDog.text = "${data.cleanedPetName} (${data.dogCode})"
    }

    override fun whenClick(view: View) {
        when (view) {
            btCancel -> sendSignal(SignalCode.BACK)
            btOK -> sendSignal(SignalCode.OK)
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                if (data.alternativeHandlerIdTeam>0) {
                    team.find(data.alternativeHandlerIdTeam)
                    edCompetitorName.setText(team.competitorName)
                } else {
                    edCompetitorName.setText("")
                }
            }
            SignalCode.OK -> {
                if (data.alternativeHandlerIdTeam==0) {
                    Team.getIndividualNamedId(data.idDog, edCompetitorName.text.toString())
                } else {
                    team.competitorName=edCompetitorName.text.toString()
                    team.post()
                }
                sendSignal(SignalCode.BACK)
            }
            else -> {
                doNothing()
            }
        }

    }

}