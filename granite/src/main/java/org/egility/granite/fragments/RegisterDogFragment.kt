/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.fragment_register_dog.*
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Dog
import org.egility.library.dbobject.Team
import org.egility.library.general.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R

/**
 * Created by mbrickman on 12/03/16.
 */

class RegisterDogFragment : BaseFragment(R.layout.fragment_register_dog) {

    var data = MemberServicesData
    var shoppingList = data.shoppingList
    val team = Team()

    init {
        isBackable = false
    }


    override fun whenInitialize() {
        tvPageHeader.text = Competition.current.uniqueName + " - Member Services"
        edCode.setText("")
        edName.setText("")
        xbOnLine.isChecked = false
        AndroidUtils.goneIf(!xbOnLine.isChecked, loCode)
        xbOnLine.setOnClickListener {
            AndroidUtils.goneIf(!xbOnLine.isChecked, loCode)
        }
    }

    override fun whenClick(view: View) {
        when (view) {
            btCancel -> sendSignal(SignalCode.BACK)
            btOK -> sendSignal(SignalCode.OK)
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                edCode.setText("")
                edName.setText("")
                AndroidUtils.goneIf(!xbOnLine.isChecked, loCode)
                edName.requestFocus()
                showKeyboard()
            }
            SignalCode.OK -> {
                hideKeyboard()
                when {
                    edName.text.isEmpty() -> {
                        Global.services.popUp("Warning", "You must enter a pet name")
                    }
                    xbOnLine.isChecked && !edCode.text.toString().toIntDef().between(10000, Dog.maxIDUka + 100) -> {
                        Global.services.popUp("Warning", "You must enter a valid code for Dog 1")
                    }
                    xbOnLine.isChecked && Dog.idUkaExists(edCode.text.toString().toIntDef()) -> {
                        Global.services.popUp("Warning", "The code for this dog has already been registered")
                    }
                    else -> {
                        if (xbOnLine.isChecked) {
                            Dog.registerNew(data.idAccount, data.idCompetitor, edName.text.toString(), edCode.text.toString().toIntDef())
                            sendSignal(SignalCode.BACK)
                        } else {
                            RegistrationItem(shoppingList, data.idCompetitor, edName.text.toString())
                            sendSignal(SignalCode.CHECKOUT)
                        }
                    }
                }
            }
            else -> {
                doNothing()
            }
        }

    }

}