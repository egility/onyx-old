/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.content.Intent
import android.support.v4.app.FragmentActivity
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.inputmethod.EditorInfo
import kotlinx.android.synthetic.main.dog_menu_uka.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils.disableIf
import org.egility.android.tools.AndroidUtils.goneIf
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.tools.doHourglass
import org.egility.granite.R
import org.egility.granite.activities.SelectTeam
import org.egility.library.dbobject.*
import org.egility.library.general.*

/**
 * Created by mbrickman on 20/11/15.
 */
class DogMenuUka : BaseFragment(R.layout.dog_menu_uka) {

    var data = MemberServicesData

    class TeamMember() {
        var idDog = 0
        var idCompetitor = 0
        var jumpHeightCode = ""
        var petName = ""
    }

    class ActionData() {
        var selectedProgramme = 0
        var idTeamEntry = 0
        var teamMember = 1
        var idDogSelected = 0
        var idCompetitorSelected = 0
        var petNameSelected = ""
        var selectedGrade = ""
        var selectedJumpHeight = ""
        var isSplitPair = false
        var isTeam = false
        var proposedTeam = arrayOf(TeamMember(), TeamMember(), TeamMember())
        var proposedTeamName = ""
        var idAccount = 0
        var idAgilityClass = 0
        var idDog = 0
        var reviewMode = false
    }

    private enum class Action { NONE, CHANGE_GRADE, CHANGE_TEAM_MEMBER_HEIGHT, REPLACE_TEAM_MEMBER, ENTER_SPECIAL_CLASS }

    var shoppingList = data.shoppingList
    private var stack = data.dogMenuStack
    private var actionData = ActionData()

    private var action = Action.NONE
        set(value) {
            field = value
            actionData = ActionData()
        }

    private val entry: Entry
        get() = data.entry

    private val specialEntryClasses: AgilityClass
            get() = AgilityClass.specialParentClassesToday

    var _multiMemberClassList: String = "?"
    val multiMemberClassList: String
        get() {
            if (_multiMemberClassList == "?") {
                _multiMemberClassList = ""
                specialEntryClasses.withEach {
                    if (template.teamSize > 1) {
                        _multiMemberClassList = _multiMemberClassList.append(id.toString())
                    }
                }
            }
            return _multiMemberClassList
        }

    val proposedTeam: Array<TeamMember>
        get() = actionData.proposedTeam

    var selectedGrade: String
        get() = actionData.selectedGrade
        set(value) {
            actionData.selectedGrade = value
        }

    var selectedJumpHeight: String
        get() = actionData.selectedJumpHeight
        set(value) {
            actionData.selectedJumpHeight = value
        }

    var isSplitPair: Boolean
        get() = actionData.isSplitPair
        set(value) {
            actionData.isSplitPair = value
        }

    var isTeam: Boolean
        get() = actionData.isTeam
        set(value) {
            actionData.isTeam = value
        }

    var reviewMode: Boolean
        get() = actionData.reviewMode
        set(value) {
            actionData.reviewMode = value
        }

    val handlerTeam = Team()


    val specialTeam = Team()

    val proposedTeamDescription: String
        get() {
            var result = ""
            if (isTeam) {
                result = "Team Name: ${actionData.proposedTeamName}\n" +
                        "Member 1: ${proposedTeam[0].petName} (${Height.getHeightName(proposedTeam[0].jumpHeightCode)})\n" +
                        "Member 2: ${proposedTeam[1].petName} (${Height.getHeightName(proposedTeam[1].jumpHeightCode)})\n" +
                        "Member 3: ${proposedTeam[2].petName} (${Height.getHeightName(proposedTeam[2].jumpHeightCode)})\n"
            }
            return result
        }

    fun updateHandlers() {
        handlerTeam.select("idDog=${data.idDog} && teamType=$TEAM_NAMED_HANDLER")
    }

