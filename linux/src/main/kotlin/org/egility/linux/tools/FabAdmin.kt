/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.tools

import org.egility.library.dbobject.*
import org.egility.library.general.*
import java.util.*

/**
 * Created by mbrickman on 01/10/17.
 */
object FabAdmin {
    
    fun instanceToSuffix(instance: Int): String {
        //return ('A'.toInt() + instance).toChar().toString()
        return "${instance+1}"
    }
    
    fun addFab(idCompetition: Int, classCode: Int, classCodeInstance: Int, classDate: Date,
               gradeCode: String, suffix: Boolean = false, entryFee: Int = 0, idAgilityClassParent: Int? = null): Int {

        val gradeName = Grade.getGradeName(gradeCode)
        val template = ClassTemplate.select(classCode)
        val agilityClass = AgilityClass()

        agilityClass.append()
        agilityClass.idCompetition = idCompetition
        agilityClass.code = classCode
        agilityClass.codeInstance = classCodeInstance
        agilityClass.suffix = if (suffix) instanceToSuffix(classCodeInstance) else ""
        agilityClass.date = classDate
        if (gradeCode.isNotEmpty() && template.oneOf(ClassTemplate.FAB_AGILITY_FINALS, ClassTemplate.FAB_JUMPING_FINALS, ClassTemplate.FAB_STEEPLECHASE_FINALS)) {
            when (gradeCode) {
                "FAB01" -> agilityClass.gradeCodes = "FAB01,FAB02,FAB03"
                "FAB02" -> agilityClass.gradeCodes = "FAB02,FAB03"
                "FAB03" -> agilityClass.gradeCodes = "FAB03"
            }
        } else if (gradeCode.isNotEmpty()) {
            agilityClass.gradeCodes = gradeCode
        } else if (template.isIfcs) {
            agilityClass.gradeCodes = "IF01"
        } else  {
            agilityClass.gradeCodes = "FAB01,FAB02,FAB03"
        }
        agilityClass.entryFee = entryFee
        agilityClass.heightOptions = template.heightOptions
        agilityClass.heightCodes = template.heightCodes
        agilityClass.jumpHeightCodes = template.jumpHeightCodes
        agilityClass.subDivisions = template.subDivisions
        if (idAgilityClassParent != null) agilityClass.idAgilityClassParent = idAgilityClassParent
        agilityClass.name = agilityClass.shortDescription
        agilityClass.nameLong = agilityClass.description
        agilityClass.post()

        return agilityClass.id
    }

    fun dropFab(idCompetition: Int, classCode: Int, classCodeInstance: Int, classDate: Date, gradeCode: String) {
        val agilityClass = AgilityClass()
        agilityClass.select(
                if (gradeCode.isEmpty())
                    "idCompetition=$idCompetition AND classCode=$classCode AND classCodeInstance=$classCodeInstance AND classDate=${classDate.sqlDate}"
                else
                    "idCompetition=$idCompetition AND classCode=$classCode AND classCodeInstance=$classCodeInstance AND classDate=${classDate.sqlDate} AND gradeCodes=${gradeCode.quoted}"
        )
        if (agilityClass.found()) {
            agilityClass.delete()
        }
    }

    fun checkSuffixFab(idCompetition: Int, classCode: Int, classCodeInstance: Int, classDate: Date, gradeCode: String, suffix: Boolean = false) {
        val template = ClassTemplate.select(classCode)
        val agilityClass = AgilityClass()
        agilityClass.select(
                if (gradeCode.isEmpty())
                    "idCompetition=$idCompetition AND classCode=$classCode AND classCodeInstance=$classCodeInstance AND classDate=${classDate.sqlDate}"
                else
                    "idCompetition=$idCompetition AND classCode=$classCode AND classCodeInstance=$classCodeInstance AND classDate=${classDate.sqlDate} AND gradeCodes=${gradeCode.quoted}"
        )
        if (agilityClass.found()) {
            agilityClass.suffix = if (suffix) instanceToSuffix(classCodeInstance) else ""
            agilityClass.name = agilityClass.shortDescription
            agilityClass.nameLong = agilityClass.description
            agilityClass.post()
        }
    }


    fun adjustClassCountFab(idCompetition: Int, classCode: Int, date: Date, current: Int, proposed: Int) {
        val template = ClassTemplate.select(classCode)
        debug("adjustClassCount", "classCode=$classCode, date=${date.shortText}, from=$current, to=$proposed")
        for (instance in 0..maxOf(current, proposed) - 1) {
            if (instance > proposed - 1) {
                if (template.isGraded) {
                    dropFab(idCompetition, classCode, instance, date, "FAB01")
                    dropFab(idCompetition, classCode, instance, date, "FAB02")
                    dropFab(idCompetition, classCode, instance, date, "FAB03")
                } else {
                    dropFab(idCompetition, classCode, instance, date, "")
                }
            } else if (instance > current - 1) {
                if (template.isGraded) {
                    addFab(idCompetition, classCode, instance, date, "FAB01", suffix = proposed > 1)
                    addFab(idCompetition, classCode, instance, date, "FAB02", suffix = proposed > 1)
                    addFab(idCompetition, classCode, instance, date, "FAB03", suffix = proposed > 1)
                } else {
                    addFab(idCompetition, classCode, instance, date, "", suffix = proposed > 1)
                }
            } else {
                if (template.isGraded) {
                    checkSuffixFab(idCompetition, classCode, instance, date, "FAB01", suffix = proposed > 1)
                    checkSuffixFab(idCompetition, classCode, instance, date, "FAB02", suffix = proposed > 1)
                    checkSuffixFab(idCompetition, classCode, instance, date, "FAB03", suffix = proposed > 1)
                } else {
                    checkSuffixFab(idCompetition, classCode, instance, date, "", suffix = proposed > 1)
                }
            }
        }
    }

    fun splitClassByGrade(idAgilityClass: Int, gradeCodesPartB: String): Int {
        val partA = AgilityClass(idAgilityClass)
        val partB = AgilityClass()
        var name = partA.name
        var gradeCodesPartA = ""
        var partBList = gradeCodesPartB.replace(" ", "").split(",")
        var delimiter = if (partA.gradeCodes.contains(";")) ";" else ","
        partA.gradeCodes.split(delimiter).forEach {
            if (!partBList.contains(it)) {
                gradeCodesPartA = gradeCodesPartA.append(it, delimiter)
            }
        }

        partB.append()
        partB.cloneFrom(partA, "idAgilityClass")

        partA.gradeCodes=gradeCodesPartA
        partB.gradeCodes=gradeCodesPartB.replace(",", delimiter)

        partA.numberSuffix = "a"
        partB.numberSuffix = "b"

        partA.partType = PART_TYPE_GRADE_SPLIT
        partB.partType = PART_TYPE_GRADE_SPLIT

        partA.name = "$name (Part A)"
        partB.name = "$name (Part B)"

        partA.nameLong = "$name (Part A)"
        partB.nameLong = "$name (Part B)"

        partA.post()
        partB.post()

        Entry().where("idAgilityClass=$idAgilityClass") {
            if (partBList.contains(gradeCode)) {
                this.idAgilityClass = partB.id
                subClass = partB.chooseSubClass(gradeCode, heightCode, jumpHeightCode, 0)
            } else {
                subClass = partA.chooseSubClass(gradeCode, heightCode, jumpHeightCode, 0)
            }
            post()
        }

        return partB.id
    }

}

