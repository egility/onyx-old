package org.egility.library.general

import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Competition
import java.util.*

object IndUtils {


    fun updateClass(
        competition: Competition,
        template: ClassTemplate,
        idAgilityClass: Int = 0,
        classNumber: Int = 0,
        classNumberSuffix: String = "",
        classDate: Date,
        gradeCodes: String = "",
        heightCodes: String = "",
        name: String = "",
        rules: String = "",
        entryFee: Int = 0,
        entryFeeMembers: Int = 0
    ): Int {

        val agilityClass = AgilityClass()
        if (idAgilityClass > 0) {
            agilityClass.find("idCompetition=${competition.id} AND idAgilityClass=$idAgilityClass")
        } else if (classNumber > 0) {
            agilityClass.find("idCompetition=${competition.id} AND classNumber=$classNumber AND classNumberSuffix='$classNumberSuffix'")
        }
        if (!agilityClass.found()) {
            agilityClass.append()
        }
        agilityClass.idCompetition = competition.id
        agilityClass.date = classDate
        if (classNumber > 0) agilityClass.number = classNumber
        if (classNumberSuffix.isNotEmpty()) agilityClass.numberSuffix = classNumberSuffix
        agilityClass.code = template.code
        if (heightCodes.isNotEmpty()) agilityClass.heightCodes = heightCodes
        if (gradeCodes.isNotEmpty()) agilityClass.gradeCodes = gradeCodes
        if (entryFee > 0) agilityClass.entryFee = entryFee
        if (entryFeeMembers > 0) agilityClass.entryFeeMembers = entryFeeMembers

        agilityClass.heightOptions = heightCodes
        agilityClass.heightCodes = heightCodes.replace(";", ",")
        agilityClass.jumpHeightCodes = heightCodes
        agilityClass.heightRunningOrder = heightCodes.replace(";", ",")
        agilityClass.name = name
        agilityClass.nameLong = name
        agilityClass.ageBaseDate = if (template.ageLower > 0 || template.ageUpper > 0) competition.ageBaseDate else nullDate

        agilityClass.flag = false
        agilityClass.rules = rules

        agilityClass.post()

        return agilityClass.id
    }

}