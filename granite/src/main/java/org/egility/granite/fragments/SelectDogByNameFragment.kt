/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.granite.fragments

import android.view.KeyEvent.*
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_lookup_dog.*
import org.egility.android.BaseFragment
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.DbCursorListView
import org.egility.granite.R
import org.egility.library.dbobject.*
import org.egility.library.general.DOG_GONE
import org.egility.library.general.append
import org.egility.library.general.doNothing

class SelectDogByNameFragment : BaseFragment(R.layout.fragment_lookup_dog), DbCursorListView.Listener,
    View.OnTouchListener {

    var competitorModeAllowed: Boolean = false

    private var dog = Dog()
    private var competitor = Competitor()
    private var competitorMode = false


    fun doLookup() {
        if (!edDogName?.text.toString().isEmpty() || !edFamilyName.text.toString().isEmpty()) {
            doSelect(edDogName.text.toString(), edFamilyName.text.toString())
            hideKeyboardPanel()
        }
    }

    private fun hideKeyboardPanel() {
        loKeyboard.visibility = View.GONE
        lvDogs.visibility = View.VISIBLE
        btLookup.isEnabled = false
    }

    private fun showKeyboardPanel() {
        loKeyboard.visibility = View.VISIBLE
        lvDogs.visibility = View.GONE
        btLookup.isEnabled = true
    }

    private fun doSelect(dogName: String, familyName: String) {
        if (Competition.current.isUka) {
            if (dogName.isEmpty() && familyName.isNotEmpty() && competitorModeAllowed) {
                competitorMode = true
                val where =
                    "competitor.familyName LIKE \"$familyName%\" AND competitor.dateDeleted = 0 "
                val orderBy = "competitor.givenName, competitor.FamilyName"
                competitor.select(where, orderBy)
                lvDogs.load(this, competitor, R.layout.view_one_item_list)
            } else {
                competitorMode = false
                var where = ""
                val orderBy = "handler.givenName, handler.FamilyName, dog.petName"

                if (dogName.isEmpty() && familyName.isEmpty()) {
                    where = "false"
                }
                if (dogName.isNotEmpty()) {
                    where = "dog.petName LIKE \"$dogName%\""
                }
                if (!familyName.isEmpty()) {
                    where = where.append("handler.familyName like \"$familyName%\"", " AND ")
                }
                if (!control.useIdUka) {
                    where = where.append("dog.dateDeleted = 0 AND handler.dateDeleted = 0 ", " AND ")
                } else {
                    where = where.append("dog.dateDeleted = 0 AND handler.dateDeleted = 0  AND dog.idUka > 0 ", " AND ")
                }
                where = where.append(" dog.dogState<$DOG_GONE", " AND ")

                dog.handler.joinToParent()

                dog.select(where, orderBy)
                lvDogs.load(this, dog, R.layout.view_one_item_list)
            }
        } else if (Competition.isUkOpen) {
            dog.handler.joinToParent()
            var where = "competitionDog.idCompetition=${Competition.current.id}"
            if (dogName.isEmpty() && familyName.isEmpty()) {
                where = "false"
            }
            if (dogName.isNotEmpty()) {
                where += " AND (dog.registeredName LIKE '%$dogName%' OR dog.petName LIKE '%$dogName%')"
            }
            if (familyName.isNotEmpty()) {
                where += " AND handler.familyName like \"$familyName%\""
            }
            var dogList="-1"
            CompetitionDog().join { dog }.join { dog.owner }.join { dog.handler }
                .where(where, "handler.givenName, handler.FamilyName, dog.registeredName") {
                    dogList = dogList.append(idDog.toString())
                }
            dog.select("idDog IN ($dogList)", "handler.givenName, handler.FamilyName, dog.registeredName")
            lvDogs.load(this, dog, R.layout.view_one_item_list)
        } else {
            dog.handler.joinToParent()
            var where = "true"
            if (dogName.isEmpty() && familyName.isEmpty()) {
                where = "false"
            }
            if (dogName.isNotEmpty()) {
                where += " AND (dog.registeredName LIKE '%$dogName%' OR dog.petName LIKE '%$dogName%')"
            }
            if (familyName.isNotEmpty()) {
                where += " AND handler.familyName like \"$familyName%\""
            }
            dog.select(where, "handler.givenName, handler.FamilyName, dog.registeredName")
            lvDogs.load(this, dog, R.layout.view_one_item_list)
        }
    }

    override fun whenClick(view: View) {
        when (view) {
            btA -> keypress(KEYCODE_A)
            btB -> keypress(KEYCODE_B)
            btC -> keypress(KEYCODE_C)
            btD -> keypress(KEYCODE_D)
            btE -> keypress(KEYCODE_E)
            btF -> keypress(KEYCODE_F)
            btG -> keypress(KEYCODE_G)
            btH -> keypress(KEYCODE_H)
            btI -> keypress(KEYCODE_I)
            btJ -> keypress(KEYCODE_J)
            btK -> keypress(KEYCODE_K)
            btL -> keypress(KEYCODE_L)
            btM -> keypress(KEYCODE_M)
            btN -> keypress(KEYCODE_N)
            btO -> keypress(KEYCODE_O)
            btP -> keypress(KEYCODE_P)
            btQ -> keypress(KEYCODE_Q)
            btR -> keypress(KEYCODE_R)
            btS -> keypress(KEYCODE_S)
            btT -> keypress(KEYCODE_T)
            btU -> keypress(KEYCODE_U)
            btV -> keypress(KEYCODE_V)
            btW -> keypress(KEYCODE_W)
            btX -> keypress(KEYCODE_X)
            btY -> keypress(KEYCODE_Y)
            btZ -> keypress(KEYCODE_Z)
            btDel -> keypress(KEYCODE_DEL)
            btClear -> sendSignal(SignalCode.DELETE_ALL)
            btBack -> sendSignal(SignalCode.BACK)
            btLookup -> sendSignal(SignalCode.LOOKUP)
        }
    }

    override fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.RESET_FRAGMENT -> {
                edDogName.setOnTouchListener(this)
                edFamilyName.setOnTouchListener(this)
                AndroidUtils.hideSoftKeyboard(edDogName)
                AndroidUtils.hideSoftKeyboard(edFamilyName)
                if (returnedViaBack) {
                    doLookup()
                } else {
                    sendSignal(SignalCode.CLEAR_DATA)
                }
                signal.consumed()
            }
            SignalCode.CLEAR_DATA -> {
                edDogName.setText("")
                edFamilyName.setText("")
                dog.release()
                edDogName.requestFocus()
                showKeyboardPanel()
                signal.consumed()
            }
            SignalCode.DELETE_ALL -> {
                sendSignal(SignalCode.CLEAR_DATA)
                signal.consumed()
            }
            SignalCode.LOOKUP -> {
                doLookup()
                signal.consumed()
            }
            SignalCode.PAGE_DOWN -> {
                lvDogs.pageDown()
                signal.consumed()
            }
            SignalCode.PAGE_UP -> {
                lvDogs.pageUp()
                signal.consumed()
            }
            SignalCode.BACK -> {
                /*
                                savedState.clear()
                                savedState.putString("class", "DogByName")
                                savedState.putString("petName", edDogName.text.toString())
                                savedState.putString("familyName", edFamilyName.text.toString())
                                savedState.putInt("top", lvDogs.firstVisiblePosition)
                */
            }
            else -> {
                doNothing()
            }
        }
    }

    override fun whenPopulate(view: View, position: Int) {
        val listText1 = view.findViewById(R.id.listText1) as TextView
        if (Competition.current.isUka) {
            if (competitorMode) {
                competitor.cursor = position
                if (competitor.idUka > 0) {
                    listText1.text = "${competitor.fullName} - ${competitor.code} (${competitor.idUka})"
                } else {
                    listText1.text = "${competitor.fullName} - ${competitor.code}"
                }
            } else {
                dog.cursor = position
                if (!control.useIdUka) {
                    listText1.text = "${dog.handler.fullName} (${dog.cleanedPetName}) - ${dog.code}"
                } else {
                    listText1.text = "${dog.handler.fullName} (${dog.cleanedPetName}) - ${dog.idUka}"
                }
            }
        } else {
            dog.cursor = position
            listText1.text = "${dog.handler.fullName} (${dog.cleanedPetName})"
        }
    }

    override fun whenItemClick(position: Int) {
        if (Competition.current.isUka) {
            if (competitorMode) {
                competitor.cursor = position
                sendSignal(SignalCode.MEMBER_SELECTED, competitor.idAccount)

            } else {
                dog.cursor = position
                sendSignal(SignalCode.DOG_SELECTED, DogSelection(dog.id, dog.idAccount, dog.handler.id, dog.petName, dog.handler.fullName, dog.ukaHeightCodePerformance))
            }
        } else {
            dog.cursor = position
            CompetitionDog.enterAtShow(Competition.current.id, dog)
            sendSignal(SignalCode.DOG_SELECTED, DogSelection(dog.id, dog.idAccount, dog.handler.id, dog.petName, dog.handler.fullName, dog.ukaHeightCodePerformance))
        }
    }

    override fun whenLongClick(position: Int) {
        whenItemClick(position)
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        showKeyboardPanel()
        return false
    }

}
