/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.granite.activities

import org.egility.granite.fragments.RadioFragment
import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

/**
 * Created by mbrickman on 27/04/16.
 */
open class VirtualRadio : BaseActivity(R.layout.content_holder) {

    private lateinit var radioFragment: RadioFragment

    init {
        if (!dnr) {
            radioFragment = RadioFragment()
            radioFragment.ringPartyMode = this is VirtualRadioRing
            radioFragment.exitWhenDone = this is VirtualRadioRing
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET -> {
                defaultFragmentContainerId = R.id.loContent
                loadTopFragment(radioFragment)
                signal.consumed()
            }
            else -> {
                super.whenSignal(signal)
            }
        }

    }

}

class VirtualRadioRing : VirtualRadio();
