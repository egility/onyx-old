/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.text.Editable
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_register_member.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidOnTextChange
import org.egility.android.tools.AndroidTextWatcher
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Competitor
import org.egility.library.dbobject.Dog
import org.egility.library.dbobject.Team
import org.egility.library.general.*

/**
 * Created by mbrickman on 12/03/16.
 */

class RegisterMemberFragment : BaseFragment(R.layout.fragment_register_member), AndroidOnTextChange {
    var data = MemberServicesData
    var shoppingList = data.shoppingList
    val team = Team()

    init {
        isBackable = false
    }


    override fun whenInitialize() {
        tvPageHeader.text = "${Competition.current.uniqueName} - Member Services"
        edCode.setText("")
        edGivenName.setText("")
        edFamilyName.setText("")
        xbOnLine.isChecked = false
        xbJuniorHandler.isChecked = false

        edDog1Name.setText("")
        edDog1Code.setText("")
        edDog2Name.setText("")
        edDog2Code.setText("")


        tvCode.isEnabled = false
        edCode.isEnabled = false
        tvDog1Code.isEnabled = false
        edDog1Code.isEnabled = false
        tvDog2Code.isEnabled = false
        edDog2Code.isEnabled = false
        xbOnLine.setOnClickListener {
            tvCode.isEnabled = xbOnLine.isChecked
            edCode.isEnabled = xbOnLine.isChecked
            tvDog1Code.isEnabled = xbOnLine.isChecked
            edDog1Code.isEnabled = xbOnLine.isChecked
            tvDog2Code.isEnabled = xbOnLine.isChecked
            edDog2Code.isEnabled = xbOnLine.isChecked
        }
        edCode.addTextChangedListener(AndroidTextWatcher(edCode, this))
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
                edGivenName.setText("")
                edFamilyName.setText("")


                edDog1Name.setText("")
                edDog1Code.setText("")
                edDog2Name.setText("")
                edDog2Code.setText("")


                tvCode.isEnabled = false
                edCode.isEnabled = false
                tvDog1Code.isEnabled = false
                edDog1Code.isEnabled = false
                tvDog2Code.isEnabled = false
                edDog2Code.isEnabled = false

            }
            SignalCode.HAVE_MEMBER_CODE -> {
                var idAccount = -1
                var idUKA = edCode.text.toString().toIntDef()
                idAccount = Competitor.idUkaToIdAccount(idUKA)
                if (idAccount > 0) {
                    whenYes("Question", "Member Code already exists. Do you want to see the member's details?") {
                        hideKeyboard()

                        sendSignal(SignalCode.MEMBER_SELECTED, idAccount)
                    }
                } else {
                    edGivenName.requestFocus()
                }
            }

            SignalCode.OK -> {
                hideKeyboard()
                when {
                    edGivenName.text.isEmpty() -> {
                        Global.services.popUp("Warning", "You must enter a given name")
                    }
                    edFamilyName.text.isEmpty() -> {
                        Global.services.popUp("Warning", "You must enter a family name")
                    }
                    xbOnLine.isChecked && !edCode.text.toString().toIntDef().between(
                        10000,
                        Competitor.maxIDUka + 100
                    ) -> {
                        Global.services.popUp("Warning", "You must enter a valid member code")
                    }
                    xbOnLine.isChecked && Competitor.idUkaExists(edCode.text.toString().toIntDef()) -> {
                        Global.services.popUp("Warning", "The member code has already been registered")
                    }
                    xbOnLine.isChecked && edDog1Name.text.isNotEmpty() && !edDog1Code.text.toString().toIntDef().between(
                        10000,
                        Dog.maxIDUka + 100
                    ) -> {
                        Global.services.popUp("Warning", "You must enter a valid code for Dog 1")
                    }
                    xbOnLine.isChecked && edDog2Name.text.isNotEmpty() && !edDog2Code.text.toString().toIntDef().between(
                        10000,
                        Dog.maxIDUka + 100
                    ) -> {
                        Global.services.popUp("Warning", "You must enter a valid code for Dog 2")
                    }
                    xbOnLine.isChecked && Dog.idUkaExists(edDog1Code.text.toString().toIntDef()) -> {
                        Global.services.popUp("Warning", "The code for Dog 1 has already been registered")
                    }

                    xbOnLine.isChecked && Dog.idUkaExists(edDog2Code.text.toString().toIntDef()) -> {
                        Global.services.popUp("Warning", "The code for Dog 2 has already been registered")
                    }


                    else -> {
                        val junior = xbJuniorHandler.isChecked
                        val online = xbOnLine.isChecked
                        val competitor = Competitor.registerNew(
                            edGivenName.text.toString(),
                            edFamilyName.text.toString(),
                            edCode.text.toString().toIntDef()
                        )
                        data.selectAccount(competitor.idAccount)
                        if (online) {
                            if (edDog1Name.text.isNotEmpty()) {
                                val idDog = Dog.registerNew(
                                    data.idAccount,
                                    competitor.id,
                                    edDog1Name.text.toString(),
                                    edDog1Code.text.toString().toIntDef()
                                )
                                if (edDog2Name.text.isNotEmpty()) {
                                    Dog.registerNew(
                                        data.idAccount,
                                        competitor.id,
                                        edDog2Name.text.toString(),
                                        edDog2Code.text.toString().toIntDef()
                                    )
                                }
                            }
                            sendSignal(SignalCode.MEMBER_MENU)
                        } else {
                            MembershipItem(
                                shoppingList,
                                data.accountCompetitor.id,
                                data.accountCompetitor.idUka,
                                data.accountCompetitor.fullName,
                                junior
                            )
                            if (edDog1Name.text.isNotEmpty()) {
                                RegistrationItem(shoppingList, data.idCompetitor, edDog1Name.text.toString())
                                if (edDog2Name.text.isNotEmpty()) {
                                    RegistrationItem(shoppingList, data.idCompetitor, edDog2Name.text.toString())
                                }
                            }
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

    override fun onTextChange(view: TextView, editable: Editable) {
        when (view) {
            edCode -> {
                if (editable.length == 5) {
                    edCode.invalidate()
                    sendSignal(SignalCode.HAVE_MEMBER_CODE, null, queued = true)
                }
            }
        }
    }


}