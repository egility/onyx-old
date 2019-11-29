/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import org.egility.library.general.doNothing
import org.egility.android.BaseFragment
import org.egility.android.tools.Signal
import org.egility.granite.R

/**
 * Created by mbrickman on 10/08/16.
 */
class DummyFragment : BaseFragment(R.layout.fragment_dummy) {

    override fun whenClick(view: View) {
        doNothing()
    }

    override fun whenSignal(signal: Signal) {
        doNothing()
    }
}