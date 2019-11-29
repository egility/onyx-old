/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import android.content.Intent
import org.egility.granite.fragments.*
import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Entry
import org.egility.library.general.ringPartyData
import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import org.egility.library.dbobject.ScrimeList
import org.egility.library.general.Global

class ClassStatus : BaseActivity(R.layout.content_holder) {

    private lateinit var classMenu: ClassMenu
    private lateinit var resultsFragment: ResultsFragment
    private lateinit var closeClassListFragment: CloseClassFragment
    private lateinit var waitingForFragment: WaitingForListFragment
    private lateinit var paperScimeSelectFragment : PaperScimeSelectFragment
    private lateinit var paperScrimeListFragment : PaperScrimeListFragment
    
    val agilityClass = ringPartyData.agilityClass

    init {
        if (!dnr) {
            classMenu = ClassMenu()
            classMenu.isRingParty = true
            resultsFragment = ResultsFragment()
            closeClassListFragment = CloseClassFragment()
            waitingForFragment = WaitingForListFragment()
            paperScimeSelectFragment = PaperScimeSelectFragment()
            paperScrimeListFragment = PaperScrimeListFragment()
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET -> {
                defaultFragmentContainerId = R.id.loContent
                loadFragment(classMenu)
                signal.consumed()
            }
            SignalCode.VIEW_RESULTS -> {
                loadFragment(resultsFragment)
                signal.consumed()
            }
            SignalCode.VIEW_MISSING -> {
                loadFragment(waitingForFragment)
                signal.consumed()
            }
            SignalCode.CLOSE_CLASS -> {
                loadFragment(closeClassListFragment)
                signal.consumed()
            }
            SignalCode.BACK -> {
                ringPartyData.syncRing()
                super.whenSignal(signal)
            }
            SignalCode.EXIT -> {
                finish()
            }
            SignalCode.ENTRY_SELECTED -> {
                val scrime = Intent(this, Scrime::class.java)
                scrime.putExtra("idEntry", ringPartyData.entry.id)
                startActivity(scrime)
            }
            SignalCode.PAPER_SCRIME -> {
                agilityClass.refresh()
                if (!agilityClass.hasCourseTime()) {
                    Global.services.msgYesNo(
                        "Warning",
                        "You have not entered course times for this class. Would you like to do it now?"
                    ) { isYes ->
                        if (isYes) {
                            doActivity(EnterCourseTimes::class.java)
                        } else {
                            Global.services.popUp(
                                "Information",
                                "Try later when the course times have been entered"
                            )
                        }
                    }
                } else {
                    loadFragment(paperScimeSelectFragment)
                }
            }
            SignalCode.PAPER_SELECTED -> {
                val idEntry=signal._payload as Int?
                val hasRun=signal._payload2 as Boolean?
                if (idEntry!=null && hasRun!=null) {
                    val scrimePaper = Intent(this, ScrimePaper::class.java)
                    scrimePaper.putExtra("hasRun", hasRun)
                    scrimePaper.putExtra("idEntry", idEntry)
                    if (isActiveFragment(paperScrimeListFragment)) {
                        back()
                    }
                    startActivity(scrimePaper)
                }
            }
            SignalCode.SCRIME_NOT_RUN -> {
                paperScrimeListFragment.selector = ScrimeList.NOT_RUN
                loadFragment(paperScrimeListFragment)
            }
            SignalCode.SCRIME_RUN -> {
                paperScrimeListFragment.selector = ScrimeList.RUN
                loadFragment(paperScrimeListFragment)
            }

            else -> {
                super.whenSignal(signal)
            }
        }
    }

}
