/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.granite.activities

import org.egility.granite.fragments.PublicAddressFragment
import org.egility.granite.fragments.RadioFragment
import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

/**
 * Created by mbrickman on 27/04/16.
 */
open class PublicAddress : BaseActivity(R.layout.content_holder) {

    private lateinit var publicAddressFragment: PublicAddressFragment
    private lateinit var radioFragment: RadioFragment

    init {
        if (!dnr) {
            publicAddressFragment = PublicAddressFragment()
            radioFragment = RadioFragment()
            radioFragment.exitWhenDone = true
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET -> {
                defaultFragmentContainerId = R.id.loContent
                loadTopFragment(publicAddressFragment)
                signal.consumed()
            }
            SignalCode.VIRTUAL_RADIO -> {
                loadFragment(radioFragment)
                signal.consumed()
            }
            else -> {
                super.whenSignal(signal)
            }
        }

    }

}

