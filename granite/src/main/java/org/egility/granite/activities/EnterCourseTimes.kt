/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import android.text.Editable
import android.view.KeyEvent.*
import android.view.View
import android.widget.EditText
import android.widget.TextView
import kotlinx.android.synthetic.main.enter_course_times.*
import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Competition
import org.egility.library.general.Global
import org.egility.library.general.ringPartyData
import org.egility.android.BaseActivity
import org.egility.android.tools.*
import org.egility.android.tools.AndroidUtils.goneIf
import org.egility.granite.R


class EnterCourseTimes : BaseActivity(R.layout.enter_course_times), AndroidOnTextChange {

    private lateinit var agilityClasses: AgilityClass

    init {
        if (!dnr) {
            agilityClasses = ringPartyData.ring.agilityClasses
        }
    }

    override fun whenInitialize() {
        AndroidUtils.hideSoftKeyboard(edDistance)
        AndroidUtils.hideSoftKeyboard(edLarge)
        AndroidUtils.hideSoftKeyboard(edSmall)
        AndroidUtils.hideSoftKeyboard(edGambleTimeLarge)
        AndroidUtils.hideSoftKeyboard(edGambleTimeSmall)
        AndroidUtils.hideSoftKeyboard(edDistanceSimple)
        AndroidUtils.hideSoftKeyboard(edCourseTimeSimple)
        AndroidUtils.hideSoftKeyboard(edBatonFaults)

        AndroidUtils.hideSoftKeyboard(edGamblers7)
        AndroidUtils.hideSoftKeyboard(edGamblers5)
        AndroidUtils.hideSoftKeyboard(edGamblers4)
        AndroidUtils.hideSoftKeyboard(edGamblers3)
        AndroidUtils.hideSoftKeyboard(edGamblers2)
        AndroidUtils.hideSoftKeyboard(edGamblers1)

        edDistance.addTextChangedListener(AndroidTextWatcher(edDistance, this))
        edLarge.addTextChangedListener(AndroidTextWatcher(edLarge, this))
        edSmall.addTextChangedListener(AndroidTextWatcher(edSmall, this))
        edGambleTimeLarge.addTextChangedListener(AndroidTextWatcher(edGambleTimeLarge, this))
        edGambleTimeSmall.addTextChangedListener(AndroidTextWatcher(edGambleTimeSmall, this))

        edGamblers7.addTextChangedListener(AndroidTextWatcher(edGamblers7, this))
        edGamblers5.addTextChangedListener(AndroidTextWatcher(edGamblers5, this))
        edGamblers4.addTextChangedListener(AndroidTextWatcher(edGamblers4, this))
        edGamblers3.addTextChangedListener(AndroidTextWatcher(edGamblers3, this))
        edGamblers2.addTextChangedListener(AndroidTextWatcher(edGamblers2, this))
        edGamblers1.addTextChangedListener(AndroidTextWatcher(edGamblers1, this))
        
        goneIf(!Competition.isUkaStyle, tvGamblersCrib)

        goneIf(!Competition.isFab, tvGamblers7)
        goneIf(!Competition.isFab, edGamblers7)
        goneIf(Competition.isFab, tvGamblers4)
        goneIf(Competition.isFab, edGamblers4)
        goneIf(Competition.isFab, tvGamblers2)
        goneIf(Competition.isFab, edGamblers2)

        /*
        loGamblePoints.columnCount = if (Competition.isFab) 4 else 6
        loGamblePoints.rowCount = 2
        */

        tvPageHeader.text = "Ring " + ringPartyData.ring.number + " - Setup Courses"
        tvHint.text = ""
    }

    enum class Column {GAMBLE_TIME_LARGE, GAMBLE_TIME_SMALL, COURSE_TIME, COURSE_TIME_SMALL, COURSE_LENGTH, BATON_FAULTS, GAMBLERS_7, GAMBLERS_5, GAMBLERS_4, GAMBLERS_3, GAMBLERS_2, GAMBLERS_1 }


