/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*

/**
 * Created by mbrickman on 09/09/16.
 */

/*
    Purpose - Records height, grade and preference information use when creating pre-entries
*/

open class CompetitionDogRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) : DbTable<T>(_connection, "competitionDog", *columnNames) {
    open var idCompetition: Int by DbPropertyInt("idCompetition")
    open var idAccount: Int by DbPropertyInt("idAccount")
    open var idDog: Int by DbPropertyInt("idDog")
    open var entryType: Int by DbPropertyInt("entryType")
    open var ringNumber: Int by DbPropertyInt("ringNumber")
    open var nfc: Boolean by DbPropertyBoolean("nfc")
    open var group: String by DbPropertyString("group")
    open var extra: String by DbPropertyString("extra")
    open var flag: Boolean by DbPropertyBoolean("flag")

    // JSON properties
    open var kcExtra: JsonNode by DbPropertyJsonObject("extra", "kc")
    open var ukaExtra: JsonNode by DbPropertyJsonObject("extra", "uka")

    open var kcHandlerId: Int by DbPropertyJsonInt("extra", "kc.handlerId")
    open var kcHandler: String by DbPropertyJsonString("extra", "kc.handler")
    open var kcHeightCode: String by DbPropertyJsonString("extra", "kc.heightCode")
    open var kcGradeCode: String by DbPropertyJsonString("extra", "kc.gradeCode")
    open var kcJumpHeightCode: String by DbPropertyJsonString("extra", "kc.entryOption")

    open var indHandlerId: Int by DbPropertyJsonInt("extra", "independent.handlerId")
    open var indHandler: String by DbPropertyJsonString("extra", "independent.handler")
    open var indGradeCode: String by DbPropertyJsonString("extra", "independent.gradeCode")
    open var indJumpHeightCode: String by DbPropertyJsonString("extra", "independent.jumpHeightCode")
    open var indClearRoundOnly: Boolean by DbPropertyJsonBoolean("extra", "independent.cro")
    open var indSecondChance: Boolean by DbPropertyJsonBoolean("extra", "independent.secondChance")
    open var indSecondChanceIdEntry: Int by DbPropertyJsonInt("extra", "independent.secondChanceIdEntry")
    open var indBonusCategories: String by DbPropertyJsonString("extra", "independent.categories")
    open var indBundle: Int by DbPropertyJsonInt("extra", "independent.bundle")
    open var indG7Owner: Boolean by DbPropertyJsonBoolean("extra", "independent.g7Owner")

    // UKA extra

    open var ukaLevels: JsonNode by DbPropertyJsonObject("extra", "uka.level")
    open var ukaPerformanceLevel: String by DbPropertyJsonString("extra", "uka.level.performance")
    open var ukaSteeplechaseLevel: String by DbPropertyJsonString("extra", "uka.level.steeplechase")

    open var ukaNotPerformance: Boolean by DbPropertyJsonBoolean("extra", "uka.preference.notPerformance")
    open var ukaNotSteeplechase: Boolean by DbPropertyJsonBoolean("extra", "uka.preference.notSteeplechase")
    open var ukaCasual: Boolean by DbPropertyJsonBoolean("extra", "uka.preference.casual")
    open var ukaNursery: Boolean by DbPropertyJsonBoolean("extra", "uka.preference.nursery")
    open var ukaJunior: Boolean by DbPropertyJsonBoolean("extra", "uka.preference.junior")

    open var ukaHeightCodes: JsonNode by DbPropertyJsonObject("extra", "uka.preference.heightCode")
    open var ukaHeightCodePerformance: String by DbPropertyJsonString("extra", "uka.preference.heightCode.performance")
    open var ukaHeightCodeSteeplechase: String by DbPropertyJsonString("extra", "uka.preference.heightCode.steeplechase")
    open var ukaHeightCodeNursery: String by DbPropertyJsonString("extra", "uka.preference.heightCode.nursery")
    open var ukaHeightCodeCasual: String by DbPropertyJsonString("extra", "uka.preference.heightCode.casual")

    open var ukaHeightCode: String by DbPropertyJsonString("extra", "uka.heightCode")

    open var ukOpenHeightCode: String by DbPropertyJsonString("extra", "ukOpen.heightCode")
    open var ukOpenHandler: String by DbPropertyJsonString("extra", "ukOpen.handler")
    open var ukOpenHeightCodeLocked: Boolean by DbPropertyJsonBoolean("extra", "ukOpen.HeightCodeLocked")
    open var ukOpenNation: String by DbPropertyJsonString("extra", "ukOpen.Nation")
    open var ukOpenGroup: String by DbPropertyJsonString("extra", "ukOpen.group")
    open var ukOpenWithdrawn: Boolean by DbPropertyJsonBoolean("extra", "ukOpen.withdrawn")

