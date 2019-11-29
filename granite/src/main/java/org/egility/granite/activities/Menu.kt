/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import android.content.Intent
import android.view.View
import kotlinx.android.synthetic.main.menu.*
import org.egility.android.BaseActivity
import org.egility.android.tools.*
import org.egility.android.views.QuickButton
import org.egility.granite.R
import org.egility.granite.utils.TabletInfo
import org.egility.granite.utils.showLateEntryStats
import org.egility.granite.utils.updateApp
import org.egility.library.api.Api
import org.egility.library.api.Api.lockEntries
import org.egility.library.database.DbQuery
import org.egility.library.dbobject.*
import org.egility.library.general.*
import java.util.*

class Menu : BaseActivity(R.layout.menu) {

    private var stack = Stack<String>()
    private var proposedActivity = DEVICE_RING_PARTY
    private var deviceWasAssigned = false
    private var readyToRun = true
    private var killSystem = false

    var _ringNumbers: DbQuery? = null
    var _ringNumbersDate = nullDate
    val ringNumbers: DbQuery
        get() {
            if (_ringNumbers == null || _ringNumbersDate != today) {
                _ringNumbersDate = today
                _ringNumbers = DbQuery(
                    """
                SELECT DISTINCT ringNumber FROM agilityClass
                WHERE ringNumber>0 AND idCompetition=${Competition.current.id} AND classDate=${today.sqlDate}
                ORDER BY ringNumber
            """
                )
            }
            return _ringNumbers ?: throw Wobbly("unable to get ringNumbers")
        }

    var _days: DbQuery? = null
    var _daysDate = nullDate
    val days: DbQuery
        get() {
            if (_days == null || _daysDate != today) {
                _daysDate = today
                val daysQuery = DbQuery(
                    """
                    SELECT DISTINCT date FROM competitionDay
                    WHERE idCompetition=${Competition.current.id} AND dayType=$DAY_REGULAR AND date<=${today.sqlDate}
                    ORDER BY date DESC
                """
                )
                if (daysQuery.rowCount == 0) {
                    daysQuery.load(
                        """
                        SELECT DISTINCT date FROM competitionDay
                        WHERE idCompetition=${Competition.current.id} AND dayType=$DAY_REGULAR
                        ORDER BY date DESC
                        LIMIT 1
                    """
                    )
                }
                _days = daysQuery
            }
            return _days ?: throw Wobbly("unable to get days")
        }

    private val ring: Ring
        get() = ringPartyData.ring


    private val agilityClass: AgilityClass
        get() = ring.agilityClass


    override fun whenInitialize() {
        if (!Global.haveConnection && TabletInfo.activity.oneOf(DEVICE_SYSTEM_MANAGER, DEVICE_UNASSIGNED)) {
            selectMenu("system_manager_restricted")
            pulseRate = 10
        } else {
            TabletInfo.updateDevice()
            if (Device.assigned && Device.thisDevice.activity != DEVICE_UNASSIGNED) {
                when (Device.thisDevice.activity) {
                    DEVICE_RING_PARTY -> {
                        ringPartyData.setRingNumber(Device.thisDevice.ringNumber)
                        if (ringNumbers.rowCount > 0) selectMenu("ring_party") else selectMenu("usage")
                    }
                    DEVICE_SCOREBOARD -> {
                        ringPartyData.setRingNumber(Device.thisDevice.ringNumber)
                        if (ringNumbers.rowCount > 0) sendSignal(
                            SignalCode.START_ACTIVITY,
                            ScoreBoard::class.java
                        ) else selectMenu("usage")
                    }
                    DEVICE_SECRETARY -> selectMenu("secretary")
                    DEVICE_SYSTEM_MANAGER -> selectMenu("system_manager")
                }
            } else {
                TabletInfo.saveDeviceConfig(this)
                TabletInfo.updateDevice()
                stack.clear()
                selectMenu("usage")
            }
            pulseRate = 30
        }
    }