    private fun setColumn(column: Column, edit: EditText, factor: Int, add: Int): Int {
        var value = 0
        if (edit.text.toString().isNotEmpty()) {
            value = Integer.parseInt(edit.text.toString()) * factor + add
        }
        when (column) {
            Column.GAMBLE_TIME_LARGE -> agilityClasses.gambleTimeLarge = value
            Column.GAMBLE_TIME_SMALL -> agilityClasses.gambleTimeSmall = value
            Column.COURSE_TIME -> {
                agilityClasses.courseTime = value
                if (Competition.isKc) {
                    for (index in 0..agilityClasses.subClassCount-1) {
                        agilityClasses.setSubClassCourseTime(index, value)
                    }
                }
            }
            Column.COURSE_TIME_SMALL -> agilityClasses.courseTimeSmall = value
            Column.COURSE_LENGTH -> agilityClasses.courseLength = value
            Column.BATON_FAULTS -> agilityClasses.batonFaults = value

            Column.GAMBLERS_7 -> agilityClasses.obstacles7Point = value
            Column.GAMBLERS_5 -> agilityClasses.obstacles5Point = value
            Column.GAMBLERS_4 -> agilityClasses.obstacles4Point = value
            Column.GAMBLERS_3 -> agilityClasses.obstacles3Point = value
            Column.GAMBLERS_2 -> agilityClasses.obstacles2Point = value
            Column.GAMBLERS_1 -> agilityClasses.obstacles1Point = value

        }
        return value
    }

    fun doPost(): Boolean {
        if (agilityClasses.isOnRow) {
            if (agilityClasses.isKc) {
                setColumn(Column.COURSE_LENGTH, edDistanceSimple, 1, 0)
                setColumn(Column.COURSE_TIME, edCourseTimeSimple, 1000, 0)
                setColumn(Column.BATON_FAULTS, edBatonFaults, 1, 0)
            } else if (agilityClasses.isGamblers) {
                setColumn(Column.GAMBLE_TIME_LARGE, edGambleTimeLarge, 1000, 0)
                setColumn(Column.GAMBLE_TIME_SMALL, edGambleTimeSmall, 1000, 0)
                setColumn(Column.COURSE_TIME, edGambleTimeLarge, 1000, agilityClasses.openingTime)
                setColumn(Column.COURSE_TIME_SMALL, edGambleTimeSmall, 1000, agilityClasses.openingTime)

                setColumn(Column.GAMBLERS_7, edGamblers7, 1, 0)
                setColumn(Column.GAMBLERS_5, edGamblers5, 1, 0)
                setColumn(Column.GAMBLERS_4, edGamblers4, 1, 0)
                setColumn(Column.GAMBLERS_3, edGamblers3, 1, 0)
                setColumn(Column.GAMBLERS_2, edGamblers2, 1, 0)
                setColumn(Column.GAMBLERS_1, edGamblers1, 1, 0)

                agilityClasses.qualifyingPoints = (agilityClasses.openingTime * 6 / 10 / 1000) + 10
            } else if (agilityClasses.discretionaryCourseTime) {
                setColumn(Column.COURSE_TIME, edCourseTimeSimple, 1000, 0)
            } else {
                setColumn(Column.COURSE_LENGTH, edDistance, 1, 0)
                setColumn(Column.COURSE_TIME, edLarge, 1000, 0)
                setColumn(Column.COURSE_TIME_SMALL, edSmall, 1000, 0)
            }
            if (agilityClasses.isModified) {
                if (!agilityClasses.isValidGroupCourseData) {
                    Global.services.popUp("Warning", agilityClasses.reason)
                    return false
                }
                agilityClasses.post()
            }
            return true
        }
        return true
    }