    open var fabHeightCode: String by DbPropertyJsonString("extra", "fab.heightCode")
    open var fabGradeAgility: String by DbPropertyJsonString("extra", "fab.grade.agility")
    open var fabGradeJumping: String by DbPropertyJsonString("extra", "fab.grade.jumping")
    open var fabGradeSteeplechase: String by DbPropertyJsonString("extra", "fab.grade.steeplechase")
    open var fabCollie: Boolean by DbPropertyJsonBoolean("extra", "fab.collie")
    open var ifcsHeightCode: String by DbPropertyJsonString("extra", "ifcs.heightCode")


    val competition: Competition by DbLink<Competition>({ Competition() })
    val dog: Dog by DbLink<Dog>({ Dog() })
    val account: Account by DbLink<Account>({ Account() })


}

// select json_extract(extra, '$.uka.level.performance') from competitionDog

class CompetitionDog(vararg columnNames: String) : CompetitionDogRaw<CompetitionDog>(null, *columnNames) {

    constructor(idCompetition: Int, idDog: Int) : this() {
        find("idCompetition=$idCompetition AND idDog=$idDog")
    }

    fun findDog(idCompetition: Int, idDog: Int): Boolean {
        return find("idCompetition=$idCompetition AND idDog=$idDog")
    }

    fun options(idOrganization: Int, delimiter: String = ", "): String {
        var result=""
        when (idOrganization) {
            ORGANIZATION_KC -> result = "${Grade.getGradeName(kcGradeCode)}$delimiter${Height.getHeightJumpNameEx(kcJumpHeightCode)}"
            ORGANIZATION_INDEPENDENT -> result = "${Grade.getGradeName(indGradeCode)}$delimiter${Height.getHeightName(indJumpHeightCode)}${if(indClearRoundOnly) " CRO" else ""}"
        }
        return result
    }

    override var ukaHeightCodePerformance: String
        get() = if (super.ukaHeightCodePerformance.isEmpty() || super.ukaHeightCodePerformance < ukaHeightCode)
            ukaHeightCode else super.ukaHeightCodePerformance
        set(value) {
            super.ukaHeightCodePerformance = value
        }

    override var ukaHeightCodeSteeplechase: String
        get() = if (super.ukaHeightCodeSteeplechase.isEmpty() || super.ukaHeightCodeSteeplechase < ukaHeightCode)
            ukaHeightCode else super.ukaHeightCodeSteeplechase
        set(value) {
            super.ukaHeightCodeSteeplechase = value
        }

    override var ukaHeightCodeCasual: String
        get() = if (super.ukaHeightCodeCasual.isEmpty() || super.ukaHeightCodeCasual >= ukaHeightCode)
            Height.getCasualHeightCode(ukaHeightCode) else super.ukaHeightCodeCasual
        set(value) {
            super.ukaHeightCodeCasual = value
        }
    
    fun processKcGradeChange(newGradeCode: String, info: JsonNode = Json.nullNode(), force: Boolean = false): Boolean {
        val oldGrade = kcGradeCode
        var ok = true
        val json = if (info.isRoot && info.isNull) Json() else info
        if ((!competition.processed || force) && competition.isKc) {
            dbTransaction {
                clearMessages()
                Entry().join { agilityClass }.join { team }.join { agilityClass.competition }
                        .where("agilityClass.idCompetition=$idCompetition AND entry.gradeCode <> ${newGradeCode.quoted} AND team.idDog=$idDog",
                                "agilityClass.classDate, agilityClass.classNumber") {
                            if (ok) {
                                Entry().seek(getInt("idEntry")) { ok = kcGradeChange(newGradeCode, json) }
                            }
                        }
                if (ok) {
                    kcGradeCode = newGradeCode
                    post()
                    if (info.isNull) {
                        PlazaMessage.competitionGradeChangeOld(idAccount, account.emailList, competition.briefName, Grade.getGradeName(oldGrade), Grade.getGradeName(newGradeCode), dog.petName, json)
                    }
                }
            }

        }
        return ok
    }

