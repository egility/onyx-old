/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.View
import kotlinx.android.synthetic.main.select_dog_by_code.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidServices
import org.egility.android.tools.AndroidUtils.goneIf
import org.egility.android.tools.AndroidUtils.invisibleIf
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.tools.doHourglass
import org.egility.granite.R
import org.egility.library.api.Api
import org.egility.library.dbobject.Competition
import org.egility.library.dbobject.CompetitionDog
import org.egility.library.dbobject.Dog
import org.egility.library.dbobject.control
import org.egility.library.general.*

data class DogSelection(val idDog: Int, val idAccount: Int, val idCompetitor: Int, val petName: String = "", val handlerName: String, val ukaHeightCodePerformance: String)

class SelectDogByCodeFragment : BaseFragment(R.layout.select_dog_by_code) {

    var selectedSignal = SignalCode.DOG_SELECTED
    var isSelfService = false

    private enum class State {
        LOOKING, FOUND, INVALID, NOT_REGISTERED, NOT_ENTERED
    }

    var hint = "Enter Dog ID"
    private val foundHint = "Press OK to continue"
    private val notRegisteredHint = "Press 'Clear' and enter a different code"
    private val notEnteredHint = "Press 'Clear' and enter a different code"
    var isSelectCompetitor = false
    var autoOK = true
    var idTeam = 0
    var isSecretary = false
    var className = ""


    private var state = State.LOOKING

    var codeText = ""

    val dog = Dog()

