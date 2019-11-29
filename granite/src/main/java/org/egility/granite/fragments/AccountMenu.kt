/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.account_menu.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.tools.doHourglass
import org.egility.granite.R
import org.egility.library.api.Api
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.Dog
import org.egility.library.dbobject.control
import org.egility.library.general.*

/**
 * Created by mbrickman on 20/11/15.
 */
class AccountMenu : BaseFragment(R.layout.account_menu) {

    var data = MemberServicesData
    var shoppingList = data.shoppingList
    var stack = data.accountMenuStack

    override fun whenInitialize() {
        tvPageHeader.text = Competition.current.uniqueName + " - Member Services"
    }

    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btFinished -> sendSignal(SignalCode.RESET)
                btBack -> sendSignal(SignalCode.BACK)
                btCheckout -> sendSignal(SignalCode.CHECKOUT)
            }
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.PAGE_DOWN -> {
                svAccount.scrollBy(0, svAccount.height)
                signal.consumed()
            }
            SignalCode.PAGE_UP -> {
                svAccount.scrollBy(0, -svAccount.height)
                signal.consumed()
            }
            SignalCode.RESET_FRAGMENT -> {
                tvAccountCode.text = data.account.code
                tvBasket.text = "${shoppingList.itemCount}"

                tvLateEntries.text = data.getCreditsAvailableText(Competition.current.id)
                AndroidUtils.goneIf(shoppingList.itemCount == 0, loBasket)
                btCheckout.isEnabled = shoppingList.itemCount != 0
                btFinished.isEnabled = shoppingList.itemCount == 0

                if (stack.isEmpty()) {
                    selectMenu("main")
                } else {
                    selectMenu(stack.pop())
                }
                signal.consumed()
            }
            SignalCode.REQUEST_CHECKOUT -> {
                /* add this to force back button to return here */
                shoppingList.addNull()
            }
            SignalCode.DO_MENU -> {
                val menu = signal._payload as String?
                if (menu != null) {
                    selectMenu(menu)
                }
            }
            SignalCode.BACK -> {
                if (btBack.isEnabled) {
                    if (!stack.empty()) {
                        stack.pop()
                    }
                    if (!stack.empty()) {
                        selectMenu(stack.pop())
                        signal.consumed()
                    }
                } else {
                    signal.consumed()
                }
            }
            SignalCode.IMPORT_ACCOUNT -> {
                val dog = Dog()
                dog.select("idAccount=${data.idAccount}", "idDog=${data.idDogFirst} DESC, petName")
                if (dog.first()) {
                    var error = -1
                    doHourglass(activity, "Thinking about it...",
                        {
                            error = Api.importAccount(dog.code)
                        }, {
                            data.loadAccountDogs()
                            data.loadAccountMembers()
                            sendSignal(SignalCode.RESET_FRAGMENT)
                        })
                    signal.consumed()
                }
            }

            else -> {
                doNothing()
            }
        }
    }

    fun selectMenu(menu: String) {
        stack.push(menu)
        loMenu.removeAllViews()
        loMenu.columnCount = 2
        loMembers.removeAllViews()
        loMembers.columnCount = 2
        loDogs.removeAllViews()
        loDogs.columnCount = 3
        btBack.isEnabled = true
        when (menu) {
            "main" -> {
                tvSubTitle.text = "Service Options"
                btBack.isEnabled = shoppingList.itemCount == 0
                addMenuButton(loMenu, "Late Entry", SignalCode.BUY_LATE_ENTRY)
                addMenuButton(loMenu, "Complimentary", SignalCode.COMPLIMENTARY_ENTRY)
                addMenuButton(loMenu, "Purchases", SignalCode.TRANSACTIONS)
                //addMenuButton(loMenu, "UKA Membership", SignalCode.UKA_MEMBERSHIP)
                if (!control.liveLinkDisabled) {
                    addMenuButton(loMenu, "Update from Plaza", SignalCode.IMPORT_ACCOUNT)
                }
                var hasRetired = false
                data.accountCompetitor.forEach { competitor ->
                    if (!competitor.isDeleted) {
                        val prefix = if (data.accountCompetitor.idAccount!=data.account.id) "+" else ""
                        if (competitor.isUkaRegistered) {
                                addMenuButton(loMembers, prefix + competitor.fullName, SignalCode.DO_NOTHING, competitor.id, buttonWidth = 240, singleLine = true)
                            } else {
                                addMenuButton(loMembers, prefix + competitor.fullName + "*", SignalCode.MEMBER_REGISTER, competitor.id, buttonWidth = 240, singleLine = true)
                            }
                    }
                }

                data.accountDog.forEach { dog ->
                    if (dog.state < DOG_GONE) {
                        if (dog.state == DOG_RETIRED) {
                            hasRetired = true
                        } else {
                            if (dog.isUkaRegistered(!control.useOldRegistrationRule)) {
                                addMenuButton(loDogs, dog.cleanedPetName, SignalCode.DOG_MENU, dog.id, buttonWidth = 160, singleLine = true)
                            } else {
                                addMenuButton(loDogs, dog.cleanedPetName + "*", SignalCode.DOG_REGISTER, dog.id, buttonWidth = 160, singleLine = true)
                            }
                        }
                    }
                }
                if (hasRetired) {
                    //addMenuButton(loDogs, "Retired ...", SignalCode.DO_NOTHING, buttonWidth = 160)
                }
                if (control.liveLinkDisabled) {
                    addMenuButton(loDogs, "Add Dog ...", SignalCode.REGISTER_DOG, buttonWidth = 160)
                }
            }
        }
    }


}
