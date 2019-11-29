/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import org.egility.granite.fragments.ResultsFragment
import org.egility.library.dbobject.Ring
import org.egility.library.general.ringPartyData
import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

class Results : BaseActivity(R.layout.content_holder) {

    private lateinit var resultsFragment : ResultsFragment

    init {
        if (!dnr) {
            resultsFragment = ResultsFragment()
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET -> {
                defaultFragmentContainerId = R.id.loContent
                loadFragment(resultsFragment)
                signal.consumed()
            }
            else -> {
                super.whenSignal(signal)
            }
        }
    }


}