    fun refreshHeading() {
        if (Global.haveConnection) {
            debug("Menu", "Refresh headings")
            ring.refresh()
            tvIpAddress.text = Device.thisDevice.ipAddress
            AndroidUtils.invisibleIf(Device.thisDevice.activity != DEVICE_SYSTEM_MANAGER, tvIpAddress)
            if ((Device.thisDevice.activity == DEVICE_RING_PARTY && Device.assigned) || 
                (Device.thisDevice.activity == DEVICE_SYSTEM_MANAGER && Device.assigned && Global.activityName.oneOf("ring_party", "booking_in", "ring_manage"))) {
                tvPageHeader.text = Competition.current.uniqueName + " - " + ring.title
                tvSubHeading.text = ring.classText
            } else {
                tvPageHeader.text = Competition.current.uniqueName
                tvSubHeading.text = "e-gility system (${now.dateText})"
            }
            debug("Menu", "Headings refreshed")
        } else {
            tvPageHeader.text = "Troubleshooting"
            tvSubHeading.text = "e-gility system (${now.dateText})"
        }
    }

    override fun whenResumeFromPause() {
        if (stack.isEmpty()) {
            finish()
        } else if (stack.peek() == "booking_in") {
            refreshHeading()
            sendSignal(SignalCode.BACK)
        } else if (stack.peek() == "ring_party") {
            ringPartyData.syncRing()
            refreshHeading()
        } else {
            refreshHeading()
        }
    }

