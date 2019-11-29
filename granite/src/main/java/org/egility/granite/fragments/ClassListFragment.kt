/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.list_view_holder.*
import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Competition
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.DbCursorListView
import org.egility.granite.R
import java.util.*


/**
 * Created by mbrickman on 07/10/15.
 */
class ClassListFragment : BaseFragment(R.layout.list_view_holder), DbCursorListView.Listener {

    private var agilityClass = AgilityClass("idAgilityClass", "classDate", "className", "classProgress", "ringNumber")

    data class Row(val heading: String = "", val cursor: Int = -1)

    val days = TreeMap<Date, ArrayList<Row>>()
    var thisDate = today

    override fun whenInitialize() {
        tvPageHeader.text = Competition.current.uniqueName + " - Class List"
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        }
    }

    fun getRows(date: Date): ArrayList<Row> {
        return days.getOrPut(date, {ArrayList()})
    }
    
    fun doSelect() {
        with(Competition.current) {
            this@ClassListFragment.days.clear()
            val dateMonitor = ChangeMonitor<Date>(nullDate)
            agilityClass.where("agilityClass.idCompetition=$id", "classDate, classNumber, classNumberSuffix, part, classCode") {
                if (dateMonitor.hasChanged(date)) {
                    getRows(date).add(Row(heading = date.fullDate()))
                }
                getRows(date).add(Row(cursor = cursor))
            }
        }
        load()
        setUpNavigation()
    }
    
    fun load() {
        lvData.load(this, getRows(thisDate), R.layout.view_one_item_list)
        lvData.requestFocus()
    }

    override fun whenSignal(signal: Signal) {


        when (signal.signalCode) {
            SignalCode.PAGE_DOWN -> {
                lvData.pageDown()
                signal.consumed()
            }
            SignalCode.PAGE_UP -> {
                lvData.pageUp()
                signal.consumed()
            }
            SignalCode.RESET_FRAGMENT -> {
                doSelect()
                lvData.invalidate()
                signal.consumed()
            }
            SignalCode.SELECT_DAY -> {
                val date = signal._payload as Date?
                if (date != null) {
                    thisDate = date
                    load()
                    setUpNavigation()
                }
            }
            else -> {
                doNothing()
            }
        }
    }

    override fun whenItemClick(position: Int) {
        val row = getRows(thisDate)[position]
        if (row.cursor > -1) {
            agilityClass.cursor = row.cursor
            sendSignal(SignalCode.CLASS_SELECTED, agilityClass.id)
        }
    }

    override fun whenLongClick(position: Int) {
        doNothing()
    }

    override fun whenPopulate(view: View, position: Int) {
        val row = getRows(thisDate)[position]
        val headingText = view.findViewById(R.id.headingText) as TextView
        val listText1 = view.findViewById(R.id.listText1) as TextView

        AndroidUtils.goneIf(row.heading.isEmpty(), headingText)
        AndroidUtils.goneIf(row.heading.isNotEmpty(), listText1)

        if (!row.heading.isEmpty()) {
            headingText.text = row.heading
        } else {
            agilityClass.cursor = row.cursor
            var line=agilityClass.name
            if (agilityClass.progress> CLASS_PENDING) {
                line += " (${classToText(agilityClass.progress)})"
            }
            listText1.text = line
        }
    }

    fun setUpNavigation() {
        loNavigation.removeAllViews()
        addNavigationButton(loNavigation, "Back", SignalCode.BACK)
        if (days.count() > 1) {
            for (day in days) {
                val date = day.key
                val button = addNavigationButton(loNavigation, date.dayNameShort, SignalCode.SELECT_DAY, date)
                button.isEnabled = date != thisDate
            }
        }
    }

}

