/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.activities

import org.egility.android.BaseActivity
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.granite.R
import org.egility.granite.fragments.*
import org.egility.library.dbobject.Competition
import org.egility.library.general.*

/**
 * Created by mbrickman on 15/10/15.
 */
class MemberServices : BaseActivity(R.layout.content_holder) {

    private var data = MemberServicesData

    private lateinit var selectDogByCodeFragment: SelectDogByCodeFragment
    private lateinit var selectDogByNameFragment: SelectDogByNameFragment

    private lateinit var buyCreditsFragment: BuyCreditsFragment
    private lateinit var buyCreditsFragmentFree: BuyCreditsFragmentFree
    private lateinit var checkoutFragment: CheckoutFragment
    private lateinit var accountMenu: AccountMenu
    private lateinit var dogMenu: DogMenuUka
    private lateinit var creditsUsedFragment: CreditsUsedFragment
    private lateinit var transactionsFragment: TransactionsFragment
    private lateinit var alternativeHandlerFragment: AlternativeHandlerFragment
    private lateinit var registerMemberFragment: RegisterMemberFragment
    private lateinit var registerDogFragment: RegisterDogFragment
    private lateinit var dogRegisterFragment: DogRegisterFragment
    private lateinit var changeHandlerFragment: ChangeHandlerFragmentUka

    init {
        if (!dnr) {

            selectDogByCodeFragment = SelectDogByCodeFragment()
            selectDogByNameFragment = SelectDogByNameFragment()
            selectDogByNameFragment.competitorModeAllowed = true

            buyCreditsFragment = BuyCreditsFragment()
            buyCreditsFragmentFree = BuyCreditsFragmentFree()
            checkoutFragment = CheckoutFragment()
            accountMenu = AccountMenu()
            dogMenu = DogMenuUka()
            creditsUsedFragment = CreditsUsedFragment()
            transactionsFragment = TransactionsFragment()
            alternativeHandlerFragment = AlternativeHandlerFragment()
            registerMemberFragment = RegisterMemberFragment()
            registerDogFragment = RegisterDogFragment()
            dogRegisterFragment = DogRegisterFragment()
            changeHandlerFragment = ChangeHandlerFragmentUka()
            changeHandlerFragment.entry = MemberServicesData.entry
        }
    }

    override fun whenInitialize() {
        selectDogByCodeFragment.isSecretary = true
    }

