/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.fragment_radio.*
import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Height
import org.egility.library.dbobject.Ring
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import java.util.*

/**
 * Created by mbrickman on 08/10/15.
 */
class RadioFragment : BaseFragment(R.layout.fragment_radio) {

    var ringPartyMode = false
    var exitWhenDone = false

    var ringNumbersDate = nullDate
    val ringNumbers: ArrayList<Int> = ArrayList<Int>()
        get() {
            if (ringNumbersDate != today) {
                field.clear()
                dbQuery("""
                    SELECT DISTINCT ringNumber FROM agilityClass
                    WHERE ringNumber>0 AND idCompetition=${Competition.current.id} AND classDate=${today.sqlDate}
                    ORDER BY ringNumber
                """) {
                    val ringNumber = getInt("ringNumber")
                    field.add(ringNumber)
                }
                ringNumbersDate = today
            }
            return field
        }


    override fun whenInitialize() {
        if (ringPartyMode) {
            sendSignal(SignalCode.RING_SELECTED, ringPartyData.ring.number)
        } else {
            sendSignal(SignalCode.SELECT_RING)
        }
        AndroidUtils.goneIf(ringPartyMode, btLock)
        btLock.text = if (exitWhenDone) "Lock" else "Unlock"
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btBack -> sendSignal(SignalCode.BACK)
                btCancel -> sendSignal(SignalCode.CANCEL)
                btLock -> sendSignal(SignalCode.RADIO_LOCK)
            }
        }
    }

    var ring = Ring()
    val agilityClass = ring.agilityClass
    var heightStats = HashMap<String, AgilityClass.HeightStats>()
    var totalRunners = 0
    var totalHeights = 0
    var maxRunningOrder = 0

    val needHeight: Boolean
        get() = totalHeights > 1 && totalRunners > 20

    val steps = ArrayList<Step>()

    class Step(val steps: ArrayList<Step>, val signalCode: SignalCode, radioTemplate: RadioTemplate = RadioTemplate.UNDEFINED) {
        var name: String = ""
        val previous = steps.lastOrNull()
        val message: RadioMessage = if (previous != null) previous.message.clone(radioTemplate) else RadioMessage()

        init {
            steps.add(this)
        }
    }


    init {
        ring.join { agilityClass }
    }

    fun setRing(step: Step, ringNumber: Int) {
        ring.select("ring.idCompetition=${Competition.current.id} AND ring.date=${today.sqlDate} AND ring.ringNumber=$ringNumber").first()

        step.message.idCompetition = Competition.current.id
        step.message.ringNumber = ringNumber
        step.message.idAgilityClass = ring.idAgilityClass

        heightStats = agilityClass.getHeightStats()
        totalHeights = heightStats.size
        totalRunners = 0
        for (height in heightStats) {
            totalRunners += height.value.entered
            if (height.value.maxRunningOrder > maxRunningOrder) maxRunningOrder = height.value.maxRunningOrder
        }

        tvRing.text = "Ring ${ring.number}"
        tvClass.text = "Class ${agilityClass.name}"
        loMessage.visibility = View.VISIBLE
        tvPageFooter.text = agilityClass._heightProgress(heightStats)

    }

    override fun whenSignal(signal: Signal) {
        if (signal.signalCode == SignalCode.BACK) {
            if (steps.size > 1) {
                steps.remove(steps.last())
                val step = steps.last()
                nextStep(step, step.name)
                signal.consumed()
            }
        } else if (signal.signalCode == SignalCode.CANCEL) {
            if (steps.size <= 1) {
                sendSignal(SignalCode.BACK)
            } else {
                steps.clear()
                if (ringPartyMode) {
                    sendSignal(SignalCode.RING_SELECTED, ringPartyData.ring.number)
                } else {
                    sendSignal(SignalCode.SELECT_RING)
                }
                signal.consumed()
            }
        } else if (signal.signalCode == SignalCode.OK) {
            val radioMessage = steps.last().message
            radioMessage.save()
            steps.clear()
            sendSignal(if (exitWhenDone) SignalCode.BACK else SignalCode.SELECT_RING)
            signal.consumed()
        } else {
            when (signal.signalCode) {
                SignalCode.SELECT_RING -> {
                    val step = Step(steps, signal.signalCode)
                    nextStep(step, "ring")
                    signal.consumed()
                }
                SignalCode.RING_SELECTED -> {
                    val step = Step(steps, signal.signalCode)
                    val ringNumber = signal._payload as Int
                    setRing(step, ringNumber)
                    nextStep(step, "category")
                    signal.consumed()
                }
                SignalCode.PA_CATAGORY_SELECTED -> {
                    val step = Step(steps, signal.signalCode)
                    val category = signal._payload as String
                    when (category) {
                        "calling" -> nextStep(step, if (needHeight) "calling_height" else "calling")
                        "walking" -> nextStep(step, "walking")
                        "closing" -> nextStep(step, if (totalHeights > 1) "closing_height" else "closing")
                        "lunch" -> nextStep(step, "lunch")
                    }
                    signal.consumed()
                }
                SignalCode.HEIGHT_SELECTED -> {
                    val step = Step(steps, signal.signalCode)
                    step.message.heightCode = signal._payload as String
                    step.message.heightText = signal._payload2 as String
                    step.message.finalHeight = agilityClass.heightRunningOrder.split(",").last().eq(step.message.heightCode)

                    nextStep(step, "calling")
                    signal.consumed()
                }
                SignalCode.CLOSING_HEIGHT_SELECTED -> {
                    val step = Step(steps, signal.signalCode)
                    step.message.heightCode = signal._payload as String
                    step.message.heightText = signal._payload2 as String
                    step.message.finalHeight = agilityClass.heightRunningOrder.split(",").last().eq(step.message.heightCode)
                    nextStep(step, "closing")
                    signal.consumed()
                }
                SignalCode.PA_WALKING -> {
                    val radioTemplate = signal._payload as RadioTemplate
                    val step = Step(steps, signal.signalCode, radioTemplate)
                    nextStep(step, "starting")
                    signal.consumed()
                }
                SignalCode.PA_LUNCH -> {
                    val radioTemplate = signal._payload as RadioTemplate
                    val step = Step(steps, signal.signalCode, radioTemplate)
                    when (radioTemplate) {
                        RadioTemplate.LUNCH_BETWEEN -> nextStep(step, "lunch_starting_at")
                        RadioTemplate.CLOSED_FOR_LUNCH -> nextStep(step, "resuming")
                        RadioTemplate.NOT_BREAKING -> nextStep(step, "confirm")
                    }                    
                    signal.consumed()
                }
                SignalCode.PA_CALLING_TO -> {
                    val radioTemplate = signal._payload as RadioTemplate
                    val step = Step(steps, signal.signalCode, radioTemplate)
                    nextStep(step, "calling_to")
                    signal.consumed()
                }
                SignalCode.PA_CONFIRM -> {
                    val radioTemplate = signal._payload as (RadioTemplate?) ?: RadioTemplate.UNDEFINED
                    val step = Step(steps, signal.signalCode, radioTemplate)
                    when (radioTemplate) {
                        RadioTemplate.UNDEFINED -> doNothing()
                        RadioTemplate.WALKING_SHORTLY -> doNothing()
                        RadioTemplate.WALKING_NOW -> doNothing()
                        RadioTemplate.WALKING_OVER_LUNCH -> doNothing()
                        RadioTemplate.CALLING_FIRST -> {
                            val dogs = signal._payload2 as Int
                            step.message.dogs = dogs
                            val heightCode = step.message.heightCode
                            val minRunningOrder = if (heightCode.isNotEmpty()) (heightStats[heightCode]?.minRunningOrder
                                    ?: 1) else 1
                            step.message.callingTo = minRunningOrder + dogs - 1
                        }
                        RadioTemplate.CALLING_END, RadioTemplate.CALLING_REMAINING, RadioTemplate.CALLING_FINAL, RadioTemplate.CALLING_ALL -> {
                            val heightCode = step.message.heightCode
                            step.message.callingTo = -1
                        }
                        RadioTemplate.CALLING_FIRST -> {
                            val dogs = signal._payload2 as Int
                            step.message.dogs = dogs
                            val heightCode = step.message.heightCode
                            val minRunningOrder = if (heightCode.isNotEmpty()) (heightStats[heightCode]?.minRunningOrder
                                    ?: 1) else 1
                            step.message.callingTo = minRunningOrder + dogs - 1
                        }
                        RadioTemplate.CALLING_TO -> doNothing()
                        RadioTemplate.CLOSED_FOR_LUNCH -> doNothing()
                        RadioTemplate.CLOSING_SHORTLY -> {
                            val minutes = signal._payload2 as Int
                            step.message.inMinutes = minutes
                        }
                    }
                    nextStep(step, "confirm")
                    signal.consumed()
                }

                SignalCode.PA_LUNCH_HAVE_MINUTES -> {
                    val inMinutes = signal._payload as Int
                    val step = Step(steps, signal.signalCode)
                    step.message.inMinutes = inMinutes
                    step.message.atTime = now.addMinutes(inMinutes)
                    nextStep(step, "resuming")
                    signal.consumed()
                }
                SignalCode.PA_HAVE_MINUTES -> {
                    val inMinutes = signal._payload as Int
                    val step = Step(steps, signal.signalCode)
                    step.message.inMinutes = inMinutes
                    step.message.atTime = now.addMinutes(inMinutes)
                    nextStep(step, "confirm")
                    signal.consumed()
                }
                SignalCode.PA_HAVE_AT_TIME -> {
                    val atTime = signal._payload as Date
                    val step = Step(steps, signal.signalCode)
                    step.message.atTime = atTime
                    nextStep(step, "confirm")
                    signal.consumed()
                }
                SignalCode.PA_HAVE_LUNCH_TIME -> {
                    val atTime = signal._payload as Date
                    val step = Step(steps, signal.signalCode)
                    step.message.atTime = atTime
                    nextStep(step, "resuming")
                    signal.consumed()
                }
                SignalCode.PA_HAVE_RESUME_TIME -> {
                    val atTime = signal._payload as Date
                    val step = Step(steps, signal.signalCode)
                    step.message.resumeTime = atTime
                    nextStep(step, "confirm")
                    signal.consumed()
                }
                SignalCode.PA_HAVE_CALL_TO -> {
                    val callTo = signal._payload as Int
                    val step = Step(steps, signal.signalCode)
                    if (callTo == -1) {
                        step.message.radioTemplate = RadioTemplate.CALLING_END
                    } else {
                        step.message.callingTo = callTo
                    }
                    nextStep(step, "confirm")
                    signal.consumed()
                }
                SignalCode.RADIO_LOCK -> {
                    exitWhenDone = !exitWhenDone
                    btLock.text = if (exitWhenDone) "Lock" else "Unlock"
                }
                SignalCode.PA_STARTING_AT -> {
                    val step = Step(steps, signal.signalCode)
                    nextStep(step, "starting_at")
                    signal.consumed()
                }
                SignalCode.PA_RESUMING_AT -> {
                    val step = Step(steps, signal.signalCode)
                    nextStep(step, "resuming_at")
                    signal.consumed()
                }
                SignalCode.PA_LUNCH_STARTING_AT -> {
                    val step = Step(steps, signal.signalCode)
                    nextStep(step, "lunch_starting_at")
                    signal.consumed()
                }
                else -> {
                    doNothing()
                }
            }
        }
    }

    fun nextStep(step: Step, stepName: String) {
        debug("VirtualPa", "Option = $stepName")
        step.name = stepName
        loOptions.removeAllViews()
        loOptions.columnCount = 1
        when (stepName) {
            "ring" -> {
                var buttonWidth = defaultButtonWidth
                when (ringNumbers.size) {
                    in 0..7 -> {
                        loOptions.columnCount = 1
                    }
                    in 8..14 -> {
                        loOptions.columnCount = 2
                        buttonWidth = 200
                    }
                    else -> {
                        loOptions.columnCount = 3
                        buttonWidth = 150
                    }
                }

                for (ringNumber in ringNumbers) {
                    addMenuButton(loOptions, "Ring $ringNumber", SignalCode.RING_SELECTED, ringNumber, buttonWidth = buttonWidth)
                }
            }
            "category" -> {
                if (agilityClass.progress< CLASS_CLOSED_FOR_LUNCH) {
                    addMenuButton(loOptions, "Walking", SignalCode.PA_CATAGORY_SELECTED, "walking")
                }
                addMenuButton(loOptions, "Calling", SignalCode.PA_CATAGORY_SELECTED, "calling")
                if (agilityClass.progress == CLASS_RUNNING) {
                    addMenuButton(loOptions, "Closing", SignalCode.PA_CATAGORY_SELECTED, "closing")
                }
                addMenuButton(loOptions, "Lunch", SignalCode.PA_CATAGORY_SELECTED, "lunch")
                //addMenuButton(loOptions, "Help needed", SignalCode.PA_CATAGORY_SELECTED, "help")
                if (!ringPartyMode) {
                    //addMenuButton(loOptions, "Presenting", SignalCode.PA_CATAGORY_SELECTED, "presenting")
                }
            }
            "walking" -> {
                addMenuButton(loOptions, "Walking Shortly", SignalCode.PA_WALKING, RadioTemplate.WALKING_SHORTLY)
                addMenuButton(loOptions, "Now Walking", SignalCode.PA_WALKING, RadioTemplate.WALKING_NOW)
                addMenuButton(loOptions, "Walking Over Lunch", SignalCode.PA_WALKING, RadioTemplate.WALKING_OVER_LUNCH)
            }
            "lunch" -> {
                addMenuButton(loOptions, "Will be Lunching...", SignalCode.PA_LUNCH, RadioTemplate.LUNCH_BETWEEN, buttonWidth = 300)
                if (agilityClass.progress == CLASS_RUNNING || agilityClass.progress == CLASS_CLOSED_FOR_LUNCH) {
                    addMenuButton(loOptions, "Closed for Lunch...", SignalCode.PA_LUNCH, RadioTemplate.CLOSED_FOR_LUNCH, buttonWidth = 300)
                }
                addMenuButton(loOptions, "Not Breaking for Lunch", SignalCode.PA_LUNCH, RadioTemplate.NOT_BREAKING, buttonWidth = 300)
            }
            "calling" -> {
                val heightCode = step.message.heightCode
                val maxRunners = if (heightCode.isEmpty()) totalRunners else heightStats[heightCode]?.totalEntered ?: 0
                addMenuButton(loOptions, "All to the Ring", SignalCode.PA_CONFIRM, RadioTemplate.CALLING_ALL, buttonWidth = 300)
                if (maxRunners > 20) {
                    addMenuButton(loOptions, "First 20 to the Ring", SignalCode.PA_CONFIRM, RadioTemplate.CALLING_FIRST, 20, buttonWidth = 300)
                    addMenuButton(loOptions, "Calling to ...", SignalCode.PA_CALLING_TO, RadioTemplate.CALLING_TO, buttonWidth = 300)
                    addMenuButton(loOptions, "Calling to End", SignalCode.PA_CONFIRM, RadioTemplate.CALLING_END, buttonWidth = 300)
                }
                addMenuButton(loOptions, "All Remaining", SignalCode.PA_CONFIRM, RadioTemplate.CALLING_REMAINING, buttonWidth = 300)
                addMenuButton(loOptions, "Final Call", SignalCode.PA_CONFIRM, RadioTemplate.CALLING_FINAL, buttonWidth = 300)
            }
            "calling_height" -> {
                val lho = agilityClass.hasLowHeight
                for (heightCode in agilityClass.heightRunningOrder.split(",")) {
                    var text = if (lho) Height.getHeightJumpName(heightCode) else Height.getHeightName(heightCode)
                    if (text == "FH") text = "Full Height"
                    if (text == "LHO") text = "Lower Height"
                    addMenuButton(loOptions, text, SignalCode.HEIGHT_SELECTED, heightCode, text)
                }
            }
            "closing_height" -> {
                val lho = agilityClass.hasLowHeight
                val heights = agilityClass.heightRunningOrder.split(",").dropLast(1)
                for (heightCode in heights) {
                    var text = if (lho) Height.getHeightJumpName(heightCode) else Height.getHeightName(heightCode)
                    if (text == "FH") text = "Full Height"
                    if (text == "LHO") text = "Lower Height"
                    addMenuButton(loOptions, text, SignalCode.CLOSING_HEIGHT_SELECTED, heightCode, text)
                }
                addMenuButton(loOptions, "Class", SignalCode.CLOSING_HEIGHT_SELECTED, "", "Class")
            }
            "closing" -> {
                addMenuButton(loOptions, "Closing in 2 Mins", SignalCode.PA_CONFIRM, RadioTemplate.CLOSING_SHORTLY, 2)
                addMenuButton(loOptions, "Closing in 5 Mins", SignalCode.PA_CONFIRM, RadioTemplate.CLOSING_SHORTLY, 5)
                if (step.message.heightCode.isNotEmpty()) {
                    addMenuButton(loOptions, "Is Closed", SignalCode.PA_CONFIRM, RadioTemplate.CLOSED)
                }
            }
            "calling_to" -> {
                val heightCode = step.message.heightCode
                val max = if (heightCode.isEmpty()) maxRunningOrder else heightStats[heightCode]?.maxRunningOrder
                        ?: maxRunningOrder
                val min = if (heightCode.isEmpty()) 1 else heightStats[heightCode]?.minRunningOrder ?: 1
                val increment = if (max - min > 180) 20 else 10
                var callTo = ((min + increment / 2) / increment * increment) + increment
                loOptions.columnCount = 3
                var done = false
                do {
                    if (callTo >= max) {
                        addMenuButton(loOptions, "End", SignalCode.PA_HAVE_CALL_TO, -1, buttonWidth = 150)
                        done = true
                    } else {
                        addMenuButton(loOptions, "$callTo", SignalCode.PA_HAVE_CALL_TO, callTo, buttonWidth = 150)
                    }
                    callTo += increment
                } while (!done)
            }
            "starting" -> {
                if (agilityClass.startTime != nullDate) {
                    addMenuButton(loOptions, "Starting at ${agilityClass.startTime.timeText}", SignalCode.PA_HAVE_AT_TIME, agilityClass.startTime)
                }
                addMenuButton(loOptions, "Starting at ...", SignalCode.PA_STARTING_AT, "")
                if (!step.message.radioTemplate.code.oneOf(RadioTemplate.WALKING_OVER_LUNCH.code, RadioTemplate.CLOSED_FOR_LUNCH.code)) {
                    addMenuButton(loOptions, "Starting in 5 Mins", SignalCode.PA_HAVE_MINUTES, 5)
                    addMenuButton(loOptions, "Starting in 10 Mins", SignalCode.PA_HAVE_MINUTES, 10)
                    addMenuButton(loOptions, "Starting in 15 Mins", SignalCode.PA_HAVE_MINUTES, 15)
                }
                addMenuButton(loOptions, "No Start Time", SignalCode.PA_CONFIRM)
            }
            "lunch_starting" -> {
                if (agilityClass.ring.lunchStart != nullDate) {
                    addMenuButton(loOptions, "At ${agilityClass.ring.lunchStart.timeText}", SignalCode.PA_HAVE_LUNCH_TIME, agilityClass.ring.lunchStart)
                }
                addMenuButton(loOptions, "At ...", SignalCode.PA_LUNCH_STARTING_AT, "")
                addMenuButton(loOptions, "In 5 Mins", SignalCode.PA_LUNCH_HAVE_MINUTES, 5)
                addMenuButton(loOptions, "In 10 Mins", SignalCode.PA_LUNCH_HAVE_MINUTES, 10)
                addMenuButton(loOptions, "In 15 Mins", SignalCode.PA_LUNCH_HAVE_MINUTES, 15)
            }
            "resuming" -> {
                if (agilityClass.ring.lunchEnd != nullDate) {
                    addMenuButton(loOptions, "Resuming at ${agilityClass.ring.lunchEnd.timeText}", SignalCode.PA_HAVE_AT_TIME, agilityClass.ring.lunchEnd)
                }
                addMenuButton(loOptions, "Resuming at ...", SignalCode.PA_RESUMING_AT, "")
                addMenuButton(loOptions, "No Resume Time", SignalCode.PA_CONFIRM)
            }
            "starting_at" -> {
                val baseTime = now
                val first = baseTime.addMinutes(5 - baseTime.minutesOfHour().rem(5))
                val buttonWidth = 150
                loOptions.columnCount = 3
                for (i in 0..17) {
                    val time = first.addMinutes(i * 5)
                    addMenuButton(loOptions, "${time.timeText}", SignalCode.PA_HAVE_AT_TIME, time, time, buttonWidth = buttonWidth)
                }

            }
            "lunch_starting_at" -> {
                val baseTime = now
                val first = baseTime.addMinutes(5 - baseTime.minutesOfHour().rem(5))
                val buttonWidth = 150
                loOptions.columnCount = 3
                for (i in 0..17) {
                    val time = first.addMinutes(i * 5)
                    addMenuButton(loOptions, "at ${time.timeText}", SignalCode.PA_HAVE_LUNCH_TIME, time, time, buttonWidth = buttonWidth)
                }

            }
            "resuming_at" -> {
                val baseTime = if (step.message.atTime.isNotEmpty()) step.message.atTime else now
                val first = baseTime.addMinutes(15 - baseTime.minutesOfHour().rem(5))
                val buttonWidth = 150
                loOptions.columnCount = 3
                for (i in 0..17) {
                    val time = first.addMinutes(i * 5)
                    addMenuButton(loOptions, "${time.timeText}", SignalCode.PA_HAVE_RESUME_TIME, time, time, buttonWidth = buttonWidth)
                }
            }
            "confirm" -> {
                addMenuButton(loOptions, "Confirm", SignalCode.OK, buttonWidth = 300)

            }
        }

        AndroidUtils.goneIf(steps.size == 1, loMessage)
        if (steps.size == 1) tvPageFooter.text = ""
        tvMessage.text = step.message.text
        AndroidUtils.goneIfNoText(tvMessage)

    }


}


