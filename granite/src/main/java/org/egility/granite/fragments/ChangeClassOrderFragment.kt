/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.RadioButton
import kotlinx.android.synthetic.main.move_class_before.*
import org.egility.library.dbobject.AgilityClass
import org.egility.library.general.CLASS_RUNNING
import org.egility.library.general.doNothing
import org.egility.library.general.ringPartyData
import org.egility.android.BaseFragment
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import org.egility.library.general.CLASS_WALKING

/**
 * Created by mbrickman on 21/08/17.
 */
class ChangeClassOrderFragment : BaseFragment(R.layout.move_class_before) {

    lateinit private var agilityClasses: AgilityClass
    var newRingOrder = -1

    override fun whenInitialize() {
        agilityClasses = ringPartyData.ring.agilityClasses
        showClassGroups()
        tvPageHeader.text = "Ring " + ringPartyData.ring.number + " - Move Class"
        tvRadioGroupHeading.text = "Move ${ringPartyData.agilityClass.name} Before:"
    }

    fun showClassGroups() {
        var selectedRadioButton: RadioButton? = null

        rgAgilityClass.removeAllViews()

        while (agilityClasses.next()) {
            if (agilityClasses.id != ringPartyData.agilityClass.id && agilityClasses.progress <= CLASS_WALKING) {
                val fragmentActivity = activity
                if (fragmentActivity != null) {
                    val radioButton = fragmentActivity.layoutInflater.inflate(R.layout.template_radio_button, null) as RadioButton
                    radioButton.text = agilityClasses.name
                    radioButton.id = View.generateViewId()
                    radioButton.tag = prepareSignal(
                        SignalCode.NEW_RING_ORDER,
                        if (agilityClasses.ringOrder <= ringPartyData.agilityClass.ringOrder) agilityClasses.ringOrder else agilityClasses.ringOrder - 1
                    )
                    if (selectedRadioButton == null || agilityClasses.ringOrder == ringPartyData.agilityClass.ringOrder + 1) {
                        selectedRadioButton = radioButton
                    }
                    rgAgilityClass.addView(radioButton)
                }
            }
        }
        if (selectedRadioButton != null) {
            selectedRadioButton.performClick()
        }
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btSelectClassOK -> sendSignal(SignalCode.OK)
                btSelectClassCancel -> sendSignal(SignalCode.BACK)
            }
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.NEW_RING_ORDER -> {
                val _newRingOrder = signal._payload as? Int
                if (_newRingOrder != null) {
                    newRingOrder = _newRingOrder
                    signal.consumed()
                }
            }
            SignalCode.OK -> {
                if (newRingOrder >= 0) {
                    ringPartyData.agilityClass.moveToRingOrder(newRingOrder)
                    ringPartyData.ring.selectFirstOpenAgilityClass()
                }
                sendSignal(SignalCode.RING_DETAILS, ringPartyData.ring.selector)
                signal.consumed()
            }
            else -> {
                doNothing()
            }
        }
    }

}