    override fun whenFinalize() {
        AndroidServices.close()
    }


    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        }
        super.whenClick(view)
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.SELECT_MENU -> {
                val menu = signal._payload as? String
                if (menu != null) {
                    selectMenu(menu)
                    signal.consumed()
                }
            }
            SignalCode.START_ACTIVITY -> {
                val activityClass = signal._payload as Class<*>?
                if (activityClass != null) {
                    if (activityClass == Scrime::class.java && !agilityClass.hasCourseTime()) {
                        Global.services.msgYesNo(
                            "Warning",
                            "You have not entered course times for this class. Would you like to do it now?"
                        ) { isYes ->
                            if (isYes) {
                                doActivity(EnterCourseTimes::class.java)
                            } else {
                                Global.services.popUp(
                                    "Information",
                                    "Try later when the course times have been entered"
                                )
                            }
                        }
                    } else {
                        doActivity(activityClass)
                    }
                }
            }
            SignalCode.CHECK_PIN -> {
                val pinType = signal._payload as Int?
                if (pinType != null) {
                    val intent = Intent(this, EnterPin::class.java)
                    intent.putExtra("pinType", pinType)
                    startActivityForResult(intent, pinType)
                }
            }
            SignalCode.BACK -> {
                if (stack.size == 1) {
                    whenYes("Question", "Do you really want to exit?") {
                        stack.pop()
                        super.whenSignal(signal)
                    }
                } else {
                    stack.pop()
                    selectMenu(stack.pop())
                    signal.consumed()
                }
            }
            SignalCode.UPDATE_SOFTWARE -> {
                updateApp()
            }
            SignalCode.SHOW_STATS -> {
                showLateEntryStats()
            }
            SignalCode.PRINT_LATE_ENTRIES -> {
                doHourglass(
                    this,
                    "Preparing Report",
                    { Reports.printLateEntryCredits(control.idCompetition, Global.endOfDayDate) },
                    {})
            }
            SignalCode.PRINT_COMPLIMENTARY_ENTRIES -> {
                doHourglass(
                    this,
                    "Preparing Report",
                    { Reports.printLateEntryFree(control.idCompetition, Global.endOfDayDate) },
                    {})
            }
            SignalCode.PRINT_COMPLIMENTARY_USED -> {
                doHourglass(
                    this,
                    "Preparing Report",
                    { Reports.printComplimentaryCreditsUsed(control.idCompetition) },
                    {})
            }
            SignalCode.PRINT_ACCOUNT_PAYMENTS -> {
                doHourglass(this, "Preparing Report", { Reports.printAccountPayments(control.idCompetition) }, {})
            }
            SignalCode.PRINT_SPECIAL_ENTRIES -> {
                doHourglass(
                    this,
                    "Preparing Report",
                    { Reports.printLateEntrySpecial(control.idCompetition, Global.endOfDayDate) },
                    {})
            }
            SignalCode.PRINT_CHEQUES_LIST -> {
                doHourglass(this, "Preparing Report", { Reports.printChequeList(control.idCompetition) }, {})
            }
            SignalCode.PULSE -> {
                if (Global.haveConnection) {
                    var device =
                        "${TabletInfo.deviceName}/${NetworkObject.acuTag}, Bat: ${AndroidUtils.getBatteryText()}, Sig: ${NetworkObject.signalStrength}%, ${now.timeText}"
                    if (!AndroidUtils.isFire && AndroidUtils.getBrightness() > 0) {
                        device = "BACKLIGHT! " + device
                    }
                    tvPageFooter.text = device

                    if (Device.thisActivity == DEVICE_RING_PARTY || (stack.size > 0 && stack.peek() == "ring_party")) {
                        ringPartyData.syncRing()
                        val menu = stack.pop()
                        selectMenu(menu)
                    }

                    debug("menu", "pulse: $device, title: ${tvPageHeader.text}")

                    if (deviceWasAssigned && !Device.assigned) {
                        selectMenu("usage")
                    }
                } else {
                    var device =
                        "${TabletInfo.deviceName}/${NetworkObject.acuTag}, Bat: ${AndroidUtils.getBatteryText()}, Sig: ${NetworkObject.signalStrength}%, ${now.timeText}"
                    if (!AndroidUtils.isFire && AndroidUtils.getBrightness() > 0) {
                        device = "BACKLIGHT! " + device
                    }
                    tvPageFooter.text = device
                }

            }
            SignalCode.BATTERY -> {
                sendSignal(SignalCode.PULSE)
            }
            SignalCode.DEVICE_RING_PARTY -> {
                proposedActivity = DEVICE_RING_PARTY
                selectMenu("choose_ring")
            }
            SignalCode.DEVICE_SCOREBOARD -> {
                proposedActivity = DEVICE_SCOREBOARD
                selectMenu("choose_ring")
            }
            SignalCode.DEVICE_SECRETARY -> {
                stack.clear()
                Device.setActivity(DEVICE_SECRETARY, activityDate = Competition.current.dateEnd)
                TabletInfo.saveDeviceConfig(this)
                finish()
            }
            SignalCode.DEVICE_SYSTEM_MANAGER -> {
                stack.clear()
                Device.setActivity(DEVICE_SYSTEM_MANAGER, activityDate = Competition.current.dateEnd)
                TabletInfo.saveDeviceConfig(this)
                finish()
            }
            SignalCode.MANAGE_COMBINED_CLASS -> {
                val idAgilityClass = signal._payload as? Int
                if (idAgilityClass != null) {
                    ClassData.agilityClass.find(idAgilityClass)
                }
                doActivity(CombinedClassStatus::class.java)
            }
            SignalCode.RING_SELECTED -> {
                val ringNumber = signal._payload as? Int
                if (ringNumber != null) {
                    ringPartyData.setRingNumber(ringNumber, true)
                    if (Device.thisActivity == DEVICE_SYSTEM_MANAGER) {
                        when (proposedActivity) {
                            DEVICE_RING_PARTY -> selectMenu("ring_party")
                            DEVICE_SCOREBOARD -> sendSignal(SignalCode.START_ACTIVITY, ScoreBoard::class.java)
                        }
                    } else {
                        stack.clear()
                        Device.setActivity(proposedActivity, ringNumber, activityDate = Competition.current.dateEnd)
                        TabletInfo.saveDeviceConfig(this)
                        finish()
                    }
                }
            }
            SignalCode.CRASH -> {
                //displayPanicDialog()
                crash1()
            }
            SignalCode.SWITCH_SHOW -> {
                val idCompetition = signal._payload as Int?
                if (idCompetition != null) {
                    Competition.current.seek(idCompetition) {
                        control.idCompetition = idCompetition
                        if (control.effectiveDate.isNotEmpty()) control.effectiveDate = dateStart
                        control.post()
                        finish()
                    }
                }
            }
            SignalCode.SET_SYSTEM_DATE -> {
                val acuDate = Api.networkDate()
                whenYes(
                    "Question",
                    "Do you want to change the time on ${NetworkObject.acuTag} from:\n\n${acuDate.fullDateTimeText} to\n${realNow.fullDateTimeText}?"
                ) {
                    Api.networkDate(realNow)
                }
            }
            SignalCode.POPUP -> {
                val message = signal._payload as String?
                if (message != null) {
                    popUp("Warning", message)
                }
            }
            SignalCode.MENU_QUERY_SCRIME -> {
                val activityClass = signal._payload as Class<*>?
                if (activityClass != null) {
                    agilityClass.refresh()
                    when (agilityClass.progress) {
                        in CLASS_PENDING..CLASS_CLOSED_FOR_LUNCH -> {
                            msgYesNo("Question", "Is the class ready to start running?") { isYes ->
                                if (isYes) {
                                    agilityClass.progress = CLASS_RUNNING
                                    agilityClass.post()
                                    sendSignal(SignalCode.START_ACTIVITY, activityClass)
                                } else {
                                    sendSignal(
                                        SignalCode.POPUP,
                                        "You cannot scrime until the class is ready to run",
                                        queued = true
                                    )
                                }
                            }
                        }
                        CLASS_CLOSED -> {
                            popUp("Warning", "You scrime once the class has closed")
                        }
                        else -> {
                            sendSignal(SignalCode.START_ACTIVITY, activityClass)
                        }
                    }
                }
            }
            SignalCode.MENU_QUERY_PROGRESS -> {
                val activityClass = signal._payload as Class<*>?
                if (activityClass != null) {
                    agilityClass.refresh()
                    when (agilityClass.progress) {
                        CLASS_PENDING, CLASS_PREPARING -> {
                            msgYesNo("Question", "Is the class ready for walking?") { isYes ->
                                if (isYes) {
                                    agilityClass.progress = CLASS_WALKING
                                    agilityClass.post()
                                }
                                sendSignal(SignalCode.START_ACTIVITY, activityClass)
                            }
                        }
                        CLASS_CLOSED -> {
                            popUp("Warning", "You perform this operation once the class has closed")
                        }
                        else -> {
                            sendSignal(SignalCode.START_ACTIVITY, activityClass)
                        }
                    }
                }
            }
            SignalCode.ACCOUNTING_DAY_SELECTED -> {
                val _endOfDayDate = signal._payload as Date?
                if (_endOfDayDate != null) {
                    Global.endOfDayDate = _endOfDayDate
                    selectMenu("secretary_accounts")
                }
            }
            SignalCode.TOGGLE_CONTROL -> {
                val bit = signal._payload as Int
                if (bit != null) {
                    if (control.flags.isBitSet(bit)) {
                        control.flags = control.flags.resetBit(bit)
                    } else {
                        control.flags = control.flags.setBit(bit)
                    }
                    control.post()
                    stack.pop()
                    selectMenu("features")
                }
            }
            SignalCode.LOCK_ENTRIES -> {
                doHourglass(this, "Thinking about it...", {
                    lockEntries(Competition.current.id)
                }, {
                    stack.pop()
                    selectMenu("secretary")
                })
            }
            SignalCode.UNLOCK_ENTRIES -> {
                Competition.current.ukOpenLocked = false
                Competition.current.post()
            }
            SignalCode.ACU_WPS -> {
                msgYesNo("Connect Printer?", "Once you press 'Yes', ${NetworkObject.acuTag} will accept WiFi Protected Setup (WPS) connections for approximately 2 minutes.") { yes->
                    if (yes) Api.option("WPS")
                }
            }
            else -> {
                super.whenSignal(signal)
            }
        }
    }

    fun selectMenu(menu: String) {

        fun setTitles(title: String, subTitle: String) {
            tvSubTitle.text = title
            btLeft.visibility = View.GONE
            btMiddle.visibility = View.GONE
            btRight.visibility = View.GONE
        }

        fun setButton(button: QuickButton, caption: String, signalCode: SignalCode, _payload: Any? = null) {
            button.text = ""
            button.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            when (caption) {
                "Back" -> button.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.back, 0, 0, 0)
                "Refresh" -> button.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.refresh, 0, 0, 0)
                "Search" -> button.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.search, 0, 0, 0)
                else -> button.text = caption
            }
            button.tag = prepareSignal(signalCode, _payload)
            button.visibility = View.VISIBLE
        }

        debug("Menu", "selectMenu $menu")

        stack.push(menu)
        Global.activityName = menu        
        loMenu.removeAllViews()
        loMenu.columnCount = 1
        deviceWasAssigned = Device.assigned

        refreshHeading()
        readyToRun = (!agilityClass.isUka) || agilityClass.readyToRun

        debug("menu", "menu: $menu")


        when (menu) {
            "usage" -> {
                setTitles("Tablet Options", "Use Tablet For")
                if (ringNumbers.rowCount > 0) {
                    addMenuButton(loMenu, "Ring Party", SignalCode.DEVICE_RING_PARTY)
                    addMenuButton(loMenu, "Score Board", SignalCode.DEVICE_SCOREBOARD)
                }
                addMenuButton(loMenu, "Secretary", SignalCode.CHECK_PIN, PIN_SECRETARY)
                addMenuButton(loMenu, "System Manager", SignalCode.CHECK_PIN, PIN_SYSTEM_MANAGER)
                setButton(btLeft, "Exit", SignalCode.BACK)
            }
            "choose_ring" -> {
                setTitles("Tablet Options", "Select Ring")
                var buttonWidth = defaultButtonWidth
                when (ringNumbers.rowCount) {
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
                ringNumbers.beforeFirst()
                while (ringNumbers.next()) {
                    addMenuButton(
                        loMenu,
                        "Ring ${ringNumbers.getInt("ringNumber")}",
                        SignalCode.RING_SELECTED,
                        ringNumbers.getInt("ringNumber"),
                        buttonWidth = buttonWidth
                    )
                }
                setButton(btLeft, "Back", SignalCode.BACK)
            }
            "ring_party" -> {
                ringPartyData.syncRing()
                setTitles("Ring Party Options", "Choose a function")
                if (!Competition.isUka || !agilityClass.isLocked) {
                    if (!agilityClass.isClosed) {
                        if (Competition.hasBookingIn && agilityClass.bookIn) {
                            addMenuButton(loMenu, "Booking In", SignalCode.SELECT_MENU, "booking_in").isEnabled =
                                readyToRun
                        }
                        addMenuButton(
                            loMenu,
                            if (Competition.isUkaStyle) "Manage Queue" else "Control Ring",
                            if (Competition.isUkaStyle) SignalCode.MENU_QUERY_PROGRESS else SignalCode.START_ACTIVITY, 
                            Queue::class.java
                        ).isEnabled = readyToRun

                        addMenuButton(loMenu, "Scrime", SignalCode.MENU_QUERY_SCRIME, Scrime::class.java).isEnabled =
                            readyToRun
                    }
                    addMenuButton(loMenu, "Manage Class", SignalCode.START_ACTIVITY, ClassStatus::class.java)
                }
                if (Competition.isKc) {
                    addMenuButton(loMenu, "PA Message", SignalCode.START_ACTIVITY, VirtualRadioRing::class.java)
                }
                addMenuButton(loMenu, "View Ring Plan", SignalCode.START_ACTIVITY, RingStatusRingParty::class.java)
                if (!agilityClass.isClosed) {
                    addMenuButton(loMenu, "Enter Course Times", SignalCode.START_ACTIVITY, EnterCourseTimes::class.java)
                }
                //addMenuButton(loMenu, "Clapper Board", SignalCode.START_ACTIVITY, ClapperBoard::class.java)
                //addMenuButton(loMenu, "Manage Queue (Old)", SignalCode.MENU_QUERY_PROGRESS, CheckIn::class.java).isEnabled = readyToRun
                setButton(btLeft, "Exit", SignalCode.BACK)
            }
            "booking_in" -> {
                setTitles("Booking In Options", "Choose a function")
                addMenuButton(loMenu, "Self Service", SignalCode.MENU_QUERY_PROGRESS, SelfService::class.java)
                addMenuButton(loMenu, "Ring Party Assisted", SignalCode.MENU_QUERY_PROGRESS, BookIn::class.java)
                setButton(btLeft, "Back", SignalCode.BACK)
            }
            "ring_manage" -> {
                setTitles("Ring Manager Options", "Choose a function")
                addMenuButton(loMenu, "Manage Class", SignalCode.START_ACTIVITY, ClassStatus::class.java)
                addMenuButton(loMenu, "Manage Ring", SignalCode.START_ACTIVITY, RingStatus::class.java)
                addMenuButton(loMenu, "Enter Course Times", SignalCode.START_ACTIVITY, EnterCourseTimes::class.java)
                //addMenuButton(loMenu, "Clapper Board", SignalCode.START_ACTIVITY, ClapperBoard::class.java)
                setButton(btLeft, "Back", SignalCode.BACK)
            }
            "secretary" -> {
                if (Competition.isUkOpen && ! Competition.current.ukOpenLocked) {
                    Competition.current.refresh()
                }
                val locked = Competition.current.isUkOpen && Competition.current.ukOpenLocked
                setTitles("Show Secretary Options", "Choose a function")
                if (Competition.current.isUka) {
                    addMenuButton(loMenu, "Member Services", SignalCode.START_ACTIVITY, MemberServices::class.java)
                } else if (!locked) {
                    addMenuButton(loMenu, "Dogs", SignalCode.START_ACTIVITY, CompetitorServices::class.java)
                }
                if (ringNumbers.rowCount > 0) {
                    addMenuButton(loMenu, "Rings", SignalCode.START_ACTIVITY, RingStatus::class.java)
                }
                if (Competition.current.isUka && AgilityClass.specialParentClasses.rowCount > 0) {
                    addMenuButton(loMenu, "Special Classes", SignalCode.SELECT_MENU, "special_classes")
                }
                addMenuButton(
                    loMenu,
                    if (Competition.current.isUka) "All Classes" else "Classes",
                    SignalCode.START_ACTIVITY,
                    ClassList::class.java
                )
                if (Competition.current.isUka) {
                    addMenuButton(loMenu, "Accounts", SignalCode.CHECK_PIN, PIN_ACCOUNTS)
                    addMenuButton(loMenu, "Measuring", SignalCode.CHECK_PIN, PIN_MEASURE)
                }
                if (Competition.isKc) {
                    addMenuButton(loMenu, "Public Address", SignalCode.START_ACTIVITY, PublicAddress::class.java)
                }
                if (Competition.isUkOpen) {
                    if (locked) {
                        addMenuButton(loMenu, "Un-Lock Entries", SignalCode.UNLOCK_ENTRIES)
                    } else {
                        addMenuButton(loMenu, "Lock Entries", SignalCode.LOCK_ENTRIES)
                    }
                }
                addMenuButton(loMenu, "Connect Printer", SignalCode.ACU_WPS)
                setButton(btLeft, "Exit", SignalCode.BACK)
            }
            "special_classes" -> {
                setTitles("Special Classes", "Choose a class")
                AgilityClass.specialParentClasses.beforeFirst()
                if (AgilityClass.specialParentClasses.rowCount > 5) {
                    loMenu.columnCount = 2
                }
                while (AgilityClass.specialParentClasses.next()) {
                    addMenuButton(
                        loMenu,
                        AgilityClass.specialParentClasses.groupName,
                        SignalCode.MANAGE_COMBINED_CLASS,
                        AgilityClass.specialParentClasses.id
                    )
                }
                setButton(btLeft, "Back", SignalCode.BACK)
            }
            "secretary_accounts" -> {
                setTitles("Show Accounts", "Choose Option")
                addMenuButton(loMenu, "Reports & Stats", SignalCode.SELECT_MENU, "secretary_reports")
                addMenuButton(loMenu, "End of Day", SignalCode.START_ACTIVITY, EndOfDay::class.java)
                setButton(btLeft, "Back", SignalCode.BACK)
            }
            "select_accounting_day" -> {
                setTitles("Select Day", "Select Day")
                days.beforeFirst()
                while (days.next()) {
                    addMenuButton(
                        loMenu,
                        days.getDate("date").fullishDate(),
                        SignalCode.ACCOUNTING_DAY_SELECTED,
                        days.getDate("date")
                    )
                }
                setButton(btLeft, "Back", SignalCode.BACK)
            }
            "secretary_reports" -> {
                setTitles("Show Secretary Reports", "Choose a report")
                addMenuButton(loMenu, "Account Payments", SignalCode.PRINT_ACCOUNT_PAYMENTS)
                addMenuButton(loMenu, "Late Entry Report", SignalCode.PRINT_LATE_ENTRIES)
                addMenuButton(loMenu, "Complimentary Entry Report", SignalCode.PRINT_COMPLIMENTARY_ENTRIES)
                addMenuButton(loMenu, "Special Classes Report", SignalCode.PRINT_SPECIAL_ENTRIES)
                addMenuButton(loMenu, "Cheques Report", SignalCode.PRINT_CHEQUES_LIST)
                addMenuButton(loMenu, "Complimentary Used Report", SignalCode.PRINT_COMPLIMENTARY_USED)
                addMenuButton(loMenu, "Quick Stats", SignalCode.START_ACTIVITY, ShowStats::class.java)
                setButton(btLeft, "Back", SignalCode.BACK)
            }
            "system_manager" -> {
                setTitles("System Manager's Options", "Choose a function")
                addMenuButton(loMenu, "Secretary Options", SignalCode.SELECT_MENU, "secretary")
                if (ringNumbers.rowCount > 0) {
                    addMenuButton(loMenu, "Ring Party Options", SignalCode.DEVICE_RING_PARTY)
                    addMenuButton(loMenu, "Score Board", SignalCode.DEVICE_SCOREBOARD)
                }
                addMenuButton(loMenu, "System Tools", SignalCode.SELECT_MENU, "tools")
                setButton(btLeft, "Exit", SignalCode.BACK)
            }
            "tools" -> {
                setTitles("System Tools", "Choose a function")
                addMenuButton(loMenu, "Control Units", SignalCode.START_ACTIVITY, ControlUnitServices::class.java)
                addMenuButton(loMenu, "Network", SignalCode.START_ACTIVITY, NetworkTest::class.java)
                addMenuButton(loMenu, "Features", SignalCode.SELECT_MENU, "features")
                addMenuButton(loMenu, "Set System Date", SignalCode.SET_SYSTEM_DATE)
                addMenuButton(loMenu, "Heat Test", SignalCode.START_ACTIVITY, HeatTest::class.java)
                addMenuButton(loMenu, "Select Show", SignalCode.SELECT_MENU, "select_show")
                addMenuButton(loMenu, "Crash Test", SignalCode.CRASH)
                setButton(btLeft, "Exit", SignalCode.BACK)
            }
            "system_manager_control" -> {
                setTitles("System Control Options", "Choose a function")
                addMenuButton(loMenu, "Control Units", SignalCode.START_ACTIVITY, ControlUnitServices::class.java)
                setButton(btLeft, "Back", SignalCode.BACK)
            }
            "system_manager_restricted" -> {
                setTitles("System Control Options", "Choose a function")
                addMenuButton(loMenu, "Control Units", SignalCode.START_ACTIVITY, ControlUnitServices::class.java)
                setButton(btLeft, "Back", SignalCode.BACK)
            }
            "select_show" -> {
                Competition().where("processed AND dateEnd>=CURDATE() AND idCompetition!=${Competition.current.id}") {
                    addMenuButton(loMenu, briefName, SignalCode.SWITCH_SHOW, id)
                }
            }
            "features" -> {
                setTitles("System Control Features", "")
                if (control.liveLinkDisabled) {
                    addMenuButton(loMenu, "Enable live link", SignalCode.TOGGLE_CONTROL, 0, buttonWidth = 350)
                } else {
                    addMenuButton(loMenu, "Disable live link", SignalCode.TOGGLE_CONTROL, 0, buttonWidth = 350)
                }
                if (!control.useIdUka) {
                    addMenuButton(loMenu, "Use UKA dog codes", SignalCode.TOGGLE_CONTROL, 1, buttonWidth = 350)
                } else {
                    addMenuButton(loMenu, "Use Plaza dog codes", SignalCode.TOGGLE_CONTROL, 1, buttonWidth = 350)
                }
                if (!control.useOldRegistrationRule) {
                    addMenuButton(loMenu, "Old registration check", SignalCode.TOGGLE_CONTROL, 2, buttonWidth = 350)
                } else {
                    addMenuButton(loMenu, "New registration check", SignalCode.TOGGLE_CONTROL, 2, buttonWidth = 350)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PIN_SECRETARY -> sendSignal(SignalCode.DEVICE_SECRETARY)
                PIN_ACCOUNTS -> {
                    if (days.rowCount == 1) {
                        days.first()
                        sendSignal(SignalCode.ACCOUNTING_DAY_SELECTED, days.getDate("date"))
                    } else {
                        sendSignal(SignalCode.SELECT_MENU, "select_accounting_day")
                    }
                }
                PIN_MEASURE -> {
                    sendSignal(SignalCode.START_ACTIVITY, MeasuringServices::class.java)
                }
                PIN_SYSTEM_MANAGER -> {
                    if (killSystem) {
                        sendSignal(SignalCode.KILL_AUTHORIZED)
                    } else {
                        sendSignal(SignalCode.DEVICE_SYSTEM_MANAGER)
                    }
                }
            }
        }
    }

}