    override fun onTextChange(view: TextView, editable: Editable) {
        when (view) {
            edDistance -> {
                if (Competition.isUka  && !agilityClasses.template.noTimeFormula) {
                    tvHint.text = ""
                    loLevel.visibility = View.GONE
                    if (edDistance.text.length > 1) {
                        val distance = setColumn(Column.COURSE_LENGTH, edDistance, 1, 0)
                        with(agilityClasses) {
                            if (isGroupCourseLengthValid(distance)) {
                                tvHint.text = "(large: $groupLargeMin-$groupLargeMax, Small: $groupSmallMin-$groupSmallMax)"
                                with(agilityClasses) {
                                    tvLevel1.text = "%1.3f".format(calcRealRateOfTravel(1.0))
                                    tvLevel2.text = "%1.3f".format(calcRealRateOfTravel(0.75))
                                    tvLevel3.text = "%1.3f".format(calcRealRateOfTravel(0.5))
                                    tvLevel4.text = "%1.3f".format(calcRealRateOfTravel(0.25))
                                    tvLevel5.text = "%1.3f".format(calcRealRateOfTravel(0.0))
                                    btLevel1.text = "$groupLargeMax/$groupSmallMax"
                                    btLevel2.text = "${getGroupLargeMid(0.75)}/${getGroupSmallMid(0.75)}"
                                    btLevel3.text = "${getGroupLargeMid(0.5)}/${getGroupSmallMid(0.5)}"
                                    btLevel4.text = "${getGroupLargeMid(0.25)}/${getGroupSmallMid(0.25)}"
                                    btLevel5.text = "$groupLargeMin/$groupSmallMin"
                                }
                                goneIf(agilityClasses.isGamblers || agilityClasses.isSnooker, loLevel)

                            }
                        }
                    }
                }
            }
            edLarge -> {
                if (Competition.isUka  && !agilityClasses.template.noTimeFormula) {
                    setColumn(Column.COURSE_LENGTH, edDistance, 1, 0)
                    setColumn(Column.COURSE_TIME, edLarge, 1000, 0)
                    setEditText(edSmall, agilityClasses.suggestGroupCourseTimeSmall(), 1000)
                }
            }
            edGambleTimeLarge -> {
                edGambleTimeSmall.text = edGambleTimeLarge.text
            }
            edGamblers7 -> {
                setColumn(Column.GAMBLERS_7, edGamblers7, 1, 0)
                tvGamblersCrib.setText(agilityClasses.obstaclesText)
            }
            edGamblers5 -> {
                setColumn(Column.GAMBLERS_5, edGamblers5, 1, 0)
                tvGamblersCrib.setText(agilityClasses.obstaclesText)
            }
            edGamblers4 -> {
                setColumn(Column.GAMBLERS_4, edGamblers4, 1, 0)
                tvGamblersCrib.setText(agilityClasses.obstaclesText)
            }
            edGamblers3 -> {
                setColumn(Column.GAMBLERS_3, edGamblers3, 1, 0)
                tvGamblersCrib.setText(agilityClasses.obstaclesText)
            }
            edGamblers2 -> {
                setColumn(Column.GAMBLERS_2, edGamblers2, 1, 0)
                tvGamblersCrib.setText(agilityClasses.obstaclesText)
            }
            edGamblers1 -> {
                setColumn(Column.GAMBLERS_1, edGamblers1, 1, 0)
                tvGamblersCrib.setText(agilityClasses.obstaclesText)
            }

        }
    }

    private fun setEditText(edit: EditText, value: Int, factor: Int) {
        if (value == 0) {
            edit.text.clear()
        } else {
            edit.setText(Integer.toString(value / factor))
        }
    }

