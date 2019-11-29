/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import kotlinx.android.synthetic.main.review_classes.*
import org.egility.android.BaseFragment
import org.egility.android.NavigationGroup
import org.egility.android.tools.*
import org.egility.android.views.DbCursorListView
import org.egility.granite.R
import org.egility.library.database.DbQuery
import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Grade
import org.egility.library.dbobject.Height
import org.egility.library.general.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by mbrickman on 23/05/17.
 */
class ReviewClasses : BaseFragment(R.layout.review_classes), DbCursorListView.Listener {

    val data = CompetitorServicesData
    var thisDate = today

    data class Item(
        val idAgilityClass: Int, var date: Date = nullDate, var code: Int = -1, var number: Int = -1,
        var part: String = "", var suffix: String = "", var name: String = "", var grade: String = "",
        var entered: Boolean = false, var isEligible: Boolean = false,
        var entryType: Int = -1, var entryProgress: Int = -1, var runningOrder: Int = -1
    )


    val days = TreeMap<Date, ArrayList<Item>>()

    override fun whenInitialize() {
        tvPageHeader.text = Competition.current.uniqueName + " - Review Classes"
        tvCompetitor.text = data.competitor.fullName

        thisDate = nullDate
        var firstDate = nullDate
        for (day in Competition.current.agilityClassDates.split(",")) {
            val date = day.toDate()
            if (firstDate == nullDate) firstDate = date
            if (date == today) thisDate = today
        }
        if (thisDate == nullDate) thisDate = firstDate

        if (Competition.current.isFab) {
            val agility = Grade.getGradeShort(data.competitionDog.fabGradeAgility)
            val jumping = Grade.getGradeShort(data.competitionDog.fabGradeJumping)
            val steeplechase = Grade.getGradeShort(data.competitionDog.fabGradeSteeplechase)
            val height = Height.getHeightName(data.competitionDog.fabHeightCode)
            val ifcsHeight = Height.getHeightName(data.competitionDog.ifcsHeightCode)
            val collie = if (data.competitionDog.fabCollie) "Collie/X" else "ABC"
            tvDog.text =
                "${data.dog.petName} (${data.dog.code}): $height, $collie, A=$agility, J=$jumping, S=$steeplechase, IFCS=$ifcsHeight"
        } else {
            tvDog.text =
                "${data.dog.petName} (${data.dog.code}: G${Grade.getGradeShort(data.competitionDog.kcGradeCode)}, ${Height.getCombinedName(data.competitionDog.kcHeightCode, data.competitionDog.kcJumpHeightCode, short = true)})"
        }
    }


