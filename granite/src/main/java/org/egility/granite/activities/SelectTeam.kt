/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import android.content.Intent
import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import org.egility.granite.fragments.*
import org.egility.library.dbobject.Team

open class SelectTeam : BaseActivity(R.layout.content_holder) {

    private lateinit var selectDogByCodeFragment: SelectDogByCodeFragment
    private lateinit var selectDogByNameFragment: SelectDogByNameFragment

    init {
        if (!dnr) {
            selectDogByCodeFragment = SelectDogByCodeFragment()
            selectDogByNameFragment = SelectDogByNameFragment()
        }
    }

    var title = ""
    var hint = ""
    var autoOK = true

    override fun whenInitialize() {
        title = intent.getStringExtra("title")
        hint = intent.getStringExtra("hint")
        autoOK = intent.getBooleanExtra("autoOK", true)
        updateTitles()
    }

    private fun updateTitles() {
        selectDogByCodeFragment.title = title
        selectDogByCodeFragment.hint = hint
        selectDogByCodeFragment.autoOK = autoOK
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET -> {
                defaultFragmentContainerId = R.id.loContent
                sendSignal(SignalCode.LOOKUP_TEAM)
                signal.consumed()
            }
            SignalCode.LOOKUP_TEAM -> {
                selectDogByCodeFragment.clear()
                loadFragment(selectDogByCodeFragment)
                signal.consumed()
            }
            SignalCode.LOOKUP_TEAM_BY_NAME -> {
                loadFragment(selectDogByNameFragment)
                signal.consumed()
            }
            SignalCode.DOG_SELECTED -> {
                val dogSelection = signal._payload as DogSelection?
                if (dogSelection != null) {
                    val result = Intent()
                    result.putExtra("idTeam", Team.getIndividualId(dogSelection.idCompetitor, dogSelection.idDog))
                    result.putExtra("idCompetitor", dogSelection.idCompetitor)
                    result.putExtra("idDog", dogSelection.idDog)
                    result.putExtra("petName", dogSelection.petName)
                    result.putExtra("ukaHeightCodePerformance", dogSelection.ukaHeightCodePerformance)
                    setResult(RESULT_OK, result)
                    finish()
                    signal.consumed()
                }
            }
            SignalCode.BACK -> {
                val result = Intent()
                setResult(RESULT_CANCELED, result)
                finish()
                signal.consumed()
            }
            else -> super.whenSignal(signal)
        }

    }

}
