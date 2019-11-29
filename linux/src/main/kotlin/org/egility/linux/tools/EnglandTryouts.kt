/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.tools

import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Dog
import org.egility.library.dbobject.Height
import org.egility.library.dbobject.Team
import org.egility.library.general.*

/**
 * Created by mbrickman on 25/09/17.
 */

object EnglandTryouts {


    val A4E_TRY_OUT = 1791590773
    val A4E_PENTATHLON = 1404248772
    val A4E_GAMES = 1310257227

    val A4E_AGILITY_1 = 1955118202
    val A4E_JUMPING_1 = 1327215204
    val A4E_JUMPING_2 = 1258922410
    val A4E_AGILITY_2 = 1806981002
    val A4E_SPEEDSTAKES = 1756992903
    val A4E_SNOOKER = 1164440655
    val A4E_GAMBLERS = 1328531945

    val A4E_CLASSES = "1791590773,1404248772,1310257227,1955118202,1327215204,1164440655,1258922410,1328531945,1806981002,1756992903"


    fun prepareShow(idCompetition: Int) {
        val agilityClass = AgilityClass()
        agilityClass.find("idCompetition=$idCompetition AND classCode=${ClassTemplate.TRY_OUT_PENTATHLON.code}")
        agilityClass.prepareClass()
        agilityClass.find("idCompetition=$idCompetition AND classCode=${ClassTemplate.TRY_OUT_GAMES.code}")
        agilityClass.prepareClass()
    }


    fun aeHeightChange(idUkaDog: Int, jumpHeightCode: String) {
        val dog = Dog()
        dog.find("idUka = $idUkaDog")
        if (dog.found()) {
            println("${dog.owner.fullName} & ${dog.cleanedPetName} to ${Height.getHeightName(jumpHeightCode)}")
            dbExecute("""
            UPDATE entry JOIN team USING (idTeam)
            SET entry.jumpHeightCode=${jumpHeightCode.quoted}, entry.heightCode=${jumpHeightCode.quoted}
            WHERE idAgilityClass IN ($A4E_CLASSES) AND team.idDog=${dog.id}
            """)
        }
    }

    fun aeWithdraw(idUkaDog: Int) {
        val dog = Dog()
        dog.find("idUka = $idUkaDog")
        println("${dog.owner.fullName} & ${dog.cleanedPetName} removed")
        if (dog.found()) {
            dbExecute("""
            UPDATE entry JOIN team USING (idTeam)
            SET entry.progress=${PROGRESS_WITHDRAWN}
            WHERE idAgilityClass IN ($A4E_CLASSES) AND team.idDog=${dog.id}
            """)
        }
    }

    fun aeAdd(idUkaDog: Int, jumpHeightCode: String) {
        val dog = Dog()
        dog.join(dog.owner)
        val agilityClass = AgilityClass()
        agilityClass.find(A4E_TRY_OUT)
        dog.find("idUka = $idUkaDog")
        if (dog.found()) {
            val idTeamOwner = Team.getIndividualId(dog.idCompetitor, dog.id)
            agilityClass.enter(
                    idTeam = idTeamOwner,
                    idAccount = dog.owner.getAccountID(),
                    heightCode = jumpHeightCode,
                    entryType = ENTRY_IMPORTED_EXCEL,
                    fee = 3500,
                    timeEntered = now
            )
        }
    }

}

