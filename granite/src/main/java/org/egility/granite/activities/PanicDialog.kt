/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import android.view.View
import kotlinx.android.synthetic.main.panic_dialog.*
import org.egility.android.BaseActivity
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

/**
 * Created by mbrickman on 12/01/16.
 */
class PanicDialog : BaseActivity(R.layout.panic_dialog) {

    override fun whenInitialize() {
        setFinishOnTouchOutside(false)
    }

    override fun whenFinalize() {
        AndroidUtils.kill()
    }

    override fun whenClick(view: View) {
        when (view) {
            btOK -> sendSignal(SignalCode.BACK)
            else -> super.whenClick(view)
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.BACK -> {
                finish()
            }
            else -> {
                super.whenSignal(signal)
            }
        }
    }


}
