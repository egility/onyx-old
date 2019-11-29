/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import android.view.View
import kotlinx.android.synthetic.main.show_stats.*
import org.egility.library.database.DbQuery
import org.egility.library.dbobject.Competition
import org.egility.library.general.*
import org.egility.android.BaseActivity
import org.egility.android.tools.SignalCode
import org.egility.granite.R

/**
 * Created by mbrickman on 27/03/16.
 */

class ShowStats : BaseActivity(R.layout.show_stats) {

    override fun whenInitialize() {
        tvPageHeader.text = Competition.current.uniqueName + " - Show Stats"
        val sql = """
            SELECT
                SUM(IF(type = ${ITEM_LATE_ENTRY_PAID}, quantity, 0)) AS lateEntries,
                SUM(IF(type = ${ITEM_LATE_ENTRY_DISCRETIONARY}, quantity, 0)) AS discretionary,
                SUM(IF(type = ${ITEM_LATE_ENTRY_STAFF}, quantity, 0)) AS staff,
                SUM(IF(type = ${ITEM_LATE_ENTRY_UKA}, quantity, 0)) AS rep,
                SUM(IF(type = ${ITEM_LATE_ENTRY_TRANSFER}, quantity, 0)) AS transfers,

                SUM(IF(type = ${ITEM_LATE_ENTRY_PAID}, cash, 0)) AS lateCash,
                SUM(IF(type = ${ITEM_LATE_ENTRY_PAID}, cheque, 0)) AS lateCheque,
                
                SUM(IF(type = ${ITEM_SPECIAL_CLASS}, cash, 0)) AS specialCash,
                SUM(IF(type = ${ITEM_SPECIAL_CLASS}, cheque, 0)) AS specialCheque,
                
                SUM(IF(type IN ($ITEM_REGISTRATION), cash, 0)) AS accountCash,
                SUM(IF(type IN ($ITEM_REGISTRATION), cheque, 0)) AS accountCheque,


                SUM(IF(type = 150 && promised <> 0, amount, 0)) AS accountPayments,
                SUM(cash) AS cash,
                SUM(cheque) AS cheques,
                SUM(promised) AS inPost
            FROM
                competitionLedger
            WHERE
                idCompetition = ${Competition.current.id} AND
                DATE(accountingDate) = ${Global.endOfDayDate.sqlDate}
            LIMIT 1
        """
        var query = DbQuery(sql)
        query.first()

        tvLateEntriesSold.text = query.getInt("lateEntries").toString()
        tvComplimentary.text = query.getInt("discretionary").toString()
        tvStaff.text = query.getInt("staff").toString()

        tvLateCash.text = query.getInt("lateCash").money
        tvLateCheques.text = query.getInt("lateCheque").money
        tvLateTotal.text = (query.getInt("lateCash") + query.getInt("lateCheque")).money

        tvSpecialCash.text = query.getInt("specialCash").money
        tvSpecialCheques.text = query.getInt("specialCheque").money
        tvSpecialTotal.text = (query.getInt("specialCash") + query.getInt("specialCheque")).money

        tvAccountCash.text = query.getInt("accountCash").money
        tvAccountCheques.text = query.getInt("accountCheque").money
        tvAccountTotal.text = (query.getInt("accountCash") + query.getInt("accountCheque")).money

        tvTotalCash.text = (query.getInt("lateCash") + query.getInt("specialCash") + query.getInt("accountCash")).money
        tvTotalCheques.text = (query.getInt("lateCheque") + query.getInt("specialCheque") + query.getInt("accountCheque")).money
        tvTotalTotal.text = (query.getInt("lateCash") + query.getInt("specialCash") + query.getInt("accountCash") + query.getInt("lateCheque") + query.getInt("specialCheque") + query.getInt("accountCheque")).money
    }

    override fun whenClick(view: View) {
        when (view) {
            btBack -> sendSignal(SignalCode.BACK)
        }
        super.whenClick(view)
    }


}