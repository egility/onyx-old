/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_end_of_day_cheque.*
import org.egility.library.database.DbQuery
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.CompetitionDay
import org.egility.library.dbobject.Competitor
import org.egility.library.general.money
import org.egility.android.BaseFragment
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.DbCursorListView
import org.egility.granite.R
import org.egility.granite.R.layout.fragment_end_of_day_cheque
import org.egility.granite.activities.EndOfDay

/**
 * Created by mbrickman on 17/08/16.
 */
class EndOfDayChequeFragment : BaseFragment(fragment_end_of_day_cheque), DbCursorListView.Listener {

    val day = EndOfDay.day
    val query = DbQuery()

    override fun whenInitialize() {
        tvPageHeader.text = Competition.current.uniqueName + " - End of Day (Cheques)"
        lvCheques.load(this, query, R.layout.view_cheque)
        lvCheques.requestFocus()

        day.loadCheques(query)
        day.loadData()
        xbAllPresent.isChecked = day.calculatedCheque == day.totalCheque
        xbAllPresent.setOnClickListener {
            if (xbAllPresent.isChecked) {
                day.totalCheque = day.calculatedCheque
            } else {
                day.totalCheque = 0
            }
        }
    }


    override fun whenClick(view: View) {
        when (view) {
            btNext -> sendSignal(SignalCode.END_OF_DAY_CHEQUE_DONE)
            btBack -> sendSignal(SignalCode.END_OF_DAY_CHEQUE_BACK)
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal) {
        }
    }

    override fun whenItemClick(position: Int) {
    }

    override fun whenLongClick(position: Int) {
    }

    override fun whenPopulate(view: View, position: Int) {
        val tvFullName = view.findViewById(R.id.tvFullName) as TextView
        val tvCheque = view.findViewById(R.id.tvCheque) as TextView

        query.cursor = position
        tvFullName.text = Competitor.getFullName(query.getString("givenName"), query.getString("familyName"))
        tvCheque.text = query.getInt("cheque").money
    }


}