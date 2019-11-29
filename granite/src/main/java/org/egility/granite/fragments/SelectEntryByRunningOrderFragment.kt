/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.select_entry_by_running_order.*
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Height
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.NavigationGroup
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.QuickButton
import org.egility.granite.R

open class SelectEntryByRunningOrderFragmentBase : BaseFragment(R.layout.select_entry_by_running_order) {

    var idAgilityClass = 0
    var btBack: QuickButton? = null
    var idEntry = -1
    var progress = -1
    var paperScrime = this is PaperScimeSelectFragment
    
    private val agilityClass
        get() = ringPartyData.agilityClass

    private var invalid = ""
    private var runningOrderText = ""

    override fun whenResumeFromPause() {
        sendSignal(SignalCode.RESET_FRAGMENT)
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)      
        } else {
            when (view) {
                btZero -> sendSignal(SignalCode.DIGIT, 0)
                btOne -> sendSignal(SignalCode.DIGIT, 1)
                btTwo -> sendSignal(SignalCode.DIGIT, 2)
                btThree -> sendSignal(SignalCode.DIGIT, 3)
                btFour -> sendSignal(SignalCode.DIGIT, 4)
                btFive -> sendSignal(SignalCode.DIGIT, 5)
                btSix -> sendSignal(SignalCode.DIGIT, 6)
                btSeven -> sendSignal(SignalCode.DIGIT, 7)
                btEight -> sendSignal(SignalCode.DIGIT, 8)
                btNine -> sendSignal(SignalCode.DIGIT, 9)
                btDelAll -> sendSignal(SignalCode.DELETE_ALL)
                btDel -> sendSignal(SignalCode.DELETE)
                btOK -> sendSignal(SignalCode.OK)
            }
        }
    }

    private fun checkButtons() {
        btZero.isEnabled = runningOrderText.isNotEmpty()
        btOK?.isEnabled = runningOrderText.isNotEmpty()
        btBack?.isEnabled = runningOrderText.isEmpty()
    }

    private fun checkHeightButtons() {
        for (index in 1..loNavigation.childCount) {
            val button = loNavigation.getChildAt(index) as? QuickButton
            if (button != null) {
                val signal=button.tag as? Signal
                val heightCode=signal?._payload as? String
                AndroidUtils.disableIf(heightCode!=null && heightCode == ringPartyData.jumpHeight.code, button)
            }
        }

    }

    override fun whenSignal(signal: Signal) {

        when (signal.signalCode) {
            SignalCode.DIGIT -> {
                val digit = signal._payload as Int?
                if (digit != null) {
                    runningOrderText = (runningOrderText.toIntDef(0) * 10 + digit).toString()
                    tvRunningOrder.text = runningOrderText
                    checkButtons()
                    checkInvalid()
                    if (runningOrderText.length==5) {
                        sendSignal(SignalCode.OK)
                    }
                }
                signal.consumed()
            }
            SignalCode.DELETE -> {
                if (!runningOrderText.isEmpty()) {
                    runningOrderText = runningOrderText.substring(0, runningOrderText.length - 1)
                    tvRunningOrder.text = runningOrderText
                    checkInvalid()
                    checkButtons()
                }
                signal.consumed()
            }
            SignalCode.DELETE_ALL -> {
                if (!runningOrderText.isEmpty()) {
                    checkInvalid()
                    checkButtons()
                    runningOrderText = ""
                    tvRunningOrder.text = runningOrderText
                }
                signal.consumed()
            }
            SignalCode.BACK -> {
                if (!runningOrderText.isEmpty()) {
                    signal.consumed()
                }
            }
            SignalCode.RESET_FRAGMENT -> {
                runningOrderText = ""
                clear()
                tvPageHeader.text = "Ring ${ringPartyData.ring.number}: ${agilityClass.name}"
                setUpNavigation()
                checkHeightButtons()
                checkButtons()
                tvPageFooter.text = agilityClass.heightProgress()
                tvRunningOrder.text = runningOrderText
                tvHint.text = validHint
                checkInvalid()
                signal.consumed()
            }
            SignalCode.OK -> {
                if (runningOrderText.isNotEmpty()) {
                    val idEntry = if (runningOrderText.length==5 && !paperScrime) 
                        seekCodeEntry(Integer.parseInt(runningOrderText)) 
                    else 
                        seekIdEntry(Integer.parseInt(runningOrderText))
                    if (paperScrime) {
                        if (idEntry > -1) {
                            sendSignal(SignalCode.PAPER_SELECTED, idEntry, progress == PROGRESS_RUN)
                        } else {
                            invalid = runningOrderText
                            checkInvalid()
                        }


                    } else {
                        if (idEntry > -1) {
                            sendSignal(SignalCode.HAVE_ENTRY, idEntry)
                        } else if (idEntry == -2) {
                            tvHint.text = if (runningOrderText.length == 5)
                                "*** Dog $runningOrderText is not entered as ${Height.getHeightJumpName(ringPartyData.jumpHeightCode)} ***"
                            else
                                "*** R/O $runningOrderText is not entered as ${Height.getHeightJumpName(ringPartyData.jumpHeightCode)} ***"
                        } else if (idEntry == -3) {
                            tvHint.text = "*** Dog $runningOrderText is not entered ***"
                        } else {
                            invalid = runningOrderText
                            checkInvalid()
                        }
                    }
                }
                signal.consumed()
            }
            SignalCode.REFRESH -> {
                checkHeightButtons()
                tvPageFooter.text = agilityClass.heightProgress()

            }
            else -> {
                doNothing()
            }
        }
    }

    private val invalidHint="*** Invalid Running Order ***"
    private val validHint=if(paperScrime) "Enter Running Order" else "Enter Running Order/Plaza Code"

    private fun checkInvalid() {
        if (invalid.isNotEmpty() && invalid == runningOrderText && tvHint!=null && tvHint.text != invalidHint) {
            tvHint.text = invalidHint
        } else if (invalid != runningOrderText && tvHint!=null && tvHint.text != validHint) {
            tvHint.text = validHint
            invalid = ""
        }
    }

    fun clear() {
        invalid = ""
        runningOrderText = ""
        checkInvalid()
    }

    private fun seekIdEntry(runningOrder: Int): Int {
        var result=-1
        if (paperScrime) {
            dbQuery("""
            SELECT idEntry, progress FROM entry
            WHERE
                idAgilityClass=${agilityClass.id} AND
                runningOrder=$runningOrder
        """){
                idEntry=getInt("idEntry")
                progress=getInt("progress")
                result = idEntry
            }
        } else {
            dbQuery("""
            SELECT idEntry, jumpHeightCode FROM entry
            WHERE
                idAgilityClass=${agilityClass.id} AND
                runningOrder=$runningOrder
        """){
                idEntry=getInt("idEntry")
                val jumpHeightCode=getString("jumpHeightCode")
                if (jumpHeightCode==ringPartyData.jumpHeightCode) {
                    result = idEntry
                } else if (result==-1) {
                    result=-2
                }
            }
        }
        return result
    }

    private fun seekCodeEntry(dogCode: Int): Int {
        var result=-1
        dbQuery("""
            SELECT idEntry, jumpHeightCode FROM entry JOIN team USING (idTeam) JOIN dog USING (idDog)
            WHERE
                entry.idAgilityClass=${agilityClass.id} AND
                dog.dogCode=$dogCode
        """){
            val idEntry=getInt("idEntry")
            val jumpHeightCode=getString("jumpHeightCode")
            if (jumpHeightCode==ringPartyData.jumpHeightCode) {
                result = idEntry
            } else if (result==-1) {
                result=-2
            }
        }
        if (result==-1) {
            dbQuery("SELECT idDog FROM dog WHERE dogCode=$dogCode"){
                result = -3
            }
        }
        return result
    }


    val classHeights = Height()

    fun setUpNavigation() {
        var selectedHeight = ringPartyData.jumpHeightCode
        var selected = 0

        loNavigation.removeAllViews()
        btBack = addNavigationButton(loNavigation, "Back", SignalCode.BACK)

        if (this is SelectEntryByRunningOrderFragment) {
            if (classHeights.rowCount == 0) {
                classHeights.selectClassHeights(agilityClass.id, true)
            } else {
                classHeights.beforeFirst()
            }

            if (agilityClass.jumpHeightArray.size > 1) {
                val navigationGroup = NavigationGroup()
                for (height in agilityClass.jumpHeightArray) {
                    if (selectedHeight == "") selectedHeight = height.code
                    if (height.code == selectedHeight) selected = navigationGroup.size
                    navigationGroup.add(height.heightCaptionShort, SignalCode.HEIGHT_SELECTED, height.code)
                }
                addNavigationGroup(loNavigation, navigationGroup, selected, if (Competition.isKc) 3 else 0)
            }

            addNavigationButton(loNavigation, "Search", SignalCode.LIST_BY_NAME)
            addNavigationButton(loNavigation, "Review", SignalCode.RUNNERS_LIST)
            addNavigationButton(loNavigation, "PA", SignalCode.VIRTUAL_RADIO)
            if (ringPartyData.jumpHeightCode.isEmpty()) {
                ringPartyData.jumpHeightCode = selectedHeight
            }
        } else if (this is PaperScimeSelectFragment) {
            addNavigationButton(loNavigation, "List Scrimed", SignalCode.SCRIME_RUN)
            addNavigationButton(loNavigation, "List Not Scrimed", SignalCode.SCRIME_NOT_RUN)
        }
    }


}



class SelectEntryByRunningOrderFragment(): SelectEntryByRunningOrderFragmentBase()
class PaperScimeSelectFragment(): SelectEntryByRunningOrderFragmentBase()