    override fun whenSignal(signal: Signal) {

        when (signal.signalCode) {
            SignalCode.RESET -> {
                defaultFragmentContainerId = R.id.loContent
                selectDogByCodeFragment.clear()
                selectDogByCodeFragment.title = Competition.current.uniqueName + " - Select Member"
                selectDogByCodeFragment.hint = "Enter Member's Dog Code"
                selectDogByCodeFragment.isSelectCompetitor = true
                selectDogByCodeFragment.autoOK = true
                selectDogByCodeFragment.selectedSignal = SignalCode.DOG_SELECTED
                loadTopFragment(selectDogByCodeFragment)
                signal.consumed()
            }
            SignalCode.SELECT_MEMBER_USING_CODE -> {
                selectDogByCodeFragment.clear()
                selectDogByCodeFragment.title = Competition.current.uniqueName + " - Select Team Member"
                selectDogByCodeFragment.hint = "Enter Dog ID"
                selectDogByCodeFragment.isSelectCompetitor = false
                selectDogByCodeFragment.autoOK = false
                selectDogByCodeFragment.selectedSignal = SignalCode.DOG_SELECTED

                loadTopFragment(selectDogByCodeFragment)
                signal.consumed()
            }
            SignalCode.MEMBER_MENU -> {
                loadFragment(accountMenu)
                signal.consumed()
            }
            SignalCode.DOG_MENU -> {
                val idDog = signal._payload as Int?
                if (idDog != null) {
                    data.selectDog(idDog)
                    loadFragment(dogMenu)
                    signal.consumed()
                }
            }
            SignalCode.DOG_SELECTED -> {
                val dogSelection = signal._payload as DogSelection?
                if (dogSelection != null) {
                    data.selectAccount(dogSelection.idAccount, dogSelection.idDog)
                    sendSignal(SignalCode.MEMBER_MENU)
                    signal.consumed()
                }
            }
            SignalCode.MEMBER_SELECTED -> {
                val idAccount = signal._payload as Int?
                if (idAccount != null) {
                    data.selectAccount(idAccount)
                    sendSignal(SignalCode.MEMBER_MENU)
                    signal.consumed()
                }
            }
            SignalCode.BUY_LATE_ENTRY -> {
                data.credits = 0
                data.creditsLock = false
                loadFragment(buyCreditsFragment)
                signal.consumed()
            }
            SignalCode.COMPLIMENTARY_ENTRY -> {
                data.credits = 0
                data.creditsLock = false
                loadFragment(buyCreditsFragmentFree)
                signal.consumed()
            }
            SignalCode.CHECKOUT -> {
                checkoutFragment.mode = CheckoutFragment.Mode.REGULAR
                loadFragment(checkoutFragment)
                signal.consumed()
            }
            SignalCode.CHECKOUT_FREE -> {
                checkoutFragment.mode = CheckoutFragment.Mode.COMPLIMENTARY
                loadFragment(checkoutFragment)
                signal.consumed()
            }
            SignalCode.CHECKOUT_EDIT -> {
                checkoutFragment.mode = CheckoutFragment.Mode.EDIT
                loadFragment(checkoutFragment)
                signal.consumed()
            }
            SignalCode.CREDITS_USED -> {
                val returnSignal = signal._payload as SignalCode?
                if (returnSignal != null) {
                    creditsUsedFragment.returnSignal = returnSignal
                    loadFragment(creditsUsedFragment)
                    signal.consumed()
                }
            }
            SignalCode.LOOKUP_TEAM_BY_NAME -> {
                loadFragment(selectDogByNameFragment)
                signal.consumed()
            }
            SignalCode.TRANSACTIONS -> {
                loadFragment(transactionsFragment)
                signal.consumed()
            }
            SignalCode.ALTERNATIVE_HANDLER -> {
                val idTeam = signal._payload as Int?
                if (idTeam != null) {
                    data.alternativeHandlerIdTeam = idTeam
                    loadFragment(alternativeHandlerFragment)
                    signal.consumed()
                }
            }
            SignalCode.REGISTER_MEMBER -> {
                loadFragment(registerMemberFragment)
                signal.consumed()
            }
            SignalCode.REGISTER_DOG -> {
                msgYesNo("Question", "Have you tried the 'Update from Plaza' option.") { yes ->
                    if (yes) {
                        loadFragment(registerDogFragment)
                    } else {
                        popUp("Information", "Try the 'Update from Plaza' option before manually adding a dog.")
                    }
                }

                signal.consumed()
            }
            SignalCode.DOG_REGISTER -> {
                val idDog = signal._payload as Int?
                if (idDog != null) {
                    data.selectDog(idDog)
                    msgYesNo("Question", "Do you want to register ${data.selectedDog.cleanedPetName}?") { yes ->
                        if (yes) {
                            if (!data.selectedDog.hasUkaHeight || !data.selectedDog.hasUkaLevel) {
                                loadFragment(dogRegisterFragment)
                            } else {
                                sendSignal(SignalCode.DOG_REGISTER_CHECKOUT)
                            }
                        }
                    }
                    signal.consumed()
                }
            }
            SignalCode.MEMBER_REGISTER -> {
                val idCompetitor = signal._payload as Int?
                if (idCompetitor != null) {
                    data.selectCompetitor(idCompetitor)
                    if (data.selectedCompetitor.ukaDateConfirmed.isEmpty()) {
                        msgYesNo("Question", "Does ${data.selectedCompetitor.fullName} want to join UKA?") { yes ->
                            AddMembershipItem(data.shoppingList, data.selectedCompetitor.id, data.selectedCompetitor.fullName, data.selectedCompetitor.dateOfBirth  > today.addYears(-16))
                            sendSignal(SignalCode.CHECKOUT)
                        }
                    } else if (data.selectedCompetitor.ukaMembershipExpires.before(today)) {
                        if (!data.selectedCompetitor.isUkaRegistered) {
                            msgYesNo("Question", "Does ${data.selectedCompetitor.fullName} want to renew membership?") { yes ->
                                RenewMembershipItem(data.shoppingList, data.selectedCompetitor.id, data.selectedCompetitor.fullName, data.selectedCompetitor.dateOfBirth  > today.addYears(-16))
                                sendSignal(SignalCode.CHECKOUT)
                            }
                        }
                    }
                    signal.consumed()
                }
            }
            SignalCode.DOG_REGISTER_CHECKOUT -> {
                AddRegistrationItem(data.shoppingList, data.selectedDog.id, data.selectedDog.cleanedPetName, data.selectedDog.code)
                sendSignal(SignalCode.CHECKOUT)
            }
            SignalCode.CHANGE_HANDLER -> {
                loadFragment(changeHandlerFragment)
                signal.consumed()
            }
            SignalCode.HANDLER_CHANGED -> {
                data.entry.refresh()
                sendSignal(SignalCode.BACK)
            }
            else -> super.whenSignal(signal)
        }

    }

}