    companion object {

        fun isEntered(idCompetition: Int, dog: Dog): Boolean {
            var entered = false
            dbQuery("SELECT idDog FROM competitionDog WHERE idCompetition=${Competition.current.id} AND idDog=${dog.id}") {
                entered = true
            }
            return entered
        }

            fun enterAtShow(idCompetition: Int, dog: Dog) {
            var entered= false
            dbQuery("SELECT idDog FROM competitionDog WHERE idCompetition=${Competition.current.id} AND idDog=${dog.id}") {
                entered = true
            }
            if (!entered) {
                val competitionDog = CompetitionDog()
                competitionDog.append()
                competitionDog.idCompetition = Competition.current.id
                competitionDog.idDog = dog.id
                competitionDog.entryType = ENTRY_AT_SHOW
                competitionDog.idAccount = dog.idAccount
                if (Competition.current.isFab) {
                    competitionDog.fabHeightCode = dog.fabHeightCode
                    competitionDog.fabGradeAgility = dog.fabGradeAgility
                    competitionDog.fabGradeJumping = dog.fabGradeJumping
                    competitionDog.fabGradeSteeplechase = dog.fabGradeSteeplechase
                    competitionDog.fabCollie = dog.fabCollie
                    competitionDog.ifcsHeightCode = dog.ifcsHeightCode
                } else {
                    competitionDog.nfc = false
                    competitionDog.kcHandlerId = dog.idCompetitorHandler
                    competitionDog.kcHandler = dog.handlerName
                    competitionDog.kcHeightCode = dog.kcHeightCode
                    competitionDog.kcGradeCode = dog.kcGradeCode
                    competitionDog.kcJumpHeightCode = dog.kcHeightCode
                }
                competitionDog.post()
            }
        }
        fun buildKc(idCompetition: Int) {

            dbExecute("UPDATE competitionDog SET flag=IF(nfc, FALSE, TRUE)  WHERE idCompetition=$idCompetition")

            dbQuery("""
                SELECT DISTINCT
                    team.idDog,
                    team.idTeam,
                    entry.entryType,
                    entry.idAccount,
                    entry.dogRingNumber,
                    entry.gradeCode,
                    entry.heightCode,
                    entry.jumpHeightCode,
                    dog.dogCode
                FROM
                    entry
                        JOIN
                    agilityClass USING (idAgilityClass)
                        JOIN
                    team USING (idTeam)
                        JOIN
                    dog USING (idDog)
                WHERE
                    agilityClass.idCompetition = $idCompetition AND entry.progress = $PROGRESS_ENTERED
                GROUP BY team.idDog
            """) {
                val idDog = getInt("idDog")
                val idTeam = getInt("idTeam")
                val idAccount = getInt("idAccount")
                val entryType = getInt("entryType")
                val ringNumber = getInt("dogRingNumber")
                val gradeCode = getString("gradeCode")
                val heightCode = getString("heightCode")
                val jumpHeightCode = getString("jumpHeightCode")
                val dogCode = getInt("dogCode")
                CompetitionDog().seekOrAppend("idCompetition=$idCompetition AND idDog=$idDog", onAppend = {
                    this.idCompetition = idCompetition
                    this.idDog = idDog
                    // this is a bit random as may pick up junior handler or alternative handler for a single class
                    val team = Team(idTeam)
                    kcHandler = team.getPrincipalName()
                    kcHandlerId = team.idCompetitor
                }) {
                    this.idAccount = idAccount
                    this.entryType = entryType
                    this.ringNumber = if (ringNumber > 0) ringNumber else dogCode
                    kcGradeCode = gradeCode
                    kcHeightCode = heightCode
                    kcJumpHeightCode = jumpHeightCode
                    flag = false
                    post()
                }
            }
            dbExecute("DELETE FROM competitionDog WHERE idCompetition=$idCompetition AND flag=TRUE")
        }

        fun buildUka(idCompetition: Int) {

            dbExecute("UPDATE competitionDog SET flag=TRUE WHERE idCompetition=$idCompetition")

            dbQuery("""
                SELECT DISTINCT
                    team.idDog,
                    team.idTeam,
                    entry.entryType,
                    entry.idAccount,
                    entry.dogRingNumber,
                    MAX(IF(agilityClass.classCode IN (${ClassTemplate.getProgrammeList(PROGRAMME_STEEPLECHASE)}),
                        agilityClass.gradeCodes,
                        '')) AS steeplechaseGrade,
                    MAX(IF(agilityClass.classCode IN (${ClassTemplate.getProgrammeList(PROGRAMME_PERFORMANCE)}),
                        agilityClass.gradeCodes,
                        '')) AS performanceGrade,
                    entry.heightCode,
                    MAX(IF(agilityClass.classCode IN (${ClassTemplate.getProgrammeList(PROGRAMME_STEEPLECHASE)}),
                        entry.jumpHeightCode,
                        '')) AS steeplechaseHeightCode,
                    MAX(IF(agilityClass.classCode IN (${ClassTemplate.getProgrammeList(PROGRAMME_PERFORMANCE)}),
                        entry.jumpHeightCode,
                        '')) AS performanceHeightCode,
                    MAX(IF(agilityClass.classCode IN (${ClassTemplate.NURSERY_AGILITY.code}),
                        entry.jumpHeightCode,
                        '')) AS nurseryHeightCode,
                    MAX(IF(agilityClass.classCode IN (${ClassTemplate.casualList}),
                        entry.jumpHeightCode,
                        '')) AS casualHeightCode,
                    dog.dogCode,
                    dog.extra
                FROM
                    entry
                        JOIN
                    agilityClass USING (idAgilityClass)
                        JOIN
                    team USING (idTeam)
                        JOIN
                    dog USING (idDog)
                WHERE
                    agilityClass.idCompetition = $idCompetition AND entry.progress = $PROGRESS_ENTERED
                GROUP BY team.idDog
            """) {
                val idDog = getInt("idDog")
                val idAccount = getInt("idAccount")
                val entryType = getInt("entryType")
                val steeplechaseGrade = getString("steeplechaseGrade")
                val performanceGrade = getString("performanceGrade")
                val heightCode = getString("heightCode")
                val steeplechaseHeightCode = getString("steeplechaseHeightCode")
                val performanceHeightCode = getString("performanceHeightCode")
                val nurseryHeightCode = getString("nurseryHeightCode")
                val casualHeightCode = getString("casualHeightCode")
                val dogCode = getInt("dogCode")
                val extra = Json(getString("extra"))
                CompetitionDog().seekOrAppend("idCompetition=$idCompetition AND idDog=$idDog", onAppend = {
                    this.idCompetition = idCompetition
                    this.idDog = idDog
                }) {
                    this.idAccount = idAccount
                    this.entryType = entryType
                    this.ringNumber = dogCode
                    this.ukaHeightCode = heightCode

                    this.ukaSteeplechaseLevel = if (steeplechaseGrade.isNotEmpty()) steeplechaseGrade else
                        extra["uka.level.steeplechase"].asString
                    this.ukaPerformanceLevel = if (performanceGrade.isNotEmpty()) performanceGrade else
                        extra["uka.level.performance"].asString
                    this.ukaHeightCodeSteeplechase = if (steeplechaseHeightCode.isNotEmpty()) steeplechaseHeightCode else
                        extra["uka.preference.heightCode.steeplechase"].asString
                    this.ukaHeightCodePerformance = if (performanceHeightCode.isNotEmpty()) performanceHeightCode else
                        extra["uka.preference.heightCode.performanc"].asString
                    this.ukaHeightCodeNursery = if (nurseryHeightCode.isNotEmpty()) nurseryHeightCode else
                        extra["uka.preference.heightCode.nursery"].asString
                    this.ukaHeightCodeCasual = if (casualHeightCode.isNotEmpty()) casualHeightCode else
                        extra["uka.preference.heightCode.casual"].asString

                    flag = false
                    post()
                }
            }


            dbExecute("DELETE FROM competitionDog WHERE idCompetition=$idCompetition AND flag=TRUE")

        }

        fun ringNumberToIdDog(idCompetition: Int, ringNumber: Int): Int {
            var result = 0
            CompetitionDog().seek("idCompetition=$idCompetition AND ringNumber=$ringNumber") { result = idDog }
            return result
        }

        fun kcAddLate(idCompetition: Int, idDog: Int, gradeCode: String, heightCode: String, jumpHeightCode: String, entryType: Int): Int {
            var result = 0
            CompetitionDog().seekOrAppend("idCompetition=$idCompetition AND idDog=$idDog",
                    onAppend = {
                        this.idCompetition = idCompetition
                        this.idDog = idDog
                        this.ringNumber = Dog(idDog).code
                        this.kcGradeCode = gradeCode
                        this.kcHeightCode = heightCode
                        this.kcJumpHeightCode = jumpHeightCode
                        this.entryType = entryType
                        post()
                    }) {
                mandate(kcGradeCode == gradeCode, "kcAddLate - attempt to add with different grade ($gradeCode should be $kcGradeCode")
                mandate(kcHeightCode == heightCode, "kcAddLate - attempt to add with different height ($heightCode should be $kcHeightCode")
                mandate(kcJumpHeightCode == jumpHeightCode, "kcAddLate - attempt to add with different jump height ($jumpHeightCode should be $kcJumpHeightCode")
                result = ringNumber
            }
            return result
        }

    }

}