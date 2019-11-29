/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.MotionEvent
import android.view.View
import org.egility.granite.utils.downloadPdf
import kotlinx.android.synthetic.main.fragment_class_menu.*
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Device
import org.egility.library.dbobject.Ring
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.*
import org.egility.granite.R
import java.util.*

/**
 * Created by mbrickman on 08/10/15.
 */
class ClassMenu : BaseFragment(R.layout.fragment_class_menu) {

    var isRingParty: Boolean = false
    var startTimeOnly: Boolean = false
    var canEditRunningOrders: Boolean=false

    enum class Menu {PROGRESS, RING, POSITION }

    var loading = false
    var time = Date(now.time)
    private var _timeThread: TimeThread? = null
    private var menu = Menu.PROGRESS

    private val agilityClass
        get() = ringPartyData.agilityClass
    
    private val isActiveClass: Boolean
        get() {
            ringPartyData.ring.refresh()    
            return ringPartyData.ring.idAgilityClass == agilityClass.id
        }

    override fun whenInitialize() {
        defaultButtonWidth = 350
        tvPageHeader.text = "${agilityClass.name} - Status"

        xbOverLunch.setOnClickListener {
            sendSignal(SignalCode.WALKING_OVER_LUNCH)
        }

        btTimeMinus.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    _timeThread = TimeThread(-1)
                    _timeThread?.start()
                }
                MotionEvent.ACTION_UP -> {
                    val timeThread = _timeThread
                    if (timeThread != null) {
                        if (timeThread.repeats == 0) {
                            sendSignal(SignalCode.ADJUST_TIME, timeThread.sign)
                        }
                        timeThread.buttonUp = true
                    }
                }
            }
            true
        }

        btTimePlus.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    _timeThread = TimeThread(1)
                    _timeThread?.start()
                }
                MotionEvent.ACTION_UP -> {
                    val timeThread = _timeThread
                    if (timeThread != null) {
                        if (timeThread.repeats == 0) {
                            sendSignal(SignalCode.ADJUST_TIME, timeThread.sign)
                        }
                        timeThread.buttonUp = true
                    }
                }
            }
            true
        }

        AndroidUtils.goneIf(Competition.current.isKc, loStartTime)
        AndroidUtils.goneIf(startTimeOnly, tvClassStatus)
        AndroidUtils.goneIf(startTimeOnly, loMenu)
        AndroidUtils.goneIf(startTimeOnly || Device.isPrivileged, tvOptionsLabel)
        if (startTimeOnly) {
            btOK.text = "OK"
            btOK.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)

        } else {
            btOK.text = ""
            btOK.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.back, 0, 0, 0)
        }
    }


    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btOK -> sendSignal(SignalCode.BACK)
            }
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                agilityClass.refresh()
                loadData()
                signal.consumed()
            }
            SignalCode.MOVE_TO_ANOTHER_RING -> {
                loadData(Menu.RING)
                signal.consumed()
            }
            SignalCode.RE_PRINT_RESULTS -> {
                Reports.printResults(agilityClass.id, finalize = true)
                //sendSignal(SignalCode.BACK)
                signal.consumed()
            }
            SignalCode.PRINT_RUNNING_ORDERS -> {
                Reports.printRunningOrders(agilityClass.id)
                //sendSignal(SignalCode.BACK)
                signal.consumed()
            }
            SignalCode.PRINT_CALLING_SHEETS -> {
                Reports.printCallingSheets(idAgilityClass = agilityClass.id)
                //sendSignal(SignalCode.BACK)
                signal.consumed()
            }
            SignalCode.PRINT_SCRIME_SHEETS -> {
                Reports.printScrimeSheets(idAgilityClass = agilityClass.id)
                //sendSignal(SignalCode.BACK)
                signal.consumed()
            }
            SignalCode.RE_PRINT_AWARDS -> {
                Reports.printAwards(agilityClass.id)
                sendSignal(SignalCode.BACK)
                signal.consumed()
            }
            SignalCode.DISPLAY_RESULTS -> {
                doHourglass(activity, "Preparing Report", {
                    val pdfFile = Reports.printResults(agilityClass.id, pdf = true)
                    downloadPdf(pdfFile)
                }, {
                    sendSignal(SignalCode.BACK)
                })
                signal.consumed()
            }
            SignalCode.REOPEN_CLASS -> {
                agilityClass.refresh()
                ringPartyData.ring.agilityClass.refresh()
                if (ringPartyData.ring.agilityClass.progress.oneOf(CLASS_CLOSED_FOR_LUNCH, CLASS_RUNNING)) {
                    Global.services.popUp("Warning", "You may not re-open a class while the current one is running")
                } else if (ringPartyData.ring.agilityClass.progress == CLASS_WALKING) {
                    Global.services.popUp("Warning", "You may not re-open a class while the current one is walking")
                } else {
                    whenYes("Question", "Are you really sure you want to re-open this class?") {
                        val ring = Ring()
                        ring.seek(agilityClass.idCompetition, agilityClass.date, agilityClass.ringNumber)
                        ring.chooseAgilityClass(agilityClass.id, reOpen=true)
                        sendSignal(SignalCode.BACK)
                    }
                }
                signal.consumed()
            }
            SignalCode.FORCE_CLOSE_CLASS -> {
                agilityClass.refresh()
                agilityClass.progress = CLASS_CLOSED
                agilityClass.post()
                agilityClass.updateRing()
                sendSignal(SignalCode.BACK)
                signal.consumed()
            }
            SignalCode.SET_PROGRESS -> {
                val progress = signal._payload as Int?
                if (progress != null) {
                    agilityClass.progress = progress
                    agilityClass.post()
                }
                loadData()
            }
            SignalCode.WALKING_OVER_LUNCH -> {
                agilityClass.walkingOverLunch = xbOverLunch.isChecked
                agilityClass.post()
                updateStatus()
            }
            SignalCode.GROUP_SELECTED -> {
                val group = signal._payload as String?
                if (group != null) {
                    ringPartyData.setRingGroup(group)
                }
                loadData()
            }
            SignalCode.ADJUST_TIME -> {
                val adjustment = signal._payload as Int?
                if (adjustment != null) {
                    adjustTime(adjustment)
                }
            }
            SignalCode.MOVE_TO_RING -> {
                val ringNumber = signal._payload as Int?
                if (ringNumber != null) {
                    agilityClass.moveToRing(ringNumber)
                    menu = Menu.PROGRESS
                    sendSignal(SignalCode.BACK)
                    //loadData()
                }
            }
            SignalCode.BACK -> {
                if (agilityClass.isModified) {
                    agilityClass.post()
                }
                if (menu != Menu.PROGRESS) {
                    sendSignal(SignalCode.RESET_FRAGMENT)
                    signal.consumed()
                }
            }
            else -> {
                doNothing()
            }
        }
    }

    private fun updateStatus() {
        if (ringPartyData.group.isNotEmpty()) {
            tvClassStatus.text = "Ring ${agilityClass.ringNumber} - ${agilityClass.name} (${ringPartyData.group})"
        } else {
            tvClassStatus.text = "Ring ${agilityClass.ringNumber} - ${agilityClass.name}"
        }
        tvProgress.text = "(${agilityClass.progressTextExtended})"
    }

    private fun loadData(menu: Menu = Menu.PROGRESS) {
        loading = true
        val isActive=isActiveClass
        this.menu = menu
        try {
            updateStatus()
            if (agilityClass.startTime != nullDate) {
                time.time = agilityClass.startTime.time
            } else {
                time.time = now.time
            }
            adjustTime(0)

            xbOverLunch.isChecked = agilityClass.walkingOverLunch

            loMenu.removeAllViews()
            loMenu.columnCount = 1

            if (!startTimeOnly) {
                when (menu) {
                    Menu.PROGRESS -> {
                        when (agilityClass.progress) {
                            CLASS_PENDING -> {
                                if (!isRingParty) {
                                    addMenuButton(loMenu, "Move to Another Ring", SignalCode.MOVE_TO_ANOTHER_RING)
                                    addMenuButton(loMenu, "Change Order", SignalCode.CHANGE_CLASS_ORDER)
                                }
                                if (isActive) {
                                    addMenuButton(loMenu, "Change status to Setting Up", SignalCode.SET_PROGRESS, CLASS_PREPARING)
                                }
                            }
                            CLASS_PREPARING -> {
                                if (isActive) {
                                    addMenuButton(loMenu, "Change status to Walking", SignalCode.SET_PROGRESS, CLASS_WALKING)
                                }
                                if (!isRingParty) {
                                    addMenuButton(loMenu, "Move to Another Ring", SignalCode.MOVE_TO_ANOTHER_RING)
                                    addMenuButton(loMenu, "Change Order in Ring", SignalCode.CHANGE_CLASS_ORDER)
                                }
                            }
                            CLASS_WALKING -> {
                                if (isActive) {
                                    addMenuButton(loMenu, "Change status to Setting Up", SignalCode.SET_PROGRESS, CLASS_PREPARING)
                                    addMenuButton(loMenu, "Change status to Running", SignalCode.SET_PROGRESS, CLASS_RUNNING)
                                }
                            }
                            CLASS_CLOSED_FOR_LUNCH -> {
                                if (isActive) {
                                    addMenuButton(loMenu, "Change status to Running", SignalCode.SET_PROGRESS, CLASS_RUNNING)
                                }
                            }
                            CLASS_RUNNING -> {
                                if (isActive) {
                                    addMenuButton(loMenu, "Change status to Walking", SignalCode.SET_PROGRESS, CLASS_WALKING)
                                    addMenuButton(loMenu, "Close Class", SignalCode.CLOSE_CLASS)
                                }
                                if (agilityClass.groupRunningOrder.isNotEmpty()) {
                                    val groups = agilityClass.groupRunningOrder.split(",")
                                    val index = (groups.indexOf(ringPartyData.group) + 1).rem(groups.size)
                                    addMenuButton(loMenu, "Switch to Group ${groups[index]}", SignalCode.GROUP_SELECTED, groups[index])
                                }
                                if (!agilityClass.strictRunningOrder) {
                                    addMenuButton(loMenu, "View Waiting For", SignalCode.VIEW_MISSING, agilityClass.id)
                                }
                                addMenuButton(loMenu, "View Results", SignalCode.VIEW_RESULTS, agilityClass.id)
                            }
                            CLASS_CLOSED -> {
                                addMenuButton(loMenu, "View Results", SignalCode.VIEW_RESULTS, agilityClass.id)
                                if (!isRingParty) {
                                    addMenuButton(loMenu, "Re-Print Results", SignalCode.RE_PRINT_RESULTS, agilityClass.id)
                                    if (Competition.isKc || Competition.isFab) {
                                        addMenuButton(loMenu, "Re-Print Awards", SignalCode.RE_PRINT_AWARDS, agilityClass.id)
                                    }
                                    // addMenuButton(loMenu, "Display Results", SignalCode.DISPLAY_RESULTS, agilityClass.id)
                                    addMenuButton(loMenu, "Re-Open Class", SignalCode.REOPEN_CLASS)
                                }
                            }
                        }
                        if (Device.isPrivileged && agilityClass.isKc) {
                            addMenuButton(loMenu, "Print Calling Sheets", SignalCode.PRINT_CALLING_SHEETS)
                            addMenuButton(loMenu, "Emergency Scrime Sheets", SignalCode.PRINT_SCRIME_SHEETS)
                        }
                        if ((Device.isPrivileged || agilityClass.progress== CLASS_RUNNING) && agilityClass.isKc) {
                            addMenuButton(loMenu, "Scrime (from Paper)", SignalCode.PAPER_SCRIME)
                        }
                        if (canEditRunningOrders && Device.isPrivileged && agilityClass.strictRunningOrder) {
                            addMenuButton(loMenu, "Re-Print Running Orders", SignalCode.PRINT_RUNNING_ORDERS)
                        }
                        if (canEditRunningOrders && Device.isPrivileged && agilityClass.strictRunningOrder && agilityClass.progress < CLASS_WALKING) {
                            addMenuButton(loMenu, "Edit Running Orders", SignalCode.EDIT_RUNNING_ORDERS)
                        }
                        if (Device.isSystemManager && agilityClass.progress!= CLASS_CLOSED) {
                            addMenuButton(loMenu, "Force Closed", SignalCode.FORCE_CLOSE_CLASS)
                        }
                    }
                    Menu.RING -> {
                        val ring = Ring()
                        val where = "ring.idCompetition=${agilityClass.idCompetition} AND ring.date=${agilityClass.date.sqlDate} AND ring.ringNumber<>${agilityClass.ringNumber}"
                        ring.select(where, "ringNumber")
                        var buttonWidth=defaultButtonWidth
                        when (ring.rowCount) {
                            in 0..7 -> {
                                loMenu.columnCount = 1
                            }
                            in 8..14 -> {
                                buttonWidth = 200
                                loMenu.columnCount = 2
                            }
                            else -> {
                                buttonWidth = 150
                                loMenu.columnCount = 3
                            }
                        }

                        while (ring.next()) {
                            addMenuButton(loMenu, "Move to Ring ${ring.number}", SignalCode.MOVE_TO_RING, ring.number, buttonWidth=buttonWidth)
                        }
                    }
                    else -> {
                        doNothing()
                    }
                }


            }

            AndroidUtils.goneIf(Competition.current.isKc || !agilityClass.progress.oneOf(CLASS_PREPARING, CLASS_WALKING) || menu != Menu.PROGRESS, loStartTime)
        } finally {
            loading = false
        }
    }

    fun adjustTime(minutes: Int) {
        time = time.addMinutes(minutes)
        tvTime.text = time.timeText
        agilityClass.startTime = today.at(time.hourOfDay(), time.minutesOfHour())
        agilityClass.post()
        updateStatus()
    }

    private open inner class TimeThread(val sign: Int) : Thread() {
        var repeats = 0
        var buttonUp = false

        override fun run() {
            try {
                while (!buttonUp) {
                    if (repeats > 0) {
                        sendSignal(SignalCode.ADJUST_TIME, sign * 5)
                    }
                    Thread.sleep(500)
                    repeats++
                }
            } catch (e: Throwable) {
                // ignore
            }

        }

    }

}


