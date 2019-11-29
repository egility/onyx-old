/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import org.egility.library.dbobject.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by mbrickman on 23/05/17.
 */

class ClassEntry(val idAgilityClass: Int, val agilityClassCursor: Int, val entryCursor: Int, val isEligible: Boolean, val isTeam: Boolean, var checked: Boolean) {
    val hasEntry: Boolean
        get() = entryCursor > -1

    val canChange: Boolean
        get() = isEligible && !isTeam
}

object CompetitorServicesData {

    var proposedHeightCode = ""
    var proposedJumpHeightCode = ""
    var proposedGradeCode = ""
    var proposedGroup = ""

    var proposedFabHeightCode = ""
    var proposedIfcsHeightCode = ""
    var proposedFabGradeAgility = ""
    var proposedFabGradeJumping = ""
    var proposedFabGradeSteeplechase = ""
    var proposedFabCollie = false
    
    val dogMenuStack = Stack<String>()


    val classEntries = ArrayList<ClassEntry>()

    val competitor = Competitor()
    val dog = Dog()
    val entry = Entry()
    val agilityClass = AgilityClass()
    val competitionDog = CompetitionDog()

    init {
        entry.agilityClass.joinToParent()
    }

    fun focus(position: Int): ClassEntry {
        debug("CompetitorServicesData.focus", "${position}")
        val result = classEntries[position]
        agilityClass.cursor = result.agilityClassCursor
        if (result.entryCursor > -1) {
            entry.cursor = result.entryCursor
        } else {
            entry.beforeFirst()
        }
        return result
    }

    fun selectDog(idDog: Int) {
        dog.find(idDog)
        competitor.find(dog.idCompetitor)
        competitionDog.findDog(Competition.current.id, idDog)
        clearProposed()
    }
    
    fun clearProposed() {
        if (Competition.isFab) {
            proposedFabHeightCode = competitionDog.fabHeightCode
            proposedIfcsHeightCode = competitionDog.ifcsHeightCode
            proposedFabGradeAgility = competitionDog.fabGradeAgility
            proposedFabGradeJumping = competitionDog.fabGradeJumping
            proposedFabGradeSteeplechase = competitionDog.fabGradeSteeplechase
            proposedFabCollie = competitionDog.fabCollie
        } else {
            proposedHeightCode = competitionDog.kcHeightCode
            proposedJumpHeightCode = competitionDog.kcJumpHeightCode
            proposedGradeCode = competitionDog.kcGradeCode
        }
        classEntries.clear()
    }

    fun changeFabGrade(programme: Int, newGrade: String) {
        dbTransaction {
            when (programme) {
                1 -> competitionDog.fabGradeAgility = newGrade
                2 -> competitionDog.fabGradeJumping = newGrade
                3 -> competitionDog.fabGradeSteeplechase = newGrade
            }
            competitionDog.post()
            dog.moveToGradeFabAtShow(Competition.current.id, programme, newGrade)
        }
        proposedFabGradeAgility = competitionDog.fabGradeAgility
        proposedFabGradeJumping = competitionDog.fabGradeJumping
        proposedFabGradeSteeplechase = competitionDog.fabGradeSteeplechase
    }

    fun changeFabHeight(organization: Int, newHeight: String) {
        dbTransaction {
            when (organization) {
                ORGANIZATION_FAB -> competitionDog.fabHeightCode = newHeight
                ORGANIZATION_IFCS -> competitionDog.ifcsHeightCode = newHeight
            }
            competitionDog.post()
            dog.moveToHeightFabAtShow(Competition.current.id, organization, newHeight)
        }
        proposedFabHeightCode = competitionDog.fabHeightCode
        proposedIfcsHeightCode = competitionDog.ifcsHeightCode
    }
    
    fun changeFabDivision(subDivision: Int) {
        dbTransaction {
            competitionDog.fabCollie = subDivision == 1
            competitionDog.post()
            dog.fabCollie = subDivision == 1
            dog.post()
            dog.changeFabSubdivisionAtShow(Competition.current.id, subDivision)
        }
        proposedFabCollie = competitionDog.fabCollie
    }

    fun changeUkOpenHeight(newHeight: String) {
        dbTransaction {
            competitionDog.ukOpenHeightCode = newHeight
            competitionDog.post()
            dog.moveToHeightUkOpenAtShow(Competition.current.id, newHeight)
        }
    }

    fun changeUkOpenGroup(newGroup: String) {
        dbTransaction {
            competitionDog.ukOpenGroup = newGroup
            competitionDog.post()
            dog.moveToGroupUkOpenAtShow(Competition.current.id, newGroup)
        }
    }

    fun addClassAction(isEligible: Boolean, entryCursor: Int) {
        classEntries.add(ClassEntry(
                agilityClass.id,
                agilityClass.cursor,
                entryCursor,
                isEligible,
                agilityClass.template.teamSize > 1,
                isEligible && entry.isOnRow && entry.progress != PROGRESS_ENTRY_DELETED)
        )
    }

    val isUpdated: Boolean
        get() {
            if (Competition.isKc && (proposedGradeCode != competitionDog.kcGradeCode || proposedHeightCode != competitionDog.kcHeightCode || proposedJumpHeightCode != competitionDog.kcJumpHeightCode)) {
                return true
            }
            if (Competition.isFab && (
                        proposedFabGradeAgility != competitionDog.fabGradeAgility ||
                                proposedFabGradeJumping != competitionDog.fabGradeJumping ||
                                proposedFabGradeSteeplechase != competitionDog.fabGradeSteeplechase ||
                                proposedFabCollie != competitionDog.fabCollie ||
                                proposedFabHeightCode != competitionDog.fabHeightCode ||
                                proposedIfcsHeightCode != competitionDog.ifcsHeightCode)) {
                return true
            }
            for (classEntry in classEntries) {
                if ((classEntry.hasEntry && entry.progress != PROGRESS_ENTRY_DELETED) != classEntry.checked) {
                    return true
                }
            }
            return false
        }

    fun saveProposed(agilityClassIds : ArrayList<Int>) {
        if (Competition.isFab) {
                fabLateClassChanges(Competition.current.id, competitionDog.idDog, proposedFabGradeAgility, 
                    proposedFabGradeJumping, proposedFabGradeSteeplechase, proposedFabHeightCode, proposedFabCollie, 
                    proposedIfcsHeightCode, agilityClassIds) 
            } else {
                kcLateClassChanges(Competition.current.id, competitionDog.idDog, proposedGradeCode, proposedHeightCode, 
                    proposedJumpHeightCode, agilityClassIds)
        }
        selectDog(dog.id)
    }


}

