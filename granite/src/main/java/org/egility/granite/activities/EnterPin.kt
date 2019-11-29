/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import android.content.Intent
import android.view.View
import kotlinx.android.synthetic.main.enter_pin.*
import org.egility.library.dbobject.control
import org.egility.library.general.*
import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

/**
 * Created by mbrickman on 05/09/15.
 */

class LockDevice : EnterPin(true)

open class EnterPin(var isLock: Boolean = false) : BaseActivity(R.layout.enter_pin) {

    private var pinEntered = ""
    private var pinRequired = ""
    private var pinType = 0

    override fun whenInitialize() {
        tvPin.text = ""
        if (isLock) {
            btCancel.visibility = View.INVISIBLE
        }
        pinType = intent.getIntExtra("pinType", PIN_GENERAL)
        when (pinType) {
            PIN_GENERAL -> {
                tvHint.text = "Enter PIN"
                pinRequired = control.pinGeneral
            }
            PIN_SECRETARY -> {
                tvHint.text = "Enter Secretary's PIN"
                pinRequired = control.pinSecretary
            }
            PIN_ACCOUNTS -> {
                tvHint.text = "Enter Show Manager's PIN"
                pinRequired = control.pinShowAccounts
            }
            PIN_MEASURE -> {
                tvHint.text = "Enter Measurer's PIN"
                pinRequired = control.pinMeasure
            }
            PIN_SYSTEM_MANAGER -> {
                tvHint.text = "Enter System Manager's PIN"
                pinRequired = control.pinSystemManager
            }
        }
    }

    override fun whenClick(view: View) {
        when (view) {
            btZero -> sendSignal(SignalCode.DIGIT, 0)
            btOne -> sendSignal(SignalCode.DIGIT, 1)
            btTwo -> sendSignal(SignalCode.DIGIT, 2)
            btThree -> sendSignal(SignalCode.DIGIT, 3)
            btFour -> sendSignal(SignalCode.DIGIT, 4)
            btFive -> sendSignal(SignalCode.DIGIT, 5)
            btSix -> sendSignal(SignalCode.DIGIT, 6)
            btSeven -> sendSignal(SignalCode.DIGIT, 7)
            btEight -> sendSignal(SignalCode.DIGIT, 8)
            btNine -> sendSignal(SignalCode.DIGIT, 9)
            btDel -> sendSignal(SignalCode.DELETE)
            btDelAll -> sendSignal(SignalCode.DELETE_ALL)
            btCancel -> sendSignal(SignalCode.BACK)
            else -> super.whenClick(view)
        }
    }

    public override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.DIGIT -> {
                val digit = signal._payload as Int?
                if (digit != null) {
                    pinEntered += digit
                    sendSignal(SignalCode.PIN_UPDATED)
                }
                signal.consumed()
            }
            SignalCode.DELETE -> {
                if (!pinEntered.isEmpty()) {
                    pinEntered = pinEntered.substring(0, pinEntered.length - 1)
                    sendSignal(SignalCode.PIN_UPDATED)
                }
                signal.consumed()
            }
            SignalCode.DELETE_ALL -> {
                if (!pinEntered.isEmpty()) {
                    pinEntered = ""
                    sendSignal(SignalCode.PIN_UPDATED)
                }
                signal.consumed()
            }
            SignalCode.PIN_UPDATED -> {
                if (pinEntered.length > 4) {
                    pinEntered = pinEntered.drop(4)
                }
                var asterisk = ""
                while (asterisk.length < pinEntered.length) {
                    asterisk += "*"
                }
                tvPin.text = asterisk
                if (pinEntered == pinRequired) {
                    sendSignal(SignalCode.OK)
                }
                signal.consumed()
            }
            SignalCode.BACK -> {
                if (isLock) {
                    signal.consumed()
                } else {
                    setResult(RESULT_CANCELED, Intent())
                    finish()
                }
            }
            SignalCode.OK -> {
                setResult(RESULT_OK, Intent())
                finish()
            }
            else -> {
                doNothing()
            }
        }
    }
}
