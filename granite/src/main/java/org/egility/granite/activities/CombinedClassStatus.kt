/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import org.egility.granite.fragments.EditRunningOrderFragment
import org.egility.granite.fragments.MoveInFrontOfFragment
import org.egility.granite.fragments.SpecialClassMenu
import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Entry
import org.egility.library.general.whenYes
import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import org.egility.library.general.ClassData
import org.egility.library.general.ClassTemplate

class CombinedClassStatus : BaseActivity(R.layout.content_holder) {

    private lateinit var specialClassMenu: SpecialClassMenu
    private lateinit var editRunningOrderFragment: EditRunningOrderFragment
    private lateinit var moveInFrontOfFragment: MoveInFrontOfFragment
    

    init {
        if (!dnr) {
            specialClassMenu = SpecialClassMenu()
            editRunningOrderFragment = EditRunningOrderFragment()
            moveInFrontOfFragment = MoveInFrontOfFragment()
        }
    }

    val data = ClassData

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET -> {
                defaultFragmentContainerId = R.id.loContent
                loadFragment(specialClassMenu)
                signal.consumed()
            }
            SignalCode.EDIT_RUNNING_ORDERS -> {
                if (data.agilityClass.hasChildren) {
                    data.activeChildClass = data.agilityClass.childClasses()
                } else {
                    data.activeChildClass = data.agilityClass
                }
                data.activeChildClass.beforeFirst()
                while (data.activeChildClass.next()) {
                    if (!data.activeChildClass.isChild || data.activeChildClass.isOpen) {
                        editRunningOrderFragment.agilityClass = data.activeChildClass
                        moveInFrontOfFragment.agilityClass = data.activeChildClass
                        loadFragment(editRunningOrderFragment)
                        break
                    }
                }
                signal.consumed()
            }
            SignalCode.ENTRY_SELECTED -> {
                loadFragment(moveInFrontOfFragment)
                signal.consumed()
            }
            SignalCode.MOVE_IN_FRONT_OF -> {
                val newRunningOrder = signal._payload as? Int
                if (newRunningOrder != null) {
                    val team = if (data.targetEntry.agilityClass.template == ClassTemplate.TEAM_INDIVIDUAL) data.targetEntry.team.getCompetitorDog(data.targetEntry.teamMember) else data.targetEntry.team.description

                    whenYes(
                        "Question",
                        "Are you sure you want to move $team to r/o $newRunningOrder"
                    ) {
                        data.targetEntry.moveToRunningOrder(newRunningOrder)
                        sendSignal(SignalCode.EDIT_RUNNING_ORDERS)
                    }
                }
            }
            else -> {
                super.whenSignal(signal)
            }
        }
    }

}