    override fun whenInitialize() {
        tvPageHeader.text = title
        invisibleIf(isSelectCompetitor, tvTeam)
        goneIf(!control.liveLinkDisabled || !Competition.current.isUka, btRegister)
        goneIf(control.liveLinkDisabled || !isSecretary, btImport)
        goneIf(isSelfService, loNavigation)
        goneIf(isSelfService, loTopText)
        goneIf(!isSelfService, loSelfServiceText)
        tvClass.text = className
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
            btDelAll -> sendSignal(SignalCode.DELETE_ALL)
            btDel -> sendSignal(SignalCode.DELETE)
            btBack -> sendSignal(SignalCode.BACK)
            btOK -> sendSignal(SignalCode.OK)
            btSearch -> sendSignal(SignalCode.LOOKUP_TEAM_BY_NAME)
            btRegister -> sendSignal(SignalCode.REGISTER_MEMBER)
            btImport -> sendSignal(SignalCode.IMPORT_ACCOUNT)

        }
    }

    private fun checkButtons() {
        btZero.isEnabled = state == State.LOOKING
        btOne.isEnabled = state == State.LOOKING
        btTwo.isEnabled = state == State.LOOKING
        btThree.isEnabled = state == State.LOOKING
        btFour.isEnabled = state == State.LOOKING
        btFive.isEnabled = state == State.LOOKING
        btSix.isEnabled = state == State.LOOKING
        btSeven.isEnabled = state == State.LOOKING
        btEight.isEnabled = state == State.LOOKING
        btNine.isEnabled = state == State.LOOKING
        btDel.isEnabled = true
        btDelAll.isEnabled = true
        btOK.isEnabled = (state == State.FOUND)
        btImport.isEnabled = (state == State.INVALID)
        btRegister.isEnabled = (state == State.INVALID)
        btSearch.isEnabled = codeText.isEmpty()
        btBack.isEnabled = codeText.isEmpty()
    }

    override fun whenSignal(signal: Signal) {

        when (signal.signalCode) {
            SignalCode.DIGIT -> {
                val digit = signal._payload as Int?
                if (digit != null) {
                    codeText = codeText + digit
                    tvIdTeam.text = codeText
                    checkButtons()
                    sendSignal(SignalCode.DOG_CODE_UPDATED, codeText, queued = true)
                    signal.consumed()
                }
            }
            SignalCode.DELETE -> {
                if (!codeText.isEmpty()) {
                    codeText = codeText.substring(0, codeText.length - 1)
                    tvIdTeam.text = codeText
                    checkButtons()
                    sendSignal(SignalCode.DOG_CODE_UPDATED, codeText, queued = true)
                }
                signal.consumed()
            }
            SignalCode.DELETE_ALL -> {
                if (!codeText.isEmpty()) {
                    codeText = ""
                    tvIdTeam.text = codeText
                    checkButtons()
                    sendSignal(SignalCode.DOG_CODE_UPDATED, codeText, queued = true)
                }
                signal.consumed()
            }
            SignalCode.BACK, SignalCode.PAGE_DOWN -> {
                if (isSelfService) {
                    if (isPatternMatch("bbb") || (AndroidServices.isT63 && isPatternMatch("ddd"))) {
                        sendSignal(SignalCode.CHECK_PIN, PIN_GENERAL)
                    }
                    signal.consumed()
                } else if (!codeText.isEmpty()) {
                    signal.consumed()
                }
            }
            SignalCode.DOG_CODE_UPDATED -> {
                tvHint.text = hint
                if (codeText.length == 5) {
                    seekDogCode(Integer.parseInt(codeText))
                    checkButtons()
                    if (isSelfService) {
                        if (state == State.FOUND) {
                            sendSignal(
                                selectedSignal,
                                DogSelection(dog.id, dog.idAccount, dog.idCompetitorHandler, dog.cleanedPetName, dog.handlerName, dog.ukaHeightCodePerformance)
                            )
                        } else if (state == State.NOT_REGISTERED) {
                            Global.services.popUp("Warning", "${dog.cleanedPetName} ($codeText) is not registered with UKA, please report to secretary")
                            state = State.LOOKING
                            codeText = ""
                            sendSignal(SignalCode.RESET_FRAGMENT)
                        } else {
                            Global.services.popUp("Warning", "$codeText is not a valid dog code, please re-enter")
                            state = State.LOOKING
                            codeText = ""
                            sendSignal(SignalCode.RESET_FRAGMENT)
                        }
                    } else {
                        if (state == State.FOUND || isSecretary && state == State.NOT_REGISTERED) {
                            if (autoOK) {
                                sendSignal(
                                    selectedSignal,
                                    DogSelection(dog.id, dog.idAccount, dog.idCompetitorHandler, dog.cleanedPetName, dog.handlerName, dog.ukaHeightCodePerformance)
                                )
                            } else {
                                sendSignal(SignalCode.RESET_FRAGMENT)
                            }
                        } else if (state == State.NOT_REGISTERED) {
                            sendSignal(SignalCode.RESET_FRAGMENT)
                        } else if (state == State.NOT_ENTERED) {
                            sendSignal(SignalCode.RESET_FRAGMENT)
                        } else {
                            tvCompetitor.text = "*** UNKNOWN DOG CODE ***"
                            if (isSecretary) {
                                tvHint.text = "Press 'Import' to download from Plaza"
                            }
                        }
                    }
                } else {
                    state = State.LOOKING
                    sendSignal(SignalCode.RESET_FRAGMENT)
                }
                signal.consumed()
            }
            SignalCode.RESET_FRAGMENT -> {
                checkButtons()
                tvIdTeam.text = codeText
                when (state) {
                    SelectDogByCodeFragment.State.LOOKING -> {
                        tvCompetitor.text = ""
                        tvTeam.text = ""
                        tvHint.text = hint
                    }
                    SelectDogByCodeFragment.State.FOUND -> {
                        tvCompetitor.text = dog.handlerName
                        tvTeam.text = dog.cleanedPetName
                        tvHint.text = foundHint
                    }
                    SelectDogByCodeFragment.State.NOT_REGISTERED -> {
                        tvCompetitor.text = dog.handlerName
                        tvTeam.text = dog.cleanedPetName + " (NOT REGISTERED)"
                        tvHint.text = if (isSecretary) foundHint else notRegisteredHint
                    }
                    SelectDogByCodeFragment.State.INVALID -> {
                        tvCompetitor.text = "*** UNKNOWN DOG CODE ***"
                        tvTeam.text = ""
                        tvHint.text = hint
                    }
                    SelectDogByCodeFragment.State.NOT_ENTERED -> {
                        tvCompetitor.text = dog.handlerName
                        tvTeam.text = dog.cleanedPetName + " (NOT ENTERED)"
                        tvHint.text = notEnteredHint
                    }
                }
                signal.consumed()
            }
            SignalCode.OK -> {
                if (state == State.FOUND) {
                    sendSignal(
                        selectedSignal,
                        DogSelection(dog.id, dog.idAccount, dog.idCompetitorHandler, dog.cleanedPetName, dog.handlerName, dog.ukaHeightCodePerformance)
                    )
                }
                signal.consumed()
            }
            SignalCode.IMPORT_ACCOUNT -> {
                if (codeText.length == 5) {
                    val dogCode = codeText.toIntDef(0)
                    if (dogCode > 0) {
                        var error = -1
                        doHourglass(activity, "Thinking about it...",
                            {
                                error = Api.importAccount(dogCode)
                            }, {
                                if (error == 0) {
                                    sendSignal(SignalCode.DOG_CODE_UPDATED)
                                } else {
                                    popUp(
                                        "Sorry!",
                                        "Unable to import $dogCode from Agility Plaza. Check that this is the correct code."
                                    )
                                }
                            })
                    }
                    signal.consumed()
                }
            }

            else -> {
                doNothing()
            }
        }
    }

    fun clear() {
        dog.release()
        state = State.LOOKING
        codeText = ""
    }

    private fun seekDogCode(dogCode: Int) {
        if (Competition.current.isUka) {
            state = State.INVALID
            dog.select("dogCode=$dogCode", "idAccount>0 DESC, aliasFor>0 DESC", limit = 1)
            while (dog.found() && dog.aliasFor > 0) {
                dog.find(dog.aliasFor)
            }
            if (dog.found() && dog.idCompetitorHandler > 0) {
                state =
                    if (isSecretary || dog.isUkaRegistered(!control.useOldRegistrationRule)) State.FOUND else State.NOT_REGISTERED
                codeText = dog.code.toString()
            }
        } else {
            state = State.INVALID
            dog.select("dogCode=$dogCode", "idAccount>0 DESC, aliasFor>0 DESC", limit = 1)
            while (dog.found() && dog.aliasFor > 0) {
                dog.find(dog.aliasFor)
            }
            if (dog.found()) {
                if (Competition.isUkOpen) {
                    state = if (CompetitionDog.isEntered(Competition.current.id, dog)) State.FOUND else State.NOT_ENTERED
                } else {
                    CompetitionDog.enterAtShow(Competition.current.id, dog)
                    state = State.FOUND
                }
            }
        }
    }

}



