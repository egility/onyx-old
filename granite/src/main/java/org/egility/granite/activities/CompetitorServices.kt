/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import org.egility.library.general.CompetitorServicesData
import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import org.egility.granite.fragments.*

/**
 * Created by mbrickman on 15/10/15.
 */
class CompetitorServices : BaseActivity(R.layout.content_holder) {
    private val servicesData = CompetitorServicesData
    private lateinit var dogMenu: DogMenu
    private lateinit var reviewClasses: ReviewClasses
    private lateinit var selectDogByCodeFragment: SelectDogByCodeFragment
    private lateinit var selectDogByNameFragment: SelectDogByNameFragment

    init {
        if (!dnr) {
            selectDogByCodeFragment = SelectDogByCodeFragment()
            selectDogByCodeFragment.isSecretary = true
            dogMenu = DogMenu()
            reviewClasses = ReviewClasses()
            selectDogByNameFragment = SelectDogByNameFragment()
            selectDogByNameFragment.competitorModeAllowed = true
        }
    }

    override fun whenInitialize() {
    }

    override fun whenSignal(signal: Signal) {

        when (signal.signalCode) {
            SignalCode.RESET -> {
                defaultFragmentContainerId = R.id.loContent
                selectDogByCodeFragment.clear()
                loadTopFragment(selectDogByCodeFragment)
                signal.consumed()
            }
            SignalCode.SELECT_MEMBER_USING_CODE -> {
                selectDogByCodeFragment.clear()
                loadTopFragment(selectDogByCodeFragment)
                signal.consumed()
            }
            SignalCode.MEMBER_MENU -> {
                signal.consumed()
            }
            SignalCode.DOG_MENU -> {
                dogMenu.top()
                loadFragment(dogMenu)
                signal.consumed()
            }
            SignalCode.MEMBER_SELECTED -> {
                val idCompetitor = signal._payload as Int?
                if (idCompetitor != null) {
                    signal.consumed()
                }
            }
            SignalCode.DOG_SELECTED -> {
                val selection = signal._payload as DogSelection?
                if (selection != null) {
                    servicesData.selectDog(selection.idDog)
                    dogMenu.top()
                    loadFragment(dogMenu)
                    signal.consumed()
                }
            }
            SignalCode.LOOKUP_TEAM_BY_NAME -> {
                loadFragment(selectDogByNameFragment)
                signal.consumed()
            }
            SignalCode.REVIEW_CLASSES -> {
                loadFragment(reviewClasses)
                signal.consumed()
            }
            else -> super.whenSignal(signal)
        }

    }

}