    override fun whenClick(view: View) {
        if (view is CheckBox) {
            val position = view.tag as? Int?
            if (position != null) {
                val items = days.get(thisDate)
                if (items != null) {
                    val item = items[position]
                    debug("checked", "${item.number} = ${view.isChecked}")
                    item.entered = view.isChecked
                    setUpNavigation()
                }
            }
        } else if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                doSelectEntries()
                signal.consumed()
            }
            SignalCode.PAGE_DOWN -> {
                lvEntries.pageDown()
                signal.consumed()
            }
            SignalCode.PAGE_UP -> {
                lvEntries.pageUp()
                signal.consumed()
            }
            SignalCode.FINISHED -> {
                sendSignal(SignalCode.DOG_MENU)
            }
            SignalCode.CANCEL -> {
                data.clearProposed()
                data.dogMenuStack.clear()
                sendSignal(SignalCode.BACK)
            }
            SignalCode.SELECT_DAY -> {
                val date = signal._payload as Date?
                if (date != null) {
                    thisDate = date
                    load()
                   // setUpNavigation()
                }
            }
            SignalCode.OK -> {
                doHourglass(activity, "Thinking about it...", {
                    val agilityClassIds = ArrayList<Int>()
                    for (day in days) {
                        val items = day.value
                        items.filter { it.isEligible && it.entered }.forEach { agilityClassIds.add(it.idAgilityClass) }
                    }

                    data.saveProposed(agilityClassIds)
                    data.dogMenuStack.clear()
                }, {
                    whenInitialize()
                    sendSignal(SignalCode.RESET_FRAGMENT)
                })
            }
            else -> {
                doNothing()
            }
        }

    }

    fun findItem(date: Date, idAgilityClass: Int): Item {
        val items = days.getOrPut(date, { ArrayList() })
        if (items != null) {
            for (item in items) {
                if (item.idAgilityClass == idAgilityClass) return item
            }
            val item = Item(idAgilityClass)
            items.add(item)
            return item
        }
        return (Item(-1))
    }

    fun doSelectEntries() {
        doHourglass(activity, "Thinking about it...", {
            days.clear()
            var entryList = ""
            val idTeamList = data.dog.idTeamList
            if (!idTeamList.isEmpty()) {
                val entry = DbQuery(
                    """
                        SELECT entry.idAgilityClass, entry.entryType, entry.progress, entry.runningOrder, agilityClass.classDate
                        FROM entry JOIN agilityClass using (idAgilityClass)
                        WHERE agilityClass.idCompetition=${Competition.current.id} AND entry.idTeam IN ($idTeamList)
                    """
                )
                while (entry.next()) {
                    val items = days.getOrPut(entry.getDate("classDate"), { ArrayList<Item>() })
                    items.add(
                        Item(
                            entry.getInt("idAgilityClass"), entryType = entry.getInt("entryType"),
                            entryProgress = entry.getInt("progress"), runningOrder = entry.getInt("runningOrder"),
                            entered = entry.getInt("progress") != PROGRESS_ENTRY_DELETED
                        )
                    )
                    entryList = entryList.commaAppend(entry.getInt("idAgilityClass").toString())
                }
            }
            if (entryList.isEmpty()) entryList = "9999999"
            val agilityClass = AgilityClass()
            if (Competition.isFab) {
                agilityClass.select(
                    """
                        idCompetition = ${Competition.current.id} AND (
                            idAgilityClass IN ($entryList) OR
                            (entryRule = $ENTRY_RULE_GRADE1 AND FIND_IN_SET(${data.proposedFabGradeAgility.quoted}, gradeCodes)>0) OR
                            (entryRule = $ENTRY_RULE_GRADE2 AND FIND_IN_SET(${data.proposedFabGradeJumping.quoted}, gradeCodes)>0) OR
                            (entryRule = $ENTRY_RULE_GRADE3 AND FIND_IN_SET(${data.proposedFabGradeSteeplechase.quoted}, gradeCodes)>0) OR
                            entryRule = $ENTRY_RULE_ANY_GRADE ) 
                    """.trimIndent(), "classDate, classCode, gradeCodes, suffix"
                )
            } else {
                agilityClass.select(
                    """
                        idCompetition=${Competition.current.id} AND (
                            (idAgilityClass IN ($entryList)) OR
                            (
                                FIND_IN_SET(${data.proposedGradeCode.quoted}, REPLACE(gradeCodes, ';', ','))>0 AND
                                FIND_IN_SET(${data.proposedHeightCode.quoted}, REPLACE(heightCodes, ';', ','))>0 AND
                                FIND_IN_SET(${data.proposedJumpHeightCode.quoted}, REPLACE(jumpHeightCodes, ';', ','))>0
                            )
                         )
                    """.trimIndent(), "classNumber, classNumberSuffix, part"
                )
            }

            while (agilityClass.next()) {
                val item = findItem(agilityClass.date, agilityClass.id)
                item.date = agilityClass.date
                item.code = agilityClass.code
                item.number = agilityClass.number
                item.part = agilityClass.part
                item.suffix = agilityClass.suffix
                item.grade = agilityClass.gradeCodes
                item.name = if (agilityClass.isFabStyle) agilityClass.name else agilityClass.describeClass(short = true, noPrefix = true)
                item.isEligible = if (Competition.isFab)
                    agilityClass.isFabEligible(data.proposedFabGradeAgility, data.proposedFabGradeJumping, data.proposedFabGradeSteeplechase)
                else
                    agilityClass.isEligible(data.proposedGradeCode, data.proposedHeightCode, data.proposedJumpHeightCode)
            }

            for (day in days) {
                val items = day.value
                if (Competition.isFab) {
                    items.sortWith(Comparator { a, b ->
                        if (a.date == b.date)
                            if (a.code == b.code)
                                if (a.grade == b.grade)
                                    a.suffix.compareTo(b.suffix)
                                else
                                    a.grade.compareTo(b.grade)
                            else
                                a.code.compareTo(b.code)
                        else
                            a.date.compareTo(b.date)
                    })
                } else {
                    items.sortWith(Comparator { a, b ->
                        if (a.date == b.date)
                            if (a.number == b.number)
                                a.part.compareTo(b.part)
                            else
                                a.number.compareTo(b.number)
                        else
                            a.date.compareTo(b.date)
                    })
                }

                var index = 0
                var date = nullDate
                var id = -1
                while (index < items.size) {
                    val item = items[index]
                    if (item.date != date) {
                        date = item.date
                        val heading = Item(id, name = date.format("EEEE, d MMMM"))
                        items.add(index, heading)
                        index++
                        id--
                    }
                    index++
                }
            }
        }, {
            load()
            setUpNavigation()
        })
    }

    fun load() {
        val items = days.getOrPut(thisDate, { ArrayList() })
        lvEntries.load(this, items, R.layout.view_entry)
        lvEntries.requestFocus()
    }

    override fun whenItemClick(position: Int) {
    }

    override fun whenLongClick(position: Int) {
    }

    override fun whenPopulate(view: View, position: Int) {
        val items = days.getOrPut(thisDate, { ArrayList() })
        if (position >= 0 && position < items.size) {
            val item = items[position]

            val xbEnter = view.findViewById(R.id.xbEnter) as CheckBox
            val tvHeading = view.findViewById(R.id.tvHeading) as TextView

            AndroidUtils.goneIf(item.idAgilityClass > 0, tvHeading)
            AndroidUtils.goneIf(item.idAgilityClass <= 0, xbEnter)

            tvHeading.text = item.name

            val runningOrder = if (item.runningOrder > -1) " (r/o ${item.runningOrder})" else ""
            val progress =
                if (item.entryType == ENTRY_TRANSFER && item.entryProgress <= PROGRESS_REMOVED)
                    ", Added"
                else if (item.entryType != ENTRY_TRANSFER && item.entryProgress >= PROGRESS_REMOVED)
                    ", Removed"
                else
                    ""


            var text = "${item.name}$runningOrder$progress"
            xbEnter.setTextColor(if (item.isEligible) BLACK else GREY)
            xbEnter.text = text
            xbEnter.isEnabled = item.isEligible
            xbEnter.isChecked = item.entered && item.isEligible && item.entryProgress < PROGRESS_REMOVED
            xbEnter.tag = position
        } else {
            debug("ReviewClasses", "whenPopulate: position=$position, items=${items.size}")
        }
    }

    val isUpdated: Boolean
        get() {
            if (data.proposedGradeCode != data.competitionDog.kcGradeCode ||
                data.proposedHeightCode != data.competitionDog.kcHeightCode ||
                data.proposedJumpHeightCode != data.competitionDog.kcJumpHeightCode) {
                return true
            }
            for (day in days) {
                val items = day.value
                for (item in items) {
                    if ((item.entryProgress != -1 && item.entryProgress != PROGRESS_ENTRY_DELETED) != item.entered) {
                        return true
                    }
                }
            }
            return false
        }

    fun setUpNavigation() {
        loNavigation.removeAllViews()
        addNavigationButton(loNavigation, "Back", SignalCode.BACK)
        val navigationGroup = NavigationGroup()
        var selected = 0
        if (days.count() > 1) {
            for (day in days) {
                val date = day.key
                if (date == thisDate) selected = navigationGroup.size
                navigationGroup.add(date.dayNameShort, SignalCode.SELECT_DAY, date)
            }
        }
        addNavigationGroup(loNavigation, navigationGroup, selected, 4)
        
        if (isUpdated) {
            addNavigationButton(loNavigation, "Cancel", SignalCode.CANCEL)
            addNavigationButton(loNavigation, "OK", SignalCode.OK)
        } else {
            addNavigationButton(loNavigation, "Finished", SignalCode.FINISHED)
        }
    }

}