    override fun whenInitialize() {
        tvPageHeader.text = Competition.current.uniqueName + " - Member Services"
        entry.agilityClass.joinToParent()
        loCentre.visibility = View.VISIBLE
        loResults.visibility = View.GONE
        tvResults.movementMethod = ScrollingMovementMethod()

        edTeamName.setOnEditorActionListener { textView, action, keyEvent ->

            if (action == EditorInfo.IME_ACTION_DONE) {
                sendSignal(SignalCode.TEAM_NAME_ENTERED)
            }
            true
        }

    }


    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btFinished -> sendSignal(SignalCode.FINISHED)
                btBack -> sendSignal(SignalCode.BACK)
                btCheckout -> sendSignal(SignalCode.CHECKOUT)
                btResultsOK -> sendSignal(SignalCode.RESULTS_OK)
            }
        }
    }

    fun doSelectEntries() {
        val idTeamList = Dog.getIdTeamListEx(data.idDog, multiMemberClassList)
        if (idTeamList.isEmpty()) {
            entry.select("false")
        } else {
            entry.select(
                """
                    agilityClass.idCompetition=${Competition.current.id} AND
                    agilityClass.classDate=${today.sqlDate} AND
                    agilityClass.classCode IN (${ClassTemplate.directEntryList}) AND
                    entry.progress <> $PROGRESS_TRANSFERRED AND
                    entry.progress <> $PROGRESS_ENTRY_DELETED AND
                    entry.idTeam IN ($idTeamList)
                    """, "agilityClass.classCode, agilityClass.suffix"
            )
        }
    }

    override fun whenSignal(signal: Signal) {

        var tag = 0


        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                doSelectEntries()
                displayEntries()
                tvCompetitor.text = data.handlerName
                tvDog.text = "${data.cleanedPetName} (${data.dogCode})"
                action = Action.NONE
                if (stack.isEmpty()) {
                    selectMenu("main")
                } else {
                    selectMenu(stack.pop())
                }
                btCheckout.isEnabled = shoppingList.itemCount != 0
                btFinished.isEnabled = shoppingList.itemCount == 0
                signal.consumed()
            }
            SignalCode.START_ACTION -> {
                val _action = signal._payload as Action?
                if (_action != null) {
                    action = _action
                    when (action) {
                        Action.CHANGE_GRADE -> {
                            selectMenu("select_programme", "Change grade for")
                        }
                        Action.CHANGE_TEAM_MEMBER_HEIGHT -> {
                            selectMenu("select_team_member", "Change height for")
                        }
                        Action.REPLACE_TEAM_MEMBER -> {
                            val teamMember = signal._payload2 as Int?
                            if (teamMember != null) {
                                sendSignal(SignalCode.LOOKUP_TEAM, teamMember)
                            }
                        }
                        Action.ENTER_SPECIAL_CLASS -> {
                            val cursor = signal._payload2 as Int?
                            if (cursor != null) {
                                specialEntryClasses.cursor = cursor
                                if (shoppingList.havePendingEntry(specialEntryClasses.id, data.idDog)) {
                                    Global.services.popUp(
                                        "Warning",
                                        "You already have a pending entry for this dog. Once the entry has been paid for (press 'Checkout') you can make changes by returning here and selecting the 'Edit Entries' option"
                                    )
                                } else {
                                    isSplitPair = specialEntryClasses.template == ClassTemplate.SPLIT_PAIRS
                                    isTeam = specialEntryClasses.template == ClassTemplate.TEAM
                                    if (isSplitPair) {
                                        actionData.idAccount = data.idAccount
                                        actionData.idAgilityClass = specialEntryClasses.id
                                        actionData.idDog = data.idDog
                                        specialTeam.release()
                                        proposedTeam[0].idDog = data.idDog
                                        proposedTeam[0].idCompetitor = data.idCompetitor
                                        proposedTeam[0].petName = data.cleanedPetName
                                        proposedTeam[0].jumpHeightCode = data.jumpHeightCode
                                        reviewMode = false
                                        sendSignal(SignalCode.LOOKUP_TEAM, 2)
                                    } else if (isTeam) {
                                        actionData.idAccount = data.idAccount
                                        actionData.idAgilityClass = specialEntryClasses.id
                                        actionData.idDog = data.idDog
                                        specialTeam.release()
                                        proposedTeam[0].idDog = data.idDog
                                        proposedTeam[0].idCompetitor = data.idCompetitor
                                        proposedTeam[0].petName = data.cleanedPetName
                                        proposedTeam[0].jumpHeightCode = data.jumpHeightCode
                                        reviewMode = false
                                        sendSignal(SignalCode.LOOKUP_TEAM, 2)
                                    } else {
                                        updateHandlers()
                                        if (handlerTeam.rowCount > 0) {
                                            selectMenu("select_dog_competitor_team")
                                        } else {
                                            actionData.idTeamEntry = data.team.id
                                            selectMenu("select_entry_height")
                                        }
                                    }
                                }
                            }
                        }
                        Action.NONE -> {
                        }
                    }
                }
                signal.consumed()
            }
            SignalCode.PROGRAMME_SELECTED -> {
                val programme = signal._payload as Int?
                if (programme != null) {
                    actionData.selectedProgramme = programme
                    when (action) {
                        Action.CHANGE_GRADE -> {
                            when (actionData.selectedProgramme) {
                                PROGRAMME_PERFORMANCE -> selectMenu("select_grade", "Change performance grade to")
                                PROGRAMME_STEEPLECHASE -> selectMenu("select_grade", "Change steeplechase grade to")
                            }
                        }
                        Action.CHANGE_TEAM_MEMBER_HEIGHT -> {
                        }
                        Action.REPLACE_TEAM_MEMBER -> {
                        }
                        Action.ENTER_SPECIAL_CLASS -> {
                        }
                        Action.NONE -> {
                        }
                    }
                }
                signal.consumed()
            }
            SignalCode.GRADE_SELECTED -> {
                val gradeCode = signal._payload as String?
                if (gradeCode != null) {
                    selectedGrade = gradeCode
                    when (action) {
                        Action.CHANGE_GRADE -> {
                            clearMessages()
                            doHourglass(activity, "Thinking about it...",
                                {
                                    data.moveDogToGrade(
                                        selectedGrade,
                                        actionData.selectedProgramme,
                                        Competition.current.id
                                    )
                                },
                                {
                                    doSelectEntries()
                                    displayEntries()
                                    displayResults()
                                    action = Action.NONE
                                    stack.pop()
                                    stack.pop()
                                    selectMenu(stack.pop())
                                }
                            )

                        }
                        Action.CHANGE_TEAM_MEMBER_HEIGHT -> {
                        }
                        Action.REPLACE_TEAM_MEMBER -> {
                        }
                        Action.ENTER_SPECIAL_CLASS -> {
                        }
                        Action.NONE -> {
                        }
                    }
                }
                signal.consumed()
            }
            SignalCode.SHOW_ENTRY_OPTIONS -> {
                val cursor = signal._payload as Int?
                if (cursor != null) {
                    entry.cursor = cursor
                    data.idEntrySelected = entry.id
                    selectMenu("entry")
                }
                signal.consumed()
            }
            SignalCode.REMOVE_ENTRY -> {
                if (entry.agilityClass.isSpecialParent && entry.type == ENTRY_LATE_FEE) {
                    shoppingList.removeEntry(
                        entry.agilityClass.groupName,
                        entry.id,
                        entry.jumpHeightCode,
                        entry.agilityClass.lateEntryCredits,
                        entry.agilityClass.lateEntryFee
                    )
                    sendSignal(SignalCode.CHECKOUT)
                } else {
                    entry.removeEntry()
                    refreshMenu()
                }
                signal.consumed()
            }
            SignalCode.UN_REMOVE_ENTRY -> {
                if (entry.agilityClass.isSpecialParent) {
                    shoppingList.unRemoveEntry(
                        entry.agilityClass.groupName,
                        entry.id,
                        entry.jumpHeightCode,
                        entry.agilityClass.lateEntryCredits,
                        entry.agilityClass.lateEntryFee
                    )
                    sendSignal(SignalCode.CHECKOUT)
                } else {
                    entry.unRemoveEntry()
                    refreshMenu()
                }
                signal.consumed()
            }
            SignalCode.WITHDRAWN -> {
                entry.withdraw()
                refreshMenu()
                signal.consumed()
            }
            SignalCode.UN_WITHDRAWN -> {
                entry.unWithdraw()
                refreshMenu()
                signal.consumed()
            }
            SignalCode.TRANSFER_ENTRY -> {
                doHourglass(activity, "Thinking about it...",
                    { entry.convertToCredit() },
                    {
                        stack.pop()
                        stack.pop()
                        selectMenu(stack.pop())
                        sendSignal(SignalCode.RESET_FRAGMENT)
                    })
                signal.consumed()
            }
            SignalCode.DOG_COMPETITOR_SELECTED -> {
                val idTeam = signal._payload as Int?
                if (idTeam != null) {
                    actionData.idTeamEntry = idTeam
                    when (action) {
                        Action.CHANGE_GRADE -> {
                        }
                        Action.CHANGE_TEAM_MEMBER_HEIGHT -> {
                        }
                        Action.REPLACE_TEAM_MEMBER -> {
                        }
                        Action.ENTER_SPECIAL_CLASS -> {
                            selectMenu("select_entry_height")
                        }
                        Action.NONE -> {
                        }
                    }

                }
                signal.consumed()
            }
            SignalCode.MEMBER_HEIGHT_SELECTED -> {
                val jumpHeightCode = signal._payload as String?
                val member = signal._payload2 as TeamMember?
                if (jumpHeightCode!=null && member != null) {
                    member.jumpHeightCode = jumpHeightCode
                    selectMenu("review_team")
                }
            }
            SignalCode.HEIGHT_SELECTED -> {
                val jumpHeightCode = signal._payload as String?
                if (jumpHeightCode != null) {
                    val _tag = signal._payload2 as Int?
                    if (_tag != null) tag = _tag
                    selectedJumpHeight = jumpHeightCode
                    when (action) {
                        Action.CHANGE_GRADE -> {
                        }
                        Action.CHANGE_TEAM_MEMBER_HEIGHT -> {
                            entry.team.setHeightCode(actionData.teamMember, selectedJumpHeight)
                            entry.team.post()
                            entry.heightCode = entry.team.relayHeightCode
                            entry.jumpHeightCode = entry.team.relayHeightCode
                            entry.post()
                            action = Action.NONE
                            stack.pop()
                            stack.pop()
                            selectMenu(stack.pop())
                        }
                        Action.REPLACE_TEAM_MEMBER -> {
                        }
                        Action.ENTER_SPECIAL_CLASS -> {
                            if (isSplitPair) {
                                specialTeam.setRelayHeightCode(selectedJumpHeight)
                                sendSignal(SignalCode.ENTER_RELAY_EVENT)
                            } else if (isTeam) {
                                proposedTeam[actionData.teamMember - 1].jumpHeightCode = selectedJumpHeight
                                if (actionData.teamMember < 3) {
                                    actionData.teamMember++
                                    selectMenu("initial_team_member_height")
                                } else if (reviewMode) {
                                    selectMenu("review_team")
                                } else {
                                    selectMenu("team_name")
                                }
                            } else {
                                shoppingList.enterClass(
                                    specialEntryClasses.id, specialEntryClasses.name, actionData.idTeamEntry,
                                    selectedJumpHeight, specialEntryClasses.template.runUnits,
                                    specialEntryClasses.lateEntryFee
                                )
                                action = Action.NONE
                                sendSignal(SignalCode.CHECKOUT)
                            }
                        }
                        Action.NONE -> {
                        }
                    }
                }
                signal.consumed()
            }

            SignalCode.SWAP_PAIR -> {
                entry.team.swapMembers(1, 2)
                entry.heightCode = entry.team.relayHeightCode
                entry.jumpHeightCode = entry.team.relayHeightCode
                entry.post()
                selectMenu(stack.pop())
                signal.consumed()
            }
            SignalCode.PAIRS_ORDER_WRONG -> {
                specialTeam.swapMembers(1, 2)
                sendSignal(SignalCode.PAIRS_ORDER_CORRECT)
            }
            SignalCode.PAIRS_ORDER_CORRECT -> {
                //selectMenu("select_entry_height", "Select heights for ${specialTeam.description}")
                sendSignal(SignalCode.ENTER_RELAY_EVENT)
            }

            SignalCode.CHANGE_ENTRY_HEIGHT -> {
                val jumpHeightCode = signal._payload as String?
                if (jumpHeightCode != null) {
                    entry.changeHeight(jumpHeightCode)
                    stack.pop()
                    selectMenu(stack.pop())
                }
                signal.consumed()
            }
            SignalCode.SPECIAL_CLASS_HANDLER_CHANGE -> {
                val idTeam = signal._payload as Int?
                if (idTeam != null) {
                    entry.idTeam = idTeam
                    entry.post()
                    stack.pop()
                    selectMenu(stack.pop())
                }
                signal.consumed()
            }
            SignalCode.DO_MENU -> {
                val menu = signal._payload as String?
                if (menu != null) {
                    selectMenu(menu)
                }
                signal.consumed()
            }
            SignalCode.BACK -> {
                tvResults.text = ""
                if (!stack.empty()) {
                    stack.pop()
                }
                if (!stack.empty()) {
                    selectMenu(stack.pop())
                    signal.consumed()
                }
            }
            SignalCode.FINISHED -> {
                stack.clear()
                sendSignal(SignalCode.BACK)
                signal.consumed()
            }
            SignalCode.RESULTS_OK -> {
                loStatus.visibility = View.VISIBLE
                loResults.visibility = View.GONE
                signal.consumed()
            }
            SignalCode.TEAM_MEMBER_SELECTED -> {
                val teamMember = signal._payload as Int?
                if (teamMember != null) {
                    actionData.teamMember = teamMember
                    when (action) {
                        Action.CHANGE_GRADE -> {
                        }
                        Action.CHANGE_TEAM_MEMBER_HEIGHT -> {
                            selectMenu("select_team_member_height")
                        }
                        Action.REPLACE_TEAM_MEMBER -> {
                        }
                        Action.ENTER_SPECIAL_CLASS -> {
                        }
                        Action.NONE -> {
                        }
                    }

                }
                signal.consumed()
            }
            SignalCode.TEAM_LOOKED_UP -> {
                val teamMember = signal._payload as Int?
                if (teamMember != null) {
                    actionData.teamMember = teamMember
                }
                if (isSplitPair) {
                    specialTeam.selectUkaPair(
                        actionData.idAccount,
                        actionData.idAgilityClass,
                        actionData.idDog,
                        data.idDog,
                        actionData.idDogSelected,
                        data.idCompetitor,
                        actionData.idCompetitorSelected,
                        "UKA550",
                        "UKA550"
                    )
                    selectMenu("choose_agility")
                } else if (isTeam) {
                    proposedTeam[actionData.teamMember - 1].idDog = actionData.idDogSelected
                    proposedTeam[actionData.teamMember - 1].idCompetitor = actionData.idCompetitorSelected
                    proposedTeam[actionData.teamMember - 1].petName = actionData.petNameSelected
                    proposedTeam[actionData.teamMember - 1].jumpHeightCode = actionData.selectedJumpHeight
                    if (proposedTeam[2].idDog == 0) {
                        sendSignal(SignalCode.LOOKUP_TEAM, 3)
                    } else {
                        selectMenu("team_name")
                    }
                }
                when (action) {
                    Action.CHANGE_GRADE -> {
                    }
                    Action.CHANGE_TEAM_MEMBER_HEIGHT -> {
                    }
                    Action.REPLACE_TEAM_MEMBER -> {
                        if (entry.team.type == TEAM_MULTIPLE && entry.team.classCode == ClassTemplate.SPLIT_PAIRS.code) {
                            entry.team.replaceUkaPairMember(
                                teamMember ?: 1,
                                actionData.idDogSelected,
                                actionData.idCompetitorSelected
                            )
                        } else if (entry.team.type == TEAM_MULTIPLE && entry.team.classCode == ClassTemplate.TEAM.code) {
                            when (teamMember) {
                                1 -> {
                                    entry.team.reviseUkaTeam(
                                        actionData.idDogSelected,
                                        entry.team.getIdDog(2),
                                        entry.team.getIdDog(3),
                                        actionData.idCompetitorSelected,
                                        entry.team.getIdCompetitor(2),
                                        entry.team.getIdCompetitor(3),
                                        entry.team.getHeightCode(1),
                                        entry.team.getHeightCode(2),
                                        entry.team.getHeightCode(3),
                                        entry.team.teamName
                                    )
                                }
                                2 -> {
                                    entry.team.reviseUkaTeam(
                                        entry.team.idDog,
                                        actionData.idDogSelected,
                                        entry.team.getIdDog(3),
                                        entry.team.idCompetitor,
                                        actionData.idCompetitorSelected,
                                        entry.team.getIdCompetitor(3),
                                        entry.team.getHeightCode(1),
                                        entry.team.getHeightCode(2),
                                        entry.team.getHeightCode(3),
                                        entry.team.teamName
                                    )
                                }
                                3 -> {
                                    entry.team.reviseUkaTeam(
                                        entry.team.idDog,
                                        entry.team.getIdDog(2),
                                        actionData.idDogSelected,
                                        entry.team.idCompetitor,
                                        entry.team.getIdCompetitor(2),
                                        actionData.idCompetitorSelected,
                                        entry.team.getHeightCode(1),
                                        entry.team.getHeightCode(2),
                                        entry.team.getHeightCode(3),
                                        entry.team.teamName
                                    )
                                }
                            }
                        }
                        entry.team.post()
                        entry.heightCode = entry.team.relayHeightCode
                        entry.jumpHeightCode = entry.team.relayHeightCode
                        entry.post()
                        val cursor = entry.cursor
                        doSelectEntries()
                        entry.cursor = cursor
                        action = Action.NONE
                        selectMenu(stack.pop())
                    }
                    Action.ENTER_SPECIAL_CLASS -> {
                    }
                    Action.NONE -> {
                    }
                }
            }
            SignalCode.LOOKUP_TEAM -> {
                val intent = Intent(activity, SelectTeam::class.java)
                val teamMember = signal._payload as Int?
                if (teamMember != null) {
                    actionData.teamMember = teamMember
                }
                intent.putExtra("title", tvPageHeader.text)
                if (isSplitPair) {
                    intent.putExtra("hint", "Enter Dog ID for ${data.cleanedPetName.possessive} partner")
                } else if (isTeam) {
                    intent.putExtra("hint", "Enter Dog ID for dog number $teamMember")
                } else {
                    intent.putExtra("hint", "Enter Dog ID")
                }
                intent.putExtra("autoOK", false)

                doActivityForResult(intent, 999) { resultCode, intent1 ->
                    if (resultCode == FragmentActivity.RESULT_OK) {
                        actionData.idDogSelected = intent1.getIntExtra("idDog", 0)
                        actionData.idCompetitorSelected = intent1.getIntExtra("idCompetitor", 0)
                        actionData.petNameSelected = intent1.getStringExtra("petName") ?: ""
                        actionData.selectedJumpHeight = intent1.getStringExtra("ukaHeightCodePerformance") ?: ""
                        sendSignal(SignalCode.TEAM_LOOKED_UP, teamMember)
                    } else {
                        selectMenu(stack.pop())
                    }
                }
                signal.consumed()
            }
            SignalCode.TEAM_NAME_ENTERED -> {
                actionData.proposedTeamName = edTeamName.text.toString()
                hideKeyboard()
                when (action) {
                    Action.CHANGE_GRADE -> {
                    }
                    Action.CHANGE_TEAM_MEMBER_HEIGHT -> {
                    }
                    Action.REPLACE_TEAM_MEMBER -> {
                    }
                    Action.ENTER_SPECIAL_CLASS -> {
                        reviewMode = true
                        selectMenu("review_team")
                    }
                    Action.NONE -> {
                        entry.team.teamName = actionData.proposedTeamName
                        entry.team.post()
                        stack.pop()
                        selectMenu(stack.pop())
                    }
                }
                signal.consumed()
            }
            /*
            SignalCode.REVIEW_HEIGHTS -> {
                actionData.teamMember = 1
                selectMenu("initial_team_member_height")
            }
            
             */
            SignalCode.REVIEW_HEIGHTS -> {
                selectMenu("review_heights")
            }
            SignalCode.REVIEW_HEIGHT -> {
                val teamMember = signal._payload as Int?
                if (teamMember != null) {
                    actionData.teamMember = teamMember
                    selectMenu("select_proposed_team_member_height")
                }
            }
            SignalCode.ENTER_RELAY_EVENT -> {
                if (isTeam) {
                    specialTeam.selectUkaTeam(
                        actionData.idAccount, actionData.idAgilityClass, actionData.idDog,
                        proposedTeam[0].idDog, proposedTeam[1].idDog, proposedTeam[2].idDog,
                        proposedTeam[0].idCompetitor, proposedTeam[1].idCompetitor, proposedTeam[2].idCompetitor,
                        proposedTeam[0].jumpHeightCode, proposedTeam[1].jumpHeightCode, proposedTeam[2].jumpHeightCode,
                        actionData.proposedTeamName
                    )
                }
                shoppingList.enterClass(
                    specialEntryClasses.id, specialEntryClasses.name, specialTeam.id,
                    specialTeam.relayHeightCode, specialEntryClasses.template.runUnits,
                    specialEntryClasses.lateEntryFee
                )
                action = Action.NONE
                sendSignal(SignalCode.CHECKOUT)
            }
            SignalCode.ABANDON_RELAY_EVENT -> {
                whenYes("Question", "Are you sure you want to abandon this entry") {
                    action = Action.NONE
                    top()
                }
            }
            else -> {
                doNothing()
            }

        }
    }


    fun isEnteredInClass(idAgilityClass: Int): Boolean {
        entry.beforeFirst()
        while (entry.next()) {
            if (entry.idAgilityClass == idAgilityClass) {
                return true
            }
        }
        return false
    }

    fun selectMenu(selector: String, title: String = "") {
        selectMenu(MenuItem(selector, title))
    }

    fun refreshMenu() {
        selectMenu(stack.pop())
    }

    fun top() {
        stack.clear()
        selectMenu("main")
    }

    fun selectMenu(menu: MenuItem) {
        stack.push(menu)
        loMenu.removeAllViews()
        loMenu.columnCount = 1
        goneIf(
            !menu.selector.oneOf(
                "main", "select_programme", "select_grade", "entries", "select_dog_competitor_team",
                "select_entry_height", "handlers", "choose_agility"
            ), tvEntries
        )
        goneIf(
            !menu.selector.oneOf("entry", "change_height", "select_team_member", "select_team_member_height"),
            tvEntry
        )
        goneIf(!menu.selector.oneOf("team_name"), loEditTeamName)

        goneIf(menu.selector.oneOf("team_name"), loCentre)
        disableIf(action != Action.NONE, btFinished)

        if (action == Action.ENTER_SPECIAL_CLASS && isTeam) {
            tvProposedTeam.text = proposedTeamDescription
            tvProposedTeam.visibility = View.VISIBLE
        } else {
            tvProposedTeam.visibility = View.GONE
        }

        if (tvEntry.visibility == View.VISIBLE) {
            tvEntry.text = getEntryText()
        }

        when (menu.selector) {
            "main" -> {
                tvSubTitle.text = "Options"
                addMenuButton(loMenu, "Change Grade", SignalCode.START_ACTION, Action.CHANGE_GRADE)
                if (entry.rowCount > 0) {
                    addMenuButton(loMenu, "Edit Entries", SignalCode.DO_MENU, "entries")
                }

                var count = 0
                specialEntryClasses.beforeFirst()
                while (specialEntryClasses.next()) {
                    if (!specialEntryClasses.closedForLateEntries && (entry.rowCount == 0 || !isEnteredInClass(
                            specialEntryClasses.id
                        ))
                    ) {
                        count++
                    }
                }
                if (count > 0) {
                    addMenuButton(loMenu, "Special Classes", SignalCode.DO_MENU, "enter_spacial_class")
                }
            }
            "enter_spacial_class" -> {
                tvSubTitle.text = "Special Classes"
                specialEntryClasses.beforeFirst()
                while (specialEntryClasses.next()) {
                    if (!specialEntryClasses.closedForLateEntries && (entry.rowCount == 0 || !isEnteredInClass(
                            specialEntryClasses.id
                        ))
                    ) {
                        addMenuButton(
                            loMenu,
                            "Enter ${specialEntryClasses.extendedGroupName}",
                            SignalCode.START_ACTION,
                            Action.ENTER_SPECIAL_CLASS,
                            specialEntryClasses.cursor,
                            buttonWidth = 300
                        )
                    }
                }
            }
            "select_programme" -> {
                tvSubTitle.text = "Select programme"
                addMenuButton(loMenu, "Performance", SignalCode.PROGRAMME_SELECTED, PROGRAMME_PERFORMANCE)
                addMenuButton(loMenu, "Steeplechase", SignalCode.PROGRAMME_SELECTED, PROGRAMME_STEEPLECHASE)
            }
            "select_grade" -> {
                tvSubTitle.text = "Select grade"
                addMenuButton(loMenu, "Beginners", SignalCode.GRADE_SELECTED, "UKA01")
                addMenuButton(loMenu, "Novice", SignalCode.GRADE_SELECTED, "UKA02")
                addMenuButton(loMenu, "Senior", SignalCode.GRADE_SELECTED, "UKA03")
                addMenuButton(loMenu, "Champ", SignalCode.GRADE_SELECTED, "UKA04")
            }
            "entry" -> {
                tvSubTitle.text = "Options for Entry"
                if (entry.canRemoveEntry) {
                    addMenuButton(loMenu, "Remove Entry", SignalCode.REMOVE_ENTRY)
                }
                if (entry.canUnRemoveEntry) {
                    addMenuButton(loMenu, "Restore Entry", SignalCode.UN_REMOVE_ENTRY)
                }
                if (entry.canWithdraw) {
                    addMenuButton(loMenu, "Withdraw", SignalCode.WITHDRAWN)
                }
                if (entry.canUnWithdraw) {
                    addMenuButton(loMenu, "Un-Withdraw", SignalCode.UN_WITHDRAWN)
                }
                if (entry.agilityClass.template == ClassTemplate.TEAM) {
                    addMenuButton(loMenu, "Edit Team Name", SignalCode.DO_MENU, "team_name")
                }

                if (entry.canSwapDogs) {
                    addMenuButton(loMenu, "Swap Dogs Around", SignalCode.SWAP_PAIR)
                }
                if (entry.canChangeEntry) {
                    if (entry.agilityClass.teamSize > 1) {
                        addMenuButton(
                            loMenu,
                            "Change Dog Height",
                            SignalCode.START_ACTION,
                            Action.CHANGE_TEAM_MEMBER_HEIGHT
                        )
                        for (teamMember in 1..entry.agilityClass.teamSize) {
                            if (entry.team.getIdDog(teamMember) != data.idDog) {
                                addMenuButton(
                                    loMenu,
                                    "Replace ${entry.team.getDogName(teamMember)}",
                                    SignalCode.START_ACTION,
                                    Action.REPLACE_TEAM_MEMBER,
                                    teamMember
                                )
                            }
                        }
                    } else {
                        addMenuButton(loMenu, "Change Height", SignalCode.DO_MENU, "change_height")
                        addMenuButton(loMenu, "Change Handler", SignalCode.CHANGE_HANDLER)
                    }
                }
                if (entry.canConvert) {
                    addMenuButton(loMenu, "Transfer to LE", SignalCode.TRANSFER_ENTRY)
                }
            }
            "change_height" -> {
                tvSubTitle.text = "Select New Height"
                if (entry.canChangeHeight) {
                    val height = Height()
                    val heightList = entry.agilityClass.individualEntryHeights
                    height.select("heightCode IN (${heightList.split(",").asQuotedList()})", "heightCode DESC")
                    while (height.next()) {
                        addMenuButton(loMenu, "${height.name}", SignalCode.CHANGE_ENTRY_HEIGHT, height.code).isEnabled =
                                (height.code != entry.jumpHeightCode)
                    }
                }
            }
            "entries" -> {
                loMenu.columnCount = if (entry.rowCount > 5) 2 else 1
                tvSubTitle.text = "Select Class"
                entry.beforeFirst()
                while (entry.next()) {
                    if (entry.rowCount > 5) {
                        addMenuButton(
                            loMenu,
                            entry.agilityClass.shortName,
                            SignalCode.SHOW_ENTRY_OPTIONS,
                            entry.cursor,
                            buttonWidth = 240
                        )
                    } else {
                        addMenuButton(
                            loMenu,
                            entry.agilityClass.groupName,
                            SignalCode.SHOW_ENTRY_OPTIONS,
                            entry.cursor,
                            buttonWidth = 350
                        )
                    }
                }
            }
            "select_dog_competitor_team" -> {
                tvSubTitle.text = "Select Handler for ${specialEntryClasses.name}"
                addMenuButton(
                    loMenu,
                    "${data.team.getCompetitorName(1)}",
                    SignalCode.DOG_COMPETITOR_SELECTED,
                    data.team.id,
                    350
                )
                while (handlerTeam.next()) {
                    addMenuButton(
                        loMenu,
                        "${handlerTeam.getCompetitorName(1)}",
                        SignalCode.DOG_COMPETITOR_SELECTED,
                        handlerTeam.id,
                        350
                    )
                }
            }
            "select_entry_height" -> {
                tvSubTitle.text = "Select Height for ${specialEntryClasses.name}"
                val height = Height()
                var heightList = specialEntryClasses.individualEntryHeights
                height.select("heightCode IN (${heightList.split(",").asQuotedList()})", "heightCode DESC")
                while (height.next()) {
                    addMenuButton(loMenu, height.name, SignalCode.HEIGHT_SELECTED, height.code)
                }
            }
            "select_team_member_height" -> {
                tvSubTitle.text = "Select height for ${entry.team.getDogName(actionData.teamMember)}"
                val height = Height()
                var heightList = entry.agilityClass.individualEntryHeights
                if (entry.agilityClass.template == ClassTemplate.SPLIT_PAIRS) {
                    heightList = "UKA300,UKA550"
                }
                height.select("heightCode IN (${heightList.split(",").asQuotedList()})", "heightCode DESC")
                while (height.next()) {
                    addMenuButton(loMenu, "${height.name}", SignalCode.HEIGHT_SELECTED, height.code, menu.tag)
                }
            }

            "select_proposed_team_member_height" -> {
                val member = proposedTeam[actionData.teamMember]
                tvSubTitle.text = "Select height for ${member.petName}"
                val height = Height()
                var heightList = entry.agilityClass.individualEntryHeights
                if (entry.agilityClass.template == ClassTemplate.SPLIT_PAIRS) {
                    heightList = "UKA300,UKA550"
                }
                height.select("heightCode IN (${heightList.split(",").asQuotedList()})", "heightCode DESC")
                while (height.next()) {
                    addMenuButton(loMenu, "${height.name}", SignalCode.MEMBER_HEIGHT_SELECTED, height.code, member)
                }
            }

            "initial_team_member_height" -> {
                tvSubTitle.text = "Select height for ${proposedTeam[actionData.teamMember - 1].petName}"
                val height = Height()
                var heightList = specialEntryClasses.individualEntryHeights
                if (entry.agilityClass.template == ClassTemplate.SPLIT_PAIRS) {
                    heightList = "UKA300,UKA550"
                }
                height.select("heightCode IN (${heightList.split(",").asQuotedList()})", "heightCode DESC")
                while (height.next()) {
                    addMenuButton(loMenu, "${height.name}", SignalCode.HEIGHT_SELECTED, height.code, menu.tag)
                }
            }
            "select_team_member" -> {
                tvSubTitle.text = "Select dog"
                for (i in 1..entry.team.memberCount) {
                    addMenuButton(loMenu, entry.team.getDogName(i), SignalCode.TEAM_MEMBER_SELECTED, i, 350)
                }
            }
            "choose_agility" -> {
                tvSubTitle.text = "Which is the agility dog?"
                addMenuButton(loMenu, specialTeam.getDogName(1), SignalCode.PAIRS_ORDER_CORRECT, 1)
                addMenuButton(loMenu, specialTeam.getDogName(2), SignalCode.PAIRS_ORDER_WRONG, 2)
            }
            "team_name" -> {
                if (action == Action.ENTER_SPECIAL_CLASS) {
                    edTeamName.setText(actionData.proposedTeamName)
                } else {
                    edTeamName.setText(data.entry.team.teamName)
                }
                edTeamName.requestFocus()
                showKeyboard()
            }
            "review_team" -> {
                tvSubTitle.text = "Review team"
                addMenuButton(loMenu, "Edit Team Name", SignalCode.DO_MENU, "team_name")
                //addMenuButton(loMenu, "Change Dog Height", SignalCode.START_ACTION, Action.CHANGE_TEAM_MEMBER_HEIGHT)
                addMenuButton(loMenu, "Change Heights", SignalCode.REVIEW_HEIGHTS)
                addMenuButton(loMenu, "Abandon", SignalCode.ABANDON_RELAY_EVENT)
                addMenuButton(loMenu, "OK - Pay", SignalCode.ENTER_RELAY_EVENT)
            }
            "review_heights" -> {
                tvSubTitle.text = "Change height for"
                for (member in proposedTeam) {
                    addMenuButton(loMenu, member.petName, SignalCode.REVIEW_HEIGHT, proposedTeam.indexOf(member))
                }
            }
        }
        goneIf(loMenu.childCount == 0, loCentre)

        if (menu.title.isNotEmpty()) {
            tvSubTitle.text = menu.title
        }
    }

    private fun getEntryText(): String {
        val entryText = StringBuilder("")
        debug(
            "DogMenuUka.DogMenuUka",
            "entry.idTeam=${entry.idTeam}, data.team.id=${data.team.id}, entry.rowCount=${entry.rowCount}, entry.getCursor()=${entry.cursor}"
        )
        if (entry.idTeam != data.team.id) {
            if (entry.team.memberCount > 1) {
                entryText.append("${entry.agilityClass.groupName}: ${entry.team.fullDescription}")
            } else {
                entryText.append(
                    "${entry.agilityClass.groupName}: ${entry.team.fullDescription} (${Height.getHeightName(
                        entry.jumpHeightCode
                    )})"
                )
            }
        } else {
            entryText.append("${entry.agilityClass.groupName} (${Height.getHeightName(entry.jumpHeightCode)})")
        }
        if (entry.isLateEntry) {
            entryText.append(" LE")
        }
        when (entry.progress) {
            PROGRESS_REMOVED -> entryText.append(" - REMOVED")
            PROGRESS_WITHDRAWN -> entryText.append(" - WITHDRAWN")
            PROGRESS_RUN -> entryText.append(" - RUN")
            PROGRESS_CONVERTED_TO_CREDIT -> entryText.append(" - DELETED")
        }
        return entryText.toString()
    }

    private fun displayEntries() {
        val entries = StringBuilder("")
        var cursor = -1
        if (entry.rowCount == 0) {
            entries.append("No classes entered")
        } else {
            while (entry.next()) {
                entries.commaAppend(getEntryText())
                if (entry.id == data.idEntrySelected) {
                    cursor = entry.cursor
                }
            }
        }
        tvEntries.text = "TODAY'S ENTRIES: " + entries.toString()
        if (cursor > -1) {
            entry.cursor = cursor
        }
    }

    private fun displayResults() {
        var results = "RESULTS:"
        if (messages.size == 0) {
            results += "\n  nothing to do"
        } else {
            for (message in messages) {
                results += "\n  " + message
            }
        }
        tvResults.text = results
        loStatus.visibility = View.GONE
        loResults.visibility = View.VISIBLE

    }
}
