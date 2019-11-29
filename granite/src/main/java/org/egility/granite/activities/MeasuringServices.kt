/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import org.egility.granite.fragments.*
import org.egility.library.dbobject.Competition
import org.egility.library.general.*

/**
 * Created by mbrickman on 15/10/15.
 */
class MeasuringServices : BaseActivity(R.layout.content_holder) {

    private var data = MeasuringServicesData

    private lateinit var selectHandlerByCode: SelectHandlerByCode
    private lateinit var selectDogByCodeFragment: SelectDogByCodeFragment
    private lateinit var selectDogByNameFragment: SelectDogByNameFragment
    private lateinit var dogMeasure: DogMeasure
    

    init {
        if (!dnr) {
            selectHandlerByCode = SelectHandlerByCode()
            selectHandlerByCode.guideText="Please sign on using your Agility Plaza handler code - 2 initials + 4 digits"
            selectHandlerByCode.confirmText = "Please confirm that you are %name and that the measuring equipment is correctly setup."
            selectDogByCodeFragment = SelectDogByCodeFragment()
            selectDogByNameFragment = SelectDogByNameFragment()
            dogMeasure=DogMeasure()
        }
    }

    override fun whenInitialize() {
        selectDogByCodeFragment.isSecretary = true
    }

    override fun whenSignal(signal: Signal) {

        when (signal.signalCode) {
            SignalCode.RESET -> {
                defaultFragmentContainerId = R.id.loContent
                selectHandlerByCode.title = Competition.current.uniqueName + " - Identify Measurer"
                selectDogByCodeFragment.clear()
                selectDogByCodeFragment.title = Competition.current.uniqueName + " - Select Dog"
                selectDogByCodeFragment.hint = "Enter Code for Dog to be Measured"
                selectDogByCodeFragment.isSelectCompetitor = false
                selectDogByCodeFragment.autoOK = false
                selectDogByCodeFragment.selectedSignal = SignalCode.DOG_SELECTED
                if (data.idCompetitorMeasurer>0) {
                    sendSignal(SignalCode.SELECT_DOG_USING_CODE)
                } else {
                    loadFragment(selectHandlerByCode)                    
                }
                signal.consumed()
            }
            SignalCode.HAVE_HANDLER -> {
                val idCompetitor = signal._payload as Int?
                val measurerName= signal._payload2 as String?
                if (idCompetitor != null) {
                    data.idCompetitorMeasurer = idCompetitor
                    sendSignal(SignalCode.SELECT_DOG_USING_CODE)
                }
            }
            SignalCode.SELECT_DOG_USING_CODE -> {
                selectDogByCodeFragment.clear()
                loadTopFragment(selectDogByCodeFragment)
                signal.consumed()
            }
            SignalCode.DOG_SELECTED -> {
                val dogSelection = signal._payload as DogSelection?
                if (dogSelection != null) {
                    data.selectDog(dogSelection.idDog)
                    loadFragment(dogMeasure)
                    signal.consumed()
                }
            }
            SignalCode.LOOKUP_TEAM_BY_NAME -> {
                loadFragment(selectDogByNameFragment)
                signal.consumed()
            }
            else -> super.whenSignal(signal)
        }

    }

}
