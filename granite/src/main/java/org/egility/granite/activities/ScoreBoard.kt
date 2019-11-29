/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import android.view.View
import org.egility.granite.fragments.ScoreBoardFragment
import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R


open class ScoreBoard() : BaseActivity(R.layout.content_holder) {

    private lateinit var scoreBoardListFragment : ScoreBoardFragment

    init {
        if (!dnr) {
            scoreBoardListFragment = ScoreBoardFragment()
        }
    }

    override fun whenInitialize() {
        defaultFragmentContainerId = R.id.loContent
        loadTopFragment(scoreBoardListFragment)
        pulseRate = 10
    }

    override fun whenClick(view: View) {
        super.whenClick(view)
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.PULSE -> {
                sendSignal(SignalCode.RESET_FRAGMENT)
            }
            SignalCode.BACK -> {
                if (isPatternMatch("bbb")) {
                    super.whenSignal(signal)
                } else {
                    signal.consumed()
                }
            }
            SignalCode.EXIT -> {
                finish()
            }
            else -> {
                super.whenSignal(signal)
            }
        }
    }

}