package org.egility.library.general

import org.egility.library.dbobject.*
import java.util.ArrayList

/*
    open var fabHeightCode: String by DbPropertyJsonString("extra", "fab.heightCode")
    open var fabGradeAgility: String by DbPropertyJsonString("extra", "fab.grade.agility")
    open var fabGradeJumping: String by DbPropertyJsonString("extra", "fab.grade.jumping")
    open var fabGradeSteeplechase: String by DbPropertyJsonString("extra", "fab.grade.steeplechase")
    open var fabCollie: Boolean by DbPropertyJsonBoolean("extra", "fab.collie")
    open var ifcsHeightCode: String by DbPropertyJsonString("extra", "ifcs.heightCode")

 */

fun fabLateClassChanges(idCompetition: Int, idDog: Int, gradeAgility: String, gradeJumping: String, gradeSteeplechase: String, height: String, collie: Boolean, ifcsHeight: String, agilityClassIds: ArrayList<Int>) {

    dbTransaction {
        var idAccount = 0
        var handler = ""
        val idCompetitor = Dog(idDog).idCompetitorHandler
        CompetitionDog().seek("idCompetition=$idCompetition AND idDog=$idDog") {
            if (gradeAgility.isNotEmpty()) fabGradeAgility = gradeAgility
            if (gradeJumping.isNotEmpty()) fabGradeJumping = gradeJumping
            if (gradeSteeplechase.isNotEmpty()) fabGradeSteeplechase = gradeSteeplechase

            if (height.isNotEmpty()) fabHeightCode = height
            if (ifcsHeight.isNotEmpty()) ifcsHeightCode = ifcsHeight
            fabCollie = collie
            post()
            idAccount = this.idAccount
            handler = this.kcHandler
        }.otherwise {
            throw Wobbly("Dog $idDog not entered in competition $idCompetition (kcFixEntryByClassNumber)")
        }

        // update current classes
        Entry().join { team }.join { agilityClass }.where("AgilityClass.idCompetition=$idCompetition AND team.idDog=$idDog") {
            if (agilityClassIds.isNotEmpty() && agilityClassIds.contains(agilityClass.id)) {
                if (canUpdate) {
                    // still need this class - adjust details
                    when (agilityClass.entryRule) {
                        ENTRY_RULE_GRADE1 -> this.gradeCode = gradeAgility
                        ENTRY_RULE_GRADE2 -> this.gradeCode = gradeJumping
                        ENTRY_RULE_GRADE3 -> this.gradeCode = gradeSteeplechase
                        else -> this.gradeCode = if (agilityClass.isIfcs) "IF01" else gradeAgility
                    }
                    this.subDivision = if (agilityClass.isFab && collie) 1 else 0
                    this.heightCode = if (agilityClass.isIfcs) ifcsHeight else height
                    this.jumpHeightCode = if (agilityClass.isIfcs) ifcsHeight else height
                    subClass = agilityClass.chooseSubClass(gradeCode, heightCode, jumpHeightCode, this.subDivision)
                    if (this.jumpHeightCode != this.runningOrderJumpHeightCode) runningOrder = agilityClass.nextRunningOrder(this.jumpHeightCode)
                    this.runningOrderJumpHeightCode = this.jumpHeightCode
                    progress = PROGRESS_ENTERED
                }
            } else {
                progress = PROGRESS_REMOVED_LATE_CORRECTION
            }
            post()
            agilityClass.checkMinMaxRunningOrder()
            agilityClassIds.remove(agilityClass.id)

        }

        for (idAgilityClass in agilityClassIds) {
            fabLateEntry(idDog, idAccount, idCompetitor, idAgilityClass, gradeAgility, gradeJumping, gradeSteeplechase, height, collie, ifcsHeight, ENTRY_TRANSFER)
        }

    }
}

fun fabLateEntry(idDog: Int, idAccount: Int, idCompetitor: Int, idAgilityClass: Int, gradeAgility: String, gradeJumping: String, gradeSteeplechase: String, height: String, collie: Boolean, ifcsHeight: String, entryType: Int = ENTRY_AT_SHOW) {
    val idTeam = Team.getIndividualId(idCompetitor, idDog)

    AgilityClass().seek("idAgilityClass=$idAgilityClass") {

        val gradeCode = when (entryRule) {
            ENTRY_RULE_GRADE1 -> gradeAgility
            ENTRY_RULE_GRADE2 -> gradeJumping
            ENTRY_RULE_GRADE3 -> gradeSteeplechase
            else -> if (isIfcs) "IF01" else gradeAgility
        }
        val subDivision = if (isFab && collie) 1 else 0
        val heightCode = if (isIfcs) ifcsHeight else height
        val jumpHeightCode = if (isIfcs) ifcsHeight else height


        enter(
            idTeam = idTeam,
            idAccount = idAccount,
            gradeCode = gradeCode,
            heightCode = heightCode,
            jumpHeightCode = jumpHeightCode,
            subDivision = subDivision,
            entryType = entryType,
            runningOrder = -1,
            timeEntered = now
        )
        checkMinMaxRunningOrder()
    }
}