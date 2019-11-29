/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.fragment_special_class_menu.*
import org.egility.android.BaseFragment
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.tools.doThinking
import org.egility.granite.R
import org.egility.library.api.Api
import org.egility.library.general.*

/**
 * Created by mbrickman on 08/10/15.
 */
class SpecialClassMenu : BaseFragment(R.layout.fragment_special_class_menu) {

    val data = ClassData
    val agilityClass = data.agilityClass
    
    var loading = false

    override fun whenInitialize() {
        defaultButtonWidth = 350
        tvPageHeader.text = "${agilityClass.name} - Status"
    }


    override fun whenClick(view: View) {
        if (view.tag is Signal) {
            sendSignal(view.tag as Signal)
        } else {
            when (view) {
                btOK -> sendSignal(SignalCode.BACK)
            }
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                loadData()
                signal.consumed()
            }
            SignalCode.RE_PRINT_RESULTS -> {
                doThinking(activity, {
                    val idAgilityClass = signal._payload as Int?
                    if (idAgilityClass != null) {
                        Reports.printResults(agilityClass.id)
                    }
                }, {
                    sendSignal(SignalCode.RESET_FRAGMENT)
                })
                signal.consumed()
            }
            SignalCode.PRINT_ENTRIES -> {
                doThinking(activity, {
                    Reports.printEntries(agilityClass.id)
                }, {
                    sendSignal(SignalCode.RESET_FRAGMENT)
                })
            }
            SignalCode.PRINT_RUNNING_ORDERS -> {

                doThinking(activity, {
                    val runnable = agilityClass.runnableClasses()
                    runnable.beforeFirst()
                    while (runnable.next()) {
                        if (!runnable.isChild || runnable.isOpen) {
                            Reports.printRunningOrders(runnable.id)
                        }
                    }
                }, {
                    sendSignal(SignalCode.RESET_FRAGMENT)
                })
            }
            SignalCode.REGENERATE_RUNNING_ORDERS -> {
                whenYes(
                    "Be Very Careful", "Re-generating the running orders will invalidate the previously printed " +
                            "one. These must be collected in and destroyed to avoid confusion. Do you still wish to do this?"
                ) {
                    doThinking(activity, {
                        val runnable = agilityClass.runnableClasses()
                        runnable.beforeFirst()
                        while (runnable.next()) {
                            if (!runnable.isChild || runnable.isOpen) {
                                runnable.prepareClass()
                            }
                        }
                    }, {
                        sendSignal(SignalCode.PRINT_RUNNING_ORDERS)
                    })
                }
            }
            SignalCode.REQUEST_LOCKED_FOR_ENTRIES -> {
                var message = "No changes will be permitted"
                if (agilityClass.template.isSpecialParent) {
                    message = "the running orders will be printed and no changes will be permitted"
                }
                whenYes(
                    "Be Very Careful", "Do not lock the entries unless they have been printed off and the " +
                            "competitors have had time to confirm the details. " +
                            "Once the entries are locked, $message. Do you still wish to do this?"
                ) {
                    sendSignal(SignalCode.LOCKED_FOR_ENTRIES)
                }
            }
            SignalCode.LOCKED_FOR_ENTRIES -> {
                doThinking(activity, {
                    Api.entriesClosed(agilityClass.id)
                }, {
                    agilityClass.refresh()
                    sendSignal(SignalCode.RESET_FRAGMENT)

                }
                )
            }
            SignalCode.REQUEST_LOCK_FOR_ENTRIES -> {
                agilityClass.refresh()
                if (!agilityClass.canUnlockEntries) {
                    popUp("Warning", "You cannot unlock entries because ${agilityClass.reason}")
                } else {
                    if (agilityClass.template.isSpecialParent) {
                        whenYes(
                            "Be Very Careful",
                            "Unlocking entries will invalidate the current running orders which must be withdrawn and destroyed. Do you still wish to do this?"
                        ) {
                            sendSignal(SignalCode.UN_LOCK_FOR_ENTRIES)
                        }
                    } else {
                        sendSignal(SignalCode.UN_LOCK_FOR_ENTRIES)
                    }
                }
            }
            SignalCode.UN_LOCK_FOR_ENTRIES -> {
                doThinking(activity, {
                    agilityClass.entriesReopened()
                }, {
                    agilityClass.refresh()
                    sendSignal(SignalCode.RESET_FRAGMENT)
                })
            }
            SignalCode.RE_PRINT_RESULTS -> {
                Reports.printResults(agilityClass.id, finalize = true)
                sendSignal(SignalCode.BACK)
                signal.consumed()
            }
            else -> {
                doNothing()
            }
        }
    }

    private fun loadData() {
        loading = true
        try {
            tvClassStatus.text = "${agilityClass.name}"
            loMenu.removeAllViews()
            tvProgress.text = ""
            tvNoOptions.visibility = View.GONE
            if (agilityClass.isSpecialParent) {
                if (!agilityClass.closedForLateEntries) {
                    addMenuButton(loMenu, "Print Entries", SignalCode.PRINT_ENTRIES)
                    addMenuButton(loMenu, "Lock Entries", SignalCode.REQUEST_LOCKED_FOR_ENTRIES)
                } else {
                    tvProgress.text = "(Entries Locked)"
                    addMenuButton(loMenu, "Unlock Entries", SignalCode.REQUEST_LOCK_FOR_ENTRIES)
                    if (agilityClass.template != ClassTemplate.TRY_OUT) {
                        addMenuButton(loMenu, "Re-Print Running Orders", SignalCode.PRINT_RUNNING_ORDERS)
                        addMenuButton(loMenu, "Edit Running Orders", SignalCode.EDIT_RUNNING_ORDERS)
                        addMenuButton(loMenu, "Re-Generate Running Orders", SignalCode.REGENERATE_RUNNING_ORDERS)
                    }
                }
            } else {
                addMenuButton(loMenu, "Re-Print Running Orders", SignalCode.PRINT_RUNNING_ORDERS)
                addMenuButton(loMenu, "Edit Running Orders", SignalCode.EDIT_RUNNING_ORDERS)
                addMenuButton(loMenu, "Re-Generate Running Orders", SignalCode.REGENERATE_RUNNING_ORDERS)
            }
            if (agilityClass.progress == CLASS_CLOSED) {
                addMenuButton(loMenu, "Re-Print Results", SignalCode.RE_PRINT_RESULTS, agilityClass.id)
            }
            if (loMenu.childCount == 0) {
                tvNoOptions.visibility = View.VISIBLE
            }
        } finally {
            loading = false
        }
    }
}


