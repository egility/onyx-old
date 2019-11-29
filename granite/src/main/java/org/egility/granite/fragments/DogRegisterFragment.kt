/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.fragment_dog_register.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils.goneIf
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.tools.addButton
import org.egility.granite.R
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Grade
import org.egility.library.dbobject.Height
import org.egility.library.dbobject.Team
import org.egility.library.general.MemberServicesData
import org.egility.library.general.doNothing


/**
 * Created by mbrickman on 12/03/16.
 */

class DogRegisterFragment : BaseFragment(R.layout.fragment_dog_register) {

    var data = MemberServicesData
    var dog = data.selectedDog
    val team = Team()

    init {
        isBackable = false
    }

    fun setupRadioGroups() {
        val thisActivity = activity

        goneIf(dog.hasUkaHeight, loHeight)
        goneIf(dog.hasUkaLevel, loLevel)
        
        if (thisActivity != null) {
            if (!dog.hasUkaHeight) {
                Height.allHeights.forEach { height ->
                    if (height.organization == "UKA" && !height.classHeightOnly) {
                        val heightButton =
                            rgHeight.addButton(this, height.name, SignalCode.HEIGHT_SELECTED, height.code)
                        if (height.code == dog.ukaHeightCode) {
                            heightButton?.performClick()
                        }
                    }
                }
            }
            if (!dog.hasUkaLevel) {
                Grade.allGrades.forEach { grade ->
                    if (grade.organization == "UKA" && !grade.classGradeOnly) {
                        val gradeButton = rgGrade.addButton(this, grade.name, SignalCode.GRADE_SELECTED, grade.code)
                        if (grade.code == dog.ukaEntryLevel) {
                            gradeButton?.performClick()
                        }
                    }
                }
            }
        }
    }

    override fun whenInitialize() {
        tvPageHeader.text = "${Competition.current.uniqueName} - Registration"
        tvTitle.text = "${data.cleanedPetName} - Registration"
        setupRadioGroups()
    }


    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btCancel -> {
                    sendSignal(SignalCode.BACK)
                }
                btOK -> {
                    dog.post()
                    sendSignal(SignalCode.DOG_REGISTER_CHECKOUT)
                }
            }
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.BACK -> {
                dog.undoEdits()
            }
            SignalCode.HEIGHT_SELECTED -> {
                val heightCode = signal._payload as String?
                if (heightCode!=null) {
                    dog.ukaDeclaredHeightCode = heightCode
                }
            }
            SignalCode.GRADE_SELECTED -> {
                val gradeCode = signal._payload as String?
                if (gradeCode!=null) {
                    dog.ukaEntryLevel = gradeCode
                }
            }
            else -> {
                doNothing()
            }
        }

    }

}