    fun setCursor(position: Int): Boolean {
        if (doPost()) {
            loLevel.visibility = View.GONE
            with(agilityClasses) {
                cursor = position
                if (isUkOpen) {
                    tvSmall.text = "400/300"
                    tvGambleTimeSmall.text = "400/300"
                    tvLarge.text = "600/500"
                    tvGambleTimeLarge.text = "600/500"
                } else if (isFab) {
                    tvSmall.text = "Pt/Sm/Md"
                    tvGambleTimeSmall.text = "Pt/Sm/Md"
                    tvLarge.text = "Sd/Lg"
                    tvGambleTimeLarge.text = "Sd/Lg"
                } else if (isIfcs) {
                    tvSmall.text = "Toy/Min"
                    tvGambleTimeSmall.text = "Toy/Min"
                    tvLarge.text = "Mid/Max"
                    tvGambleTimeLarge.text = "Mid/Max"
                }
                if (isKc) {
                    setEditText(edDistanceSimple, courseLength, 1)
                    setEditText(edCourseTimeSimple, courseTime, 1000)
                    setEditText(edBatonFaults, batonFaults, 1)
                } else if (isGamblers) {
                    rgOpeningTime.clearCheck()
                    when (openingTime) {
                        25000 -> rb25.isChecked = true
                        30000 -> rb30.isChecked = true
                        35000 -> rb35.isChecked = true
                        40000 -> rb40.isChecked = true
                    }
                    setEditText(edGambleTimeLarge, courseTime - openingTime, 1000)
                    setEditText(edGambleTimeSmall, courseTimeSmall - openingTime, 1000)

                    setEditText(edGamblers7, obstacles7Point, 1)
                    setEditText(edGamblers5, obstacles5Point, 1)
                    setEditText(edGamblers4, obstacles4Point, 1)
                    setEditText(edGamblers3, obstacles3Point, 1)
                    setEditText(edGamblers2, obstacles2Point, 1)
                    setEditText(edGamblers1, obstacles1Point, 1)
                    tvGamblersCrib.setText(obstaclesText)


                } else if (discretionaryCourseTime) {
                    setEditText(edCourseTimeSimple, courseTime, 1000)
                } else {
                    setEditText(edDistance, courseLength, 1)
                    setEditText(edLarge, courseTime, 1000)
                    setEditText(edSmall, courseTimeSmall, 1000)
                }
                if (agilityClasses.isTimeOutAndFault) {
                    rgObstacles.clearCheck()
                    when (qualifyingPoints) {
                        17 -> rb17.isChecked = true
                        18 -> rb18.isChecked = true
                        19 -> rb19.isChecked = true
                        20 -> rb20.isChecked = true
                    }
                }
                btPrevious.isEnabled = !isFirst
                btNext.isEnabled = !isLast

                tvClass.text = "$name (${cursor + 1} of $rowCount)"
            }

            goneIf(!agilityClasses.isTimeOutAndFault, loObstacles)

            goneIf(agilityClasses.isGamblers || agilityClasses.discretionaryCourseTime || agilityClasses.isKc, loRegular)
            goneIf(!(agilityClasses.discretionaryCourseTime|| agilityClasses.isKc), loSimple)
            goneIf(!agilityClasses.isKc, tvDistanceSimple)
            goneIf(!agilityClasses.isKc, edDistanceSimple)
            goneIf(!agilityClasses.isGamblers, loGamblers)


            goneIf(!agilityClasses.template.courseLengthNeeded, loDistance)
            goneIf(!agilityClasses.template.courseLengthNeeded, tvHint)

            goneIf(!agilityClasses.isKc || !agilityClasses.template.isRelay, loBatonFaults)
            return true
        } else {
            return false
        }
    }

