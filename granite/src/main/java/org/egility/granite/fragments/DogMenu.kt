/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.dog_menu.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils.goneIf
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.tools.doHourglass
import org.egility.granite.R
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Entry
import org.egility.library.dbobject.Grade
import org.egility.library.dbobject.Height
import org.egility.library.general.*

/**
 * Created by mbrickman on 23/05/17.
 */
class DogMenu : BaseFragment(R.layout.dog_menu) {

    val data = CompetitorServicesData
    private val entry = Entry()
    val idDog = 0

    init {
        entry.agilityClass.joinToParent()
    }

    override fun whenInitialize() {
        tvPageHeader.text = Competition.current.uniqueName + " - Competitor Services"
        tvCompetitor.text = data.competitor.fullName
        if (Competition.current.isFab) {
            val agility = Grade.getGradeShort(data.competitionDog.fabGradeAgility)
            val jumping = Grade.getGradeShort(data.competitionDog.fabGradeJumping)
            val steeplechase = Grade.getGradeShort(data.competitionDog.fabGradeSteeplechase)
            val height = Height.getHeightName(data.competitionDog.fabHeightCode)
            val ifcsHeight = Height.getHeightName(data.competitionDog.ifcsHeightCode)
            val collie = if (data.competitionDog.fabCollie) "Collie/X" else "ABC"
            tvDog.text =
                "${data.dog.petName} (${data.dog.code}): $height, $collie, A=$agility, J=$jumping, S=$steeplechase, IFCS=$ifcsHeight"
        } else if (Competition.current.isUkOpen) {
            val height = Height.getHeightName(data.competitionDog.ukOpenHeightCode)
            val group = data.competitionDog.ukOpenGroup
            if (data.competitionDog.ukOpenWithdrawn) {
                tvDog.text = "${data.dog.petName} (${data.dog.code}): WITHDRAWN"
            } else {
                tvDog.text = "${data.dog.petName} (${data.dog.code}): $height, Group $group"
            }
        } else {
            tvDog.text =
                "${data.dog.petName} (${data.dog.code}: G${Grade.getGradeShort(data.competitionDog.kcGradeCode)}, ${Height.getCombinedName(data.competitionDog.kcHeightCode, data.competitionDog.kcJumpHeightCode, short = true)})"
        }
        if (data.dogMenuStack.isEmpty()) {
            selectMenu("main")
        } else {
            val menu = data.dogMenuStack.pop()
            selectMenu(menu)
        }
    }


    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btBack -> {
                    sendSignal(SignalCode.BACK)
                }
                btFinished -> {
                    sendSignal(SignalCode.FINISHED)
                }
                btCancel -> {
                    sendSignal(SignalCode.CANCEL)
                }
            }
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                signal.consumed()
            }
            SignalCode.FINISHED -> {
                sendSignal(SignalCode.RESET)
            }
            SignalCode.CHANGE_GRADE -> {
                if (Competition.isFab) {
                    selectMenu("change_grade_programme")
                } else {
                    selectMenu("change_grade")
                }
            }
            SignalCode.CHANGE_HEIGHT -> {
                if (Competition.isFab) {
                    selectMenu("change_height_organization")
                } else {
                    selectMenu("change_height")
                }
            }
            SignalCode.CHANGE_JUMP_HEIGHT -> {
                selectMenu("change_jump_height")
            }
            SignalCode.BACK -> {
                if (!data.dogMenuStack.empty()) {
                    data.dogMenuStack.pop()
                }
                if (!data.dogMenuStack.empty()) {
                    selectMenu(data.dogMenuStack.pop())
                    signal.consumed()
                }
            }
            SignalCode.CANCEL -> {
                data.clearProposed()
                data.dogMenuStack.clear()
                selectMenu("main")
            }
            SignalCode.CHANGE_DIVISION -> {
                if (Competition.isFab) {
                    val subDivision = signal._payload as? Int
                    if (subDivision != null) {
                        doHourglass(activity, "Thinking about it...", {
                            data.changeFabDivision(subDivision)
                        }, {
                            data.dogMenuStack.clear()
                            whenInitialize()
                        })
                    }
                }
            }
            SignalCode.HEIGHT_CHANGED -> {
                if (Competition.isFab) {
                    val heightCode = signal._payload as? String
                    val organization = signal._payload2 as? Int
                    if (heightCode != null && organization != null) {
                        doHourglass(activity, "Thinking about it...", {
                            data.changeFabHeight(organization, heightCode)
                        }, {
                            data.dogMenuStack.clear()
                            whenInitialize()
                        })
                    }
                } else if (Competition.isUkOpen) {
                    val heightCode = signal._payload as? String
                    if (heightCode != null) {
                        doHourglass(activity, "Thinking about it...", {
                            data.changeUkOpenHeight(heightCode)
                        }, {
                            data.dogMenuStack.clear()
                            whenInitialize()
                        })
                    }
                } else {
                    val heightCode = signal._payload as? String
                    if (heightCode != null) {
                        data.proposedHeightCode = heightCode
                        selectMenu("change_jump_height")
                    }
                }
            }
            SignalCode.GROUP_CHANGED -> {
                val group = signal._payload as? String
                if (group != null) {
                    doHourglass(activity, "Thinking about it...", {
                        data.changeUkOpenGroup(group)
                    }, {
                        data.dogMenuStack.clear()
                        whenInitialize()
                    })
                }
            }
            SignalCode.HEIGHT_TYPE_CHANGED -> {
                val organization = signal._payload as? Int
                if (organization != null) {
                    selectMenu("change_height", organization)
                }
            }
            SignalCode.GRADE_TYPE_CHANGED -> {
                val gradeType = signal._payload as? Int
                if (gradeType != null) {
                    selectMenu("change_grade", gradeType)
                }
            }
            SignalCode.GRADE_CHANGED -> {
                if (Competition.isFab) {
                    val gradeCode = signal._payload as? String
                    val programme = signal._payload2 as? Int
                    if (gradeCode != null && programme != null) {
                        doHourglass(activity, "Thinking about it...", {
                            data.changeFabGrade(programme, gradeCode)
                        }, {
                            sendSignal(SignalCode.REVIEW_CLASSES)
                        })
                    }
                } else {
                    val gradeCode = signal._payload as? String
                    if (gradeCode != null) {
                        data.proposedGradeCode = gradeCode
                        sendSignal(SignalCode.REVIEW_CLASSES)
                    }
                }
            }
            SignalCode.JUMP_HEIGHT_CHANGED -> {
                val jumpHeightCode = signal._payload as? String
                if (jumpHeightCode != null) {
                    data.proposedJumpHeightCode = jumpHeightCode
                    sendSignal(SignalCode.REVIEW_CLASSES)
                }
            }
            SignalCode.PRINT_RUNNING_ORDERS -> {
                Reports.printPersonalRunningOrders(data.dog.idAccount, Competition.current.id)
                signal.consumed()
            }
            SignalCode.DO_MENU -> {
                val menu = signal._payload as String?
                if (menu != null) {
                    selectMenu(menu)
                }
            }
            SignalCode.WITHDRAWN -> {
                msgYesNo("Question", "Are you sure you want to withdraw ${data.dog.cleanedPetName}") { yes ->
                    if (yes) {
                        doHourglass(activity, "Thinking about it...", {
                            UkOpenUtils.drop(Competition.current.id, data.dog.id)
                        }, {
                            data.competitionDog.refresh()
                            data.dogMenuStack.clear()
                            whenInitialize()
                        })
                    }
                }
            }
            SignalCode.UN_WITHDRAWN -> {
                doHourglass(activity, "Thinking about it...", {
                    UkOpenUtils.undrop(Competition.current.id, data.dog.id)
                }, {
                    data.competitionDog.refresh()
                    data.dogMenuStack.clear()
                    whenInitialize()
                })
            }
            else -> {
                doNothing()
            }

        }

    }

    fun top() {
        data.dogMenuStack.clear()
    }


    fun selectMenu(menu: String, menuData: Int = 0) {
        data.dogMenuStack.push(menu)
        loMenu.removeAllViews()
        loMenu.columnCount = 1
        when (menu) {
            "main" -> {
                tvSubTitle.text = "Options"
                if (!data.dog.isUkaRegistered) {

                }
                when {
                    Competition.isFab -> {
                        addMenuButton(loMenu, "Change Grade", SignalCode.CHANGE_GRADE, buttonWidth = 260)
                        addMenuButton(loMenu, "Change Height", SignalCode.CHANGE_HEIGHT, buttonWidth = 260)
                        if (data.competitionDog.fabCollie) {
                            addMenuButton(loMenu, "Change to ABC", SignalCode.CHANGE_DIVISION, 0, buttonWidth = 260)
                        } else {
                            addMenuButton(loMenu, "Change to Collie/X", SignalCode.CHANGE_DIVISION, 1, buttonWidth = 260)
                        }
                        addMenuButton(loMenu, "Review Classes", SignalCode.REVIEW_CLASSES, buttonWidth = 260)
                        addMenuButton(loMenu, "Print Running Orders", SignalCode.PRINT_RUNNING_ORDERS, buttonWidth = 260)
                    }
                    Competition.isUkOpen -> {
                        addMenuButton(loMenu, "Change Dog's Height", SignalCode.CHANGE_HEIGHT, buttonWidth = 260)
                        addMenuButton(loMenu, "Change Dog's Group", SignalCode.DO_MENU, "change_group", buttonWidth = 260)
                        if (data.competitionDog.ukOpenWithdrawn) {
                            addMenuButton(loMenu, "Re-instate", SignalCode.UN_WITHDRAWN, buttonWidth = 260)
                        } else {
                            addMenuButton(loMenu, "Withdraw", SignalCode.WITHDRAWN, buttonWidth = 260)
                        }
                    }
                    else -> {
                        addMenuButton(loMenu, "Change Grade", SignalCode.CHANGE_GRADE, buttonWidth = 260)
                        addMenuButton(loMenu, "Change Dog's Height", SignalCode.CHANGE_HEIGHT, buttonWidth = 260)
                        addMenuButton(loMenu, "Change Jump Height", SignalCode.CHANGE_JUMP_HEIGHT, buttonWidth = 260)
                        addMenuButton(loMenu, "Review Classes", SignalCode.REVIEW_CLASSES, buttonWidth = 260)
                        addMenuButton(loMenu, "Print Running Orders", SignalCode.PRINT_RUNNING_ORDERS, buttonWidth = 260)

                    }
                }
            }
            "change_grade_programme" -> {
                tvSubTitle.text = "Change grade for"
                addMenuButton(loMenu, "Agility", SignalCode.GRADE_TYPE_CHANGED, 1, buttonWidth = 200)
                addMenuButton(loMenu, "Jumping", SignalCode.GRADE_TYPE_CHANGED, 2, buttonWidth = 200)
                addMenuButton(loMenu, "Steeplechase", SignalCode.GRADE_TYPE_CHANGED, 3, buttonWidth = 200)
            }
            "change_grade" -> {
                if (Competition.isFab) {
                    val programme = when (menuData) {
                        2 -> "jumping"
                        3 -> "steeplechase"
                        else -> "agility"
                    }
                    val currentGrade = when (menuData) {
                        2 -> data.competitionDog.fabGradeJumping
                        3 -> data.competitionDog.fabGradeSteeplechase
                        else -> data.competitionDog.fabGradeAgility
                    }
                    tvSubTitle.text = "Change $programme grade to"
                    for (gradeCode in Grade.fabGrades) {
                        if (gradeCode != currentGrade) {
                            addMenuButton(loMenu, Grade.getGradeName(gradeCode), SignalCode.GRADE_CHANGED, gradeCode, menuData, buttonWidth = 200)
                        }
                    }
                } else {
                    loMenu.columnCount = 2
                    tvSubTitle.text = "Change grade to"
                    data.proposedGradeCode = data.competitionDog.kcGradeCode
                    for (i in 1..7) {
                        val gradeCode = "KC0$i"
                        if (gradeCode != data.proposedGradeCode) {
                            addMenuButton(loMenu, "Grade $i", SignalCode.GRADE_CHANGED, gradeCode, buttonWidth = 200)
                        }
                    }
                }
            }
            "change_height_organization" -> {
                tvSubTitle.text = "Change height for"
                addMenuButton(loMenu, "FAB", SignalCode.HEIGHT_TYPE_CHANGED, ORGANIZATION_FAB, buttonWidth = 200)
                addMenuButton(loMenu, "IFCS", SignalCode.HEIGHT_TYPE_CHANGED, ORGANIZATION_IFCS, buttonWidth = 200)
            }
            "change_height" -> {
                if (Competition.isFab) {
                    if (menuData == ORGANIZATION_IFCS) {
                        tvSubTitle.text = "Change IFCS height to"
                        for (heightCode in Height.ifcsHeights) {
                            if (heightCode != data.competitionDog.ifcsHeightCode) {
                                addMenuButton(loMenu, Height.getHeightName(heightCode), SignalCode.HEIGHT_CHANGED, heightCode, menuData, buttonWidth = 200)
                            }
                        }
                    } else {
                        tvSubTitle.text = "Change FAB height to"
                        for (heightCode in Height.fabHeights) {
                            if (heightCode != data.competitionDog.fabHeightCode) {
                                addMenuButton(loMenu, Height.getHeightName(heightCode), SignalCode.HEIGHT_CHANGED, heightCode, menuData, buttonWidth = 200)
                            }
                        }
                    }
                } else if (Competition.isUkOpen) {
                    loMenu.columnCount = 1
                    tvSubTitle.text = "Change dog's height to"
                    data.proposedHeightCode = data.competitionDog.ukOpenHeightCode
                    for (heightCode in Height.ukOpenHeights) {
                        if (heightCode != data.proposedHeightCode) {
                            addMenuButton(loMenu, Height.getHeightName(heightCode), SignalCode.HEIGHT_CHANGED, heightCode)
                        }
                    }
                } else {
                    loMenu.columnCount = 1
                    tvSubTitle.text = "Change dog's height to"
                    data.proposedHeightCode = data.competitionDog.kcHeightCode
                    for (height in Competition.current.heightOptions.split(",")) {
                        val heightCode = height.substringBefore(":")
                        if (heightCode != data.proposedHeightCode) {
                            addMenuButton(loMenu, Height.getHeightName(heightCode), SignalCode.HEIGHT_CHANGED, heightCode)
                        }
                    }
                }
            }
            "change_group" -> {
                loMenu.columnCount = 1
                tvSubTitle.text = "Change dog's group to"
                data.proposedGroup = data.competitionDog.ukOpenGroup
                for (group in "A,B".split(",")) {
                    if (group != data.proposedGroup) {
                        addMenuButton(loMenu, "Group $group", SignalCode.GROUP_CHANGED, group)
                    }
                }
            }
            "change_jump_height" -> {
                loMenu.columnCount = 1
                tvSubTitle.text = "Change jump height to"
                data.proposedJumpHeightCode = data.competitionDog.kcJumpHeightCode
                for (height in Competition.current.heightOptions.split(",")) {
                    val heightCode = height.substringBefore(":")
                    if (heightCode == data.proposedHeightCode) {
                        val jumpHeightCodes = height.substringAfter(":").split("|")
                        for (jumpHeightCode in jumpHeightCodes) {
                            if (heightCode != data.proposedHeightCode || jumpHeightCode != data.proposedJumpHeightCode) {
                                addMenuButton(loMenu, Height.getCombinedName(heightCode, jumpHeightCode), SignalCode.JUMP_HEIGHT_CHANGED, jumpHeightCode)
                            }
                        }
                    }
                }
            }
        }
        CheckButtons()
    }

    fun CheckButtons() {
        goneIf(data.isUpdated, btFinished)
        goneIf(!data.isUpdated, btCancel)
    }


}