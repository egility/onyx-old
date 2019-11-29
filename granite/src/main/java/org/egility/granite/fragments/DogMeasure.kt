/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import kotlinx.android.synthetic.main.dog_measure.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.AndroidUtils.goneIf
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Entry
import org.egility.library.dbobject.Height
import org.egility.library.general.*

/**
 * Created by mbrickman on 23/05/17.
 */
class DogMeasure : BaseFragment(R.layout.dog_measure) {

    private val entry = Entry()
    val idDog = 0
    var data = MeasuringServicesData
    val dog = data.selectedDog
    var under2 = false

    init {
        entry.agilityClass.joinToParent()
    }

    override fun whenInitialize() {
        tvPageHeader.text = Competition.current.uniqueName + " - Competitor Services"
        tvCompetitor.text = dog.handler.fullName
        tvDog.text = "${dog.petName} - ${Height.getHeightName(dog.ukaHeightCode)}"
        tvPrevious.text = dog.ukaMeasurementText
        tvHeightText.text = ""
        tvDateOfBirthHint.text = ""
        xbDisputed.isChecked = false
        edMeasurement.setText("")
        AndroidUtils.hideSoftKeyboard(edDateOfBirth)
        AndroidUtils.hideSoftKeyboard(edMeasurement)
        if (dog.dateOfBirth.isEmpty()) {
            edDateOfBirth.setText(textToDate(""))
            edDateOfBirth.requestFocus()
        } else {
            edDateOfBirth.setText(dog.dateOfBirth.dateTextShort)
            edMeasurement.requestFocus()
            if (dog.dateOfBirth.after(today.addYears(-2))) {
                tvDateOfBirthHint.text = "Under 2"
                under2 = true
            }
        }

        goneIf(true, btBack)
        goneIf(under2, tvDisputedLabel)
        goneIf(under2, xbDisputed)
        goneIf(under2 || !xbDisputed.isChecked, tvDisputed)

        edMeasurement.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val mm = s.toString().toIntDef(0)
                val heightText = when (mm) {
                    in 0..99 -> ""
                    in 100..350 -> "Toy"
                    in 351..430 -> "Midi"
                    in 431..500 -> "Standard"
                    else -> "Maxi"
                }
                tvHeightText.setText(heightText)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                doNothing()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                doNothing()
            }

        })

        xbDisputed.setOnCheckedChangeListener { _, checked ->
            goneIf(!checked, tvDisputed)
        }

        edDateOfBirth.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                tvDateOfBirthHint.text = ""
                under2 = false
                val dateOfBirthText = edDateOfBirth.text.toString().replace("/", "").replace("_", "")
                if (dateOfBirthText.length == 6) {
                    val dateOfBirth = if (dateOfBirthText.isNotEmpty()) dateOfBirthText.toDate("ddMMyy") else nullDate
                    if (dateOfBirth.isNotEmpty() && dateOfBirth.after(today.addMonths(-15))) {
                        tvDateOfBirthHint.text = "Too Young!!!"
                    } else if (dateOfBirth.isNotEmpty() && dateOfBirth.after(today.addYears(-2))) {
                        tvDateOfBirthHint.text = "Under 2"
                        under2 = true
                    }
                }
                goneIf(under2, tvDisputedLabel)
                goneIf(under2, xbDisputed)
                goneIf(under2 || !xbDisputed.isChecked, tvDisputed)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                doNothing()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                doNothing()
            }

        })
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
                btDel -> sendSignal(SignalCode.DELETE)
                btDelAll -> sendSignal(SignalCode.DELETE_ALL)
                btCancel -> sendSignal(SignalCode.SELECT_DOG_USING_CODE)
                btBack -> sendSignal(SignalCode.BACK)
                btFinished -> sendSignal(SignalCode.OK)
            }
        }
    }

    val focusedView: EditText
        get() {
            val result = activity?.currentFocus
            return if (result is EditText) result else edMeasurement
        }

    fun textToDate(text: String): String {
        val result = text.padEnd(6, '_')
        return result.substring(0, 2) + "/" + result.substring(2, 4) + "/" + result.substring(4, 6)
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                signal.consumed()
            }
            SignalCode.DIGIT -> {
                val char = signal._payload as Int?
                if (char != null) {
                    var text = focusedView.text.toString().replace("/", "").replace("_", "") + char.toString()
                    if (focusedView == edDateOfBirth) text = textToDate(text)
                    focusedView.setText(text)
                    signal.consumed()
                }
            }
            SignalCode.DELETE -> {
                var text = focusedView.text.toString().replace("/", "").replace("_", "")
                if (!text.isEmpty()) {
                    text = text.substring(0, text.length - 1)
                    if (focusedView == edDateOfBirth) text = textToDate(text)
                    focusedView.setText(text)
                }
                signal.consumed()
            }
            SignalCode.DELETE_ALL -> {
                var text = ""
                if (focusedView == edDateOfBirth) text = textToDate(text)
                focusedView.setText(text)
                signal.consumed()
            }
            SignalCode.OK -> {
                val disputed = xbDisputed.isChecked && !under2
                val measurement = edMeasurement.text.toString().toIntDef(0)
                val dateOfBirthText = edDateOfBirth.text.toString().replace("/", "").replace("_", "")
                val dateOfBirth = if (dateOfBirthText.isNotEmpty()) dateOfBirthText.toDate("ddMMyy") else nullDate
                if (dateOfBirth.isNotEmpty() && (dateOfBirth.format("ddMMyy") != dateOfBirthText || dateOfBirth.after(today) || dateOfBirth.before(today.addYears(-20)))) {
                    popUp("Error", "Date of birth is not valid - please correct")
                    edDateOfBirth.requestFocus()
                } else if (measurement < 120 || measurement > 700) {
                    popUp("Error", "The measurement is not valid - please correct")
                    edMeasurement.requestFocus()
                } else {
                    var message = "Please confirm that ${dog.cleanedPetName} has been measured at ${measurement}mm"
                    if (disputed) message += " (DISPUTED)"
                    if (dateOfBirth > today.addYears(-2)) {
                        message += " and will need to be re-measured when 2 years old"
                        if (measurement > 500) {
                            message += " (unless ${if (dog.gender == 0) "he" else "she"} will only compete at Maxi)"
                        }
                    }
                    msgYesNo("Confirm", "$message.") { yes ->
                        if (yes) {
                            dog.addUkaMeasurement(dateOfBirth, measurement, data.idCompetitorMeasurer, Competition.current.id, disputed)
                            sendSignal(SignalCode.SELECT_DOG_USING_CODE)
                        }
                    }
                }
            }
            else -> {
                doNothing()
            }

        }

    }


}