    override fun whenClick(view: View) {
        when (view) {
            bt0 -> keypress(KEYCODE_0)
            bt1 -> keypress(KEYCODE_1)
            bt2 -> keypress(KEYCODE_2)
            bt3 -> keypress(KEYCODE_3)
            bt4 -> keypress(KEYCODE_4)
            bt5 -> keypress(KEYCODE_5)
            bt6 -> keypress(KEYCODE_6)
            bt7 -> keypress(KEYCODE_7)
            bt8 -> keypress(KEYCODE_8)
            bt9 -> keypress(KEYCODE_9)
            btDel -> keypress(KEYCODE_DEL)

            btDone -> sendSignal(SignalCode.BACK)
            btPrevious -> sendSignal(SignalCode.PREVIOUS)
            btNext -> sendSignal(SignalCode.NEXT)
            btClear -> sendSignal(SignalCode.DELETE_ALL)

            btLevel1 -> sendSignal(SignalCode.LEVEL, 1)
            btLevel2 -> sendSignal(SignalCode.LEVEL, 2)
            btLevel3 -> sendSignal(SignalCode.LEVEL, 3)
            btLevel4 -> sendSignal(SignalCode.LEVEL, 4)
            btLevel5 -> sendSignal(SignalCode.LEVEL, 5)

            rb25 -> sendSignal(SignalCode.SEQUENCE, 25000)
            rb30 -> sendSignal(SignalCode.SEQUENCE, 30000)
            rb35 -> sendSignal(SignalCode.SEQUENCE, 35000)
            rb40 -> sendSignal(SignalCode.SEQUENCE, 40000)

            rb17 -> sendSignal(SignalCode.OBSTACLES, 17)
            rb18 -> sendSignal(SignalCode.OBSTACLES, 18)
            rb19 -> sendSignal(SignalCode.OBSTACLES, 19)
            rb20 -> sendSignal(SignalCode.OBSTACLES, 20)

            else -> super.whenClick(view)
        }

    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET -> {
                var proposedCursor = 0
                while (agilityClasses.next()) {
                    if (agilityClasses.isFirst || agilityClasses.id == ringPartyData.agilityClass.id) {
                        proposedCursor = agilityClasses.cursor
                    }
                }
                setCursor(proposedCursor)
                signal.consumed()
            }
            SignalCode.PREVIOUS -> {
                setCursor(agilityClasses.cursor - 1)
                signal.consumed()
            }
            SignalCode.NEXT -> {
                setCursor(agilityClasses.cursor + 1)
                signal.consumed()
            }
            SignalCode.DELETE_ALL -> {
                val target = currentFocus
                if (target is EditText) {
                    target.setText("")
                    if (target === edDistance) {
                        edLarge.setText("")
                        edSmall.setText("")
                    }
                }
                signal.consumed()
            }
            SignalCode.LEVEL -> {
                val level = signal._payload as Int?
                when (level) {
                    1 -> {
                        edLarge.setText(Integer.toString(agilityClasses.groupLargeMax))
                        edSmall.setText(Integer.toString(agilityClasses.groupSmallMax))
                    }
                    2 -> {
                        edLarge.setText(Integer.toString(agilityClasses.getGroupLargeMid(0.75)))
                        edSmall.setText(Integer.toString(agilityClasses.getGroupSmallMid(0.75)))
                    }
                    3 -> {
                        edLarge.setText(Integer.toString(agilityClasses.getGroupLargeMid(0.5)))
                        edSmall.setText(Integer.toString(agilityClasses.getGroupSmallMid(0.5)))
                    }
                    4 -> {
                        edLarge.setText(Integer.toString(agilityClasses.getGroupLargeMid(0.25)))
                        edSmall.setText(Integer.toString(agilityClasses.getGroupSmallMid(0.25)))
                    }
                    5 -> {
                        edLarge.setText(Integer.toString(agilityClasses.groupLargeMin))
                        edSmall.setText(Integer.toString(agilityClasses.groupSmallMin))
                    }
                }
                signal.consumed()
            }
            SignalCode.SEQUENCE -> {
                val openingTime = signal._payload as? Int
                if (openingTime != null) {
                    agilityClasses.openingTime = openingTime
                }
                signal.consumed()
            }
            SignalCode.OBSTACLES -> {
                val obstacles = signal._payload as? Int
                if (obstacles != null) {
                    agilityClasses.qualifyingPoints = obstacles
                }
                signal.consumed()
            }
            SignalCode.BACK -> {
                if (doPost()) {
                    super.whenSignal(signal)
                } else {
                    signal.consumed()
                }
            }
            else -> {
                super.whenSignal(signal)
            }
        }
    }

}
