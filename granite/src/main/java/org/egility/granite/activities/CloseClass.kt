/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import org.egility.granite.fragments.CloseClassFragment
import org.egility.library.general.ringPartyData
import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

/**
 * Created by mbrickman on 10/06/15.
 */
open class CloseClass : BaseActivity(R.layout.content_holder) {

    private lateinit var closeClassListFragment: CloseClassFragment

    init {
        if (!dnr) {
            closeClassListFragment = CloseClassFragment()
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET -> {
                ringPartyData.syncRing()
                defaultFragmentContainerId = R.id.loContent
                loadTopFragment(closeClassListFragment)
                signal.consumed()
            }
            else -> {
                super.whenSignal(signal)
            }
        }

    }

}
