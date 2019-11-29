/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.dbobject

import org.egility.library.database.*
import org.egility.library.general.*
import java.util.*
import kotlin.collections.ArrayList

/*
ALTER TABLE `sandstone`.`dog` 
DROP COLUMN `ukaState`,
DROP COLUMN `unusedFlag`,
DROP COLUMN `accountStatus`,
DROP COLUMN `ukaRedCard`,
DROP COLUMN `idAccountOld`,
DROP COLUMN `dogFlags`,
DROP COLUMN `dataProviderDogCode`,
DROP COLUMN `dataProviderDogId`,
DROP COLUMN `dataProvider`,
DROP COLUMN `ukaAliases`,
DROP COLUMN `ukaPending`,
DROP COLUMN `otherBreedName`,
DROP COLUMN `mixedBreed`,
DROP INDEX `dataProvider` ;
 */

open class DogRaw<T : DbTable<T>>(_connection: DbConnection? = null, vararg columnNames: String) :
    DbTable<T>(_connection, "dog", *columnNames) {

    open var id: Int by DbPropertyInt("idDog")
    open var idCompetitor: Int by DbPropertyInt("idCompetitor")
    open var idCompetitorHandler: Int by DbPropertyInt("idCompetitorHandler")
    open var idAccount: Int by DbPropertyInt("idAccount")
    open var idAccountShared: Int by DbPropertyInt("idAccountShared")
    open var code: Int by DbPropertyInt("dogCode")
    open var registeredName: String by DbPropertyString("registeredName")
    open var petName: String by DbPropertyString("petName")
    open var dateOfBirth: Date by DbPropertyDate("dateOfBirth")
    open var idKC: String by DbPropertyString("idKC")
    open var idUka: Int by DbPropertyInt("idUka")
    open var gender: Int by DbPropertyInt("gender")
    open var idBreed: Int by DbPropertyInt("idBreed")
    open var state: Int by DbPropertyInt("dogState")
    open var ukaDateConfirmed: Date by DbPropertyDate("ukaDateConfirmed")
    open var aliasFor: Int by DbPropertyInt("aliasFor")
    open var extra: Json by DbPropertyJson("extra")

    open var dateCreated: Date by DbPropertyDate("dateCreated")
    open var deviceCreated: Int by DbPropertyInt("deviceCreated")
    open var dateModified: Date by DbPropertyDate("dateModified")
    open var deviceModified: Int by DbPropertyInt("deviceModified")
    open var dateDeleted: Date by DbPropertyDate("dateDeleted")


    open var kcExtra: JsonNode by DbPropertyJsonObject("extra", "kc")
    open var ukaExtra: JsonNode by DbPropertyJsonObject("extra", "uka")

    open var kcHeightCode: String by DbPropertyJsonString("extra", "kc.heightCode")
    open var kcPointsProgression: Int by DbPropertyJsonInt("extra", "kc.points")
    open var kcGradeArray: JsonNode by DbPropertyJsonObject("extra", "kc.grade")
    open var kcGrade1: Date by DbPropertyJsonDate("extra", "kc.grade.0")
    open var kcGrade2: Date by DbPropertyJsonDate("extra", "kc.grade.1")
    open var kcGrade3: Date by DbPropertyJsonDate("extra", "kc.grade.2")
    open var kcGrade4: Date by DbPropertyJsonDate("extra", "kc.grade.3")
    open var kcGrade5: Date by DbPropertyJsonDate("extra", "kc.grade.4")
    open var kcGrade6: Date by DbPropertyJsonDate("extra", "kc.grade.5")
    open var kcGrade7: Date by DbPropertyJsonDate("extra", "kc.grade.6")
    open var kcGradeHoldArray: JsonNode by DbPropertyJsonObject("extra", "kc.gradeHold")
    open var kcEntryOption: String by DbPropertyJsonString("extra", "kc.entryOption")


    open var kcWarrantLevel: Int by DbPropertyJsonInt("extra", "kc.agilityWarrant")
    open var kcChampion: Boolean by DbPropertyJsonBoolean("extra", "kc.champion")
    open var kcAgilityChampion: Boolean by DbPropertyJsonBoolean("extra", "kc.agilityChampion")
    open var kcShowCertificateMerit: Boolean by DbPropertyJsonBoolean("extra", "kc.merit")
    open var kcJuniorWarrant: Boolean by DbPropertyJsonBoolean("extra", "kc.juniorWarrant")
    open var kcObedienceWarrant: Boolean by DbPropertyJsonBoolean("extra", "kc.obedienceWarrant")
    open var kcRallyLevel: Int by DbPropertyJsonInt("extra", "kc.rallyLevel")
    open var kcRallyLevelExcellent: Boolean by DbPropertyJsonBoolean("extra", "kc.rallyLevelEx")

    open var kcChampWins: JsonNode by DbPropertyJsonObject("extra", "kc.champWins")

    open var ownerType: Int by DbPropertyJsonInt("extra", "owner.type")
    open var ownerName: String by DbPropertyJsonString("extra", "owner.name")
    open var ownerAddress: String by DbPropertyJsonString("extra", "owner.address")

    // UKA extra
    open var ukaBarred: Boolean by DbPropertyJsonBoolean("extra", "uka.barred")
    open var ukaBarredReason: String by DbPropertyJsonString("extra", "uka.barredReason")
    open var ukaAliases: String by DbPropertyJsonString("extra", "uka.aliases")

    open var ukaRegistrationType: Int by DbPropertyJsonInt("extra", "uka.registration.type")
    open var ukaRegistrationCompetition: Int by DbPropertyJsonInt("extra", "uka.registration.idCompetition")

    open var ukaEntryLevel: String by DbPropertyJsonString("extra", "uka.level.initial")
    open var ukaChampEquiv: Boolean by DbPropertyJsonBoolean("extra", "uka.level.ukaChampEquiv")
    open var ukaPerformanceLevel: String by DbPropertyJsonString("extra", "uka.level.performance")
    open var ukaSteeplechaseLevel: String by DbPropertyJsonString("extra", "uka.level.steeplechase")
    open var ukaQualifications: String by DbPropertyJsonString("extra", "uka.qualifications")

    open var ukaMeasure: JsonNode by DbPropertyJsonObject("extra", "uka.height.measure")
    open var ukaMeasure1: Int by DbPropertyJsonInt("extra", "uka.height.measure.0")
    open var ukaMeasure2: Int by DbPropertyJsonInt("extra", "uka.height.measure.1")
    open var ukaMeasure3: Int by DbPropertyJsonInt("extra", "uka.height.measure.2")
    open var ukaMeasureProvisional: Boolean by DbPropertyJsonBoolean("extra", "uka.height.provisional")
    open var ukaMeasureDisputed: Boolean by DbPropertyJsonBoolean("extra", "uka.height.disputed")
    open var ukaDeclaredHeightCode: String by DbPropertyJsonString("extra", "uka.height.declared")

    open var ukaNotPerformance: Boolean by DbPropertyJsonBoolean("extra", "uka.preference.notPerformance")
    open var ukaNotSteeplechase: Boolean by DbPropertyJsonBoolean("extra", "uka.preference.notSteeplechase")
    open var ukaCasual: Boolean by DbPropertyJsonBoolean("extra", "uka.preference.casual")
    open var ukaNursery: Boolean by DbPropertyJsonBoolean("extra", "uka.preference.nursery")
    open var ukaJunior: Boolean by DbPropertyJsonBoolean("extra", "uka.preference.junior")

    open var ukaHeightCodes: JsonNode by DbPropertyJsonObject("extra", "uka.preference.heightCode")
    open var ukaHeightCodePerformance: String by DbPropertyJsonString("extra", "uka.preference.heightCode.performance")
    open var ukaHeightCodeSteeplechase: String by DbPropertyJsonString(
        "extra",
        "uka.preference.heightCode.steeplechase"
    )
    open var ukaHeightCodeNursery: String by DbPropertyJsonString("extra", "uka.preference.heightCode.nursery")
    open var ukaHeightCodeCasual: String by DbPropertyJsonString("extra", "uka.preference.heightCode.casual")

    open var ukaBeginnersAgility: Int by DbPropertyJsonInt("extra", "uka.progress.beginners.agility")
    open var ukaBeginnersJumping: Int by DbPropertyJsonInt("extra", "uka.progress.beginners.jumping")
    open var ukaBeginnersGames: Int by DbPropertyJsonInt("extra", "uka.progress.beginners.games")
    open var ukaBeginnersSteeplechase: Int by DbPropertyJsonInt("extra", "uka.progress.beginners.steeplechase")

    open var ukaNoviceAgility: Int by DbPropertyJsonInt("extra", "uka.progress.novice.agility")
    open var ukaNoviceJumping: Int by DbPropertyJsonInt("extra", "uka.progress.novice.jumping")
    open var ukaNoviceGames: Int by DbPropertyJsonInt("extra", "uka.progress.novice.games")
    open var ukaNoviceSteeplechase: Int by DbPropertyJsonInt("extra", "uka.progress.novice.steeplechase")

    open var ukaSeniorAgility: Int by DbPropertyJsonInt("extra", "uka.progress.senior.agility")
    open var ukaSeniorJumping: Int by DbPropertyJsonInt("extra", "uka.progress.senior.jumping")
    open var ukaSeniorGames: Int by DbPropertyJsonInt("extra", "uka.progress.senior.games")
    open var ukaSeniorSteeplechase: Int by DbPropertyJsonInt("extra", "uka.progress.senior.steeplechase")

    open var ukaChampAgility: Int by DbPropertyJsonInt("extra", "uka.progress.champ.agility")
    open var ukaChampJumping: Int by DbPropertyJsonInt("extra", "uka.progress.champ.jumping")
    open var ukaChampGames: Int by DbPropertyJsonInt("extra", "uka.progress.champ.games")
    open var ukaChampSteeplechase: Int by DbPropertyJsonInt("extra", "uka.progress.champ.steeplechase")

    open var ukaChampWinAgility: Int by DbPropertyJsonInt("extra", "uka.progress.champWin.agility")
    open var ukaChampWinJumping: Int by DbPropertyJsonInt("extra", "uka.progress.champWin.jumping")
    open var ukaChampWinGames: Int by DbPropertyJsonInt("extra", "uka.progress.champWin.games")
    open var ukaChampWinSteeplechase: Int by DbPropertyJsonInt("extra", "uka.progress.champWin.steeplechase")

    open var ukOpenHeightCode: String by DbPropertyJsonString("extra", "ukOpen.heightCode")
    open var ukOpenNation: String by DbPropertyJsonString("extra", "ukOpen.nation")


    // FAB extra
    open var fabHeightCode: String by DbPropertyJsonString("extra", "fab.heightCode")
    open var fabGradeAgility: String by DbPropertyJsonString("extra", "fab.grade.agility")
    open var fabGradeJumping: String by DbPropertyJsonString("extra", "fab.grade.jumping")
    open var fabGradeSteeplechase: String by DbPropertyJsonString("extra", "fab.grade.steeplechase")

    open var fabNotAgility: Boolean by DbPropertyJsonBoolean("extra", "fab.preference.notAgility")
    open var fabNotJumping: Boolean by DbPropertyJsonBoolean("extra", "fab.preference.notJumping")
    open var fabNotSteeplechase: Boolean by DbPropertyJsonBoolean("extra", "fab.preference.notSteeplechase")
    open var fabGrandPrix: Boolean by DbPropertyJsonBoolean("extra", "fab.preference.grandPrix")
    open var fabAllsorts: Boolean by DbPropertyJsonBoolean("extra", "fab.preference.allsorts")
    open var fabIfcs: Boolean by DbPropertyJsonBoolean("extra", "fab.preference.ifcs")
    open var fabCollieState: Int by DbPropertyJsonInt("extra", "fab.collie")

    open var ifcsHeightCode: String by DbPropertyJsonString("extra", "ifcs.heightCode")

    open var archivedState: Int by DbPropertyJsonInt("extra", "archived.state")
    open var archivedCode: Int by DbPropertyJsonInt("extra", "archived.code")

    open var shareCode: String by DbPropertyJsonString("extra", "share.code")

    val owner: Competitor by DbLink<Competitor>({ Competitor() }, label = "owner")
    val handler: Competitor by DbLink<Competitor>({ Competitor() }, label = "handler", keyNames = *arrayOf("idCompetitorHandler"))
    val breed: Breed by DbLink<Breed>({ Breed() })
    val account: Account by DbLink<Account>({ Account() })

}

class Dog(vararg columnNames: String, connection: DbConnection? = null) : DogRaw<Dog>(connection, *columnNames) {

    constructor(idDog: Int) : this() {
        find(idDog)
    }

    val isCollie: Boolean
        get() = idBreed.oneOf(49, 1007, 9001)

    var fabCollie: Boolean
        get() = if (fabCollieState == 0) isCollie else fabCollieState == 1
        set(value) {
            fabCollieState = if (value) 1 else 2
        }

    val ukaRegistered: Boolean
        get() = ukaDateConfirmed.isNotEmpty()

    val ownerText: String
        get() = when (ownerType) {
            DOG_OWNER_SINGLE -> "${owner.fullName}, ${account.fullAddress.asCommaLine}"
            DOG_OWNER_HOUSEHOLD -> "$ownerName, ${account.fullAddress.asCommaLine}"
            else -> "$ownerName, $ownerAddress"
        }


    val ukaTitle: String
        get() {
            val delimiter = " "
            val result = java.lang.StringBuilder("")
            var winCount = 0
            var champCount = 0
            if (ukaChampAgility + ukaChampJumping + ukaChampGames >= 60) {
                val count = minInt((ukaChampAgility + ukaChampJumping + ukaChampGames) / 60, ukaChampAgility/12, ukaChampJumping/12, ukaChampGames/12)
                if (count>0) champCount++
                if (count==1)
                    result.delimiterAppend("CAP", delimiter)
                else if (count>=1)
                    result.delimiterAppend("CAP$count", delimiter)
            }
            if (ukaChampWinAgility + ukaChampWinJumping + ukaChampWinGames >= 60) {
                val count = minInt((ukaChampWinAgility + ukaChampWinJumping + ukaChampWinGames) / 60, ukaChampWinAgility/12, ukaChampWinJumping/12, ukaChampWinGames/12)
                if (count>0) winCount++
                if (count==1) 
                    result.delimiterAppend("WCAP", delimiter)
                else if (count>=1) 
                    result.delimiterAppend("WCAP$count", delimiter)
            }
            if (ukaChampSteeplechase>=60) {
                val count = ukaChampSteeplechase / 60
                if (count>0) champCount++
                if (count==1)
                    result.delimiterAppend("CSC", delimiter)
                else if (count>=1)
                    result.delimiterAppend("CSC$count", delimiter)
            }
            if (ukaChampWinSteeplechase>=60) {
                val count = ukaChampWinSteeplechase / 60
                if (count>0) winCount++
                if (count==1)
                    result.delimiterAppend("WCSC", delimiter)
                else if (count>=1)
                    result.delimiterAppend("WCSC$count", delimiter)
            }
            if (champCount==2) {
                result.delimiterAppend("OAC", delimiter)
            }
            if (winCount==2) {
                result.delimiterAppend("UWAC", delimiter)
            }
            return result.toString()
        }

    override var fabHeightCode: String
        get() {
            var result = super.fabHeightCode
            if (result.isEmpty()) {
                result = when (kcHeightCode) {
                    "KC650" -> "FAB600"
                    "KC500" -> "FAB500"
                    "KC450" -> "FAB400"
                    "KC350" -> "FAB300"
                    else -> ""
                }
            }
            if (result.isEmpty()) {
                result = when (ukaHeightCode) {
                    "UKA650" -> "FAB600"
                    "UKA550" -> "FAB500"
                    "UKA400" -> "FAB400"
                    "UKA300" -> "FAB300"
                    else -> "FAB600"
                }
            }
            return result
        }
        set(value) {
            super.fabHeightCode = value
        }

    override var ifcsHeightCode: String
        get() {
            var result = super.ifcsHeightCode
            if (result.isEmpty()) {
                result = when (fabHeightCode) {
                    "FAB600" -> "IF600"
                    "FAB500" -> "IF500"
                    "FAB400" -> "IF400"
                    "FAB300" -> "IF300"
                    "FAB200" -> "IF300"
                    else -> "IF300"
                }
            }
            return result
        }
        set(value) {
            super.ifcsHeightCode = value
        }

    override var fabGradeAgility: String
        get() {
            var result = super.fabGradeAgility
            if (result.isEmpty()) {
                result = when (kcGradeCode) {
                    "KC01", "KC02", "KC03" -> "FAB01"
                    "KC04", "KC05" -> "FAB02"
                    "KC06", "KC07" -> "FAB03"
                    else -> "FAB01"
                }
            }
            return result
        }
        set(value) {
            super.fabGradeAgility = value
        }

    override var fabGradeJumping: String
        get() {
            var result = super.fabGradeJumping
            if (result.isEmpty()) {
                result = when (kcGradeCode) {
                    "KC01", "KC02", "KC03" -> "FAB01"
                    "KC04", "KC05" -> "FAB02"
                    "KC06", "KC07" -> "FAB03"
                    else -> "FAB01"
                }
            }
            return result
        }
        set(value) {
            super.fabGradeJumping = value
        }

    override var fabGradeSteeplechase: String
        get() {
            var result = super.fabGradeSteeplechase
            if (result.isEmpty()) {
                result = when (kcGradeCode) {
                    "KC01", "KC02", "KC03" -> "FAB01"
                    "KC04", "KC05" -> "FAB02"
                    "KC06", "KC07" -> "FAB03"
                    else -> "FAB01"
                }
            }
            return result
        }
        set(value) {
            super.fabGradeSteeplechase = value
        }

    val ukaMeasurementText: String
        get() {
            var result = ""
            if (ukaMeasure1 > 0) result = result.append("$ukaMeasure1")
            if (ukaMeasure2 > 0) result = result.append("$ukaMeasure2")
            if (ukaMeasure3 > 0) result = result.append("$ukaMeasure3")
            return if (result.isEmpty()) "Not measured" else if (ukaMeasureProvisional) "$result (Provisional)" else if (ukaMeasureDisputed) "$result (Disputed)" else result
        }


    fun addUkaMeasurement(revisedDateOfBirth: Date, measurement: Int, idMeasurer: Int, idCompetitionMeasure: Int, disputed: Boolean) {
        dbTransaction {
            dateOfBirth = revisedDateOfBirth
            if (ukaMeasure1 == 0) {
                ukaMeasure1 = measurement
            } else if (ukaMeasure2 == 0) {
                ukaMeasure2 = measurement
            } else {
                ukaMeasure3 = measurement
            }
            ukaMeasureProvisional = dateOfBirth.isNotEmpty() && dateOfBirth.after(today.addYears(-2))
            ukaMeasureDisputed = !ukaMeasureProvisional && disputed
            Measurement().withAppend {
                idCompetition = idCompetitionMeasure
                idDog = this@Dog.id
                idCompetitorMeasurer = idMeasurer
                idOrganization = ORGANIZATION_UKA
                value = measurement
                if (disputed) flags = 1
                post()
            }
            post()
        }
    }

    fun generateCode(suggested: Int = 0, doPost: Boolean = true): Int {
        var test = if (suggested > 0) suggested else random(99999, 40000)
        do {
            val query = DbQuery("SELECT dogCode FROM dog WHERE dogCode = $test")
            if (!query.found()) {
                code = test

                if (doPost) {
                    post()
                }
                test = 0
            } else {
                test = random(99999, 40000)
            }
        } while (test > 0)
        return test
    }


    val ukaState: Int
        get() = when {
            ukaBarred -> UKA_SUSPENDED
            ukaDateConfirmed.isNotEmpty() -> UKA_COMPLETE
            else -> UKA_INCOMPLETE
        }

    val isUkaRegisteredOldRule: Boolean
        get() = ukaDateConfirmed.isNotEmpty() || idUka > 0

    val isUkaRegistered: Boolean
        get() = ukaDateConfirmed.isNotEmpty()

    fun isUkaRegistered(newRule: Boolean = false): Boolean {
        return if (newRule) isUkaRegistered else isUkaRegisteredOldRule
    }

    val hasKcChampWins: Boolean
        get() {
            var wins = 0
            var winsPre2019 = 0
            if (kcGradeCode == "KC07" && kcChampWins.size >= 4) {
                for (win in kcChampWins) {
                    val date = win["date"].asDate
                    val show = win["show"].asString
                    val agilityClass = win["class"].asString
                    if (date.isNotEmpty() && show.isNotEmpty() && agilityClass.isNotEmpty()) {
                        wins++
                        if (date.before("2019-01-01".toDate())) winsPre2019++
                    }
                }
            }
            return wins > 4 || winsPre2019 > 3
        }

    override fun whenBeforePost() {
        if (super.code == 0 && idAccount > 0 && state < DOG_GONE) {
            generateCode(idUka)
        }
        if (super.idCompetitorHandler == 0 && idCompetitor > 0) {
            super.idCompetitorHandler = idCompetitor
        }
    }

    override var ukaEntryLevel: String
        get() = if (super.ukaEntryLevel.isNotEmpty()) super.ukaEntryLevel else "UKA01"
        set(value) {
            super.ukaEntryLevel = value
        }

    override var code: Int
        get() {
            if (super.code == 0 && idAccount > 0 && state < DOG_GONE) {
                generateCode(idUka)
            }
            return if (super.code > 0) super.code else idUka
        }
        set(value) {
            super.code = value
        }

    val cleanedPetName: String
        get() = stripTitles(petName).naturalCase
    
    val cleanedPetNameAndUkaTitle: String
        get() = (cleanedPetName + " " + ukaTitle).trim()

    val kcNameText: String
        get() = if (_petName.isEmpty() || _petName.eq(registeredName)) registeredName else "$cleanedPetName ($registeredName)"

    val genderText: String
        get() = if (gender == 1) "Dog" else if (gender == 2) "Bitch" else "n/a"

    val handlerName: String
        get() = if (super.idCompetitorHandler > 0) handler.fullName else owner.fullName

    val isHandlerUkaRegistered: Boolean
        get() = if (super.idCompetitorHandler > 0) handler.isUkaRegistered else owner.isUkaRegistered


    val isDeleted: Boolean
        get() = !isNull("dateDeleted") && dateDeleted.time > 0

    var kcRegistered: Boolean
        get() = idKC.isNotEmpty() && Dog.KcRegistrationValid(idKC, code)
        set(value) {
            if (value && idKC.isEmpty()) {
                idKC = "unknown"
            }
        }

    val _idCompetitorHandler: Int
        get() = super.idCompetitorHandler

    override var idCompetitorHandler: Int
        get() = if (super.idCompetitorHandler > 0) super.idCompetitorHandler else idCompetitor
        set(value) {
            super.idCompetitorHandler = value
        }

    var _registeredName: String
        get() = super.registeredName
        set(value) {
            super.registeredName = value
        }

    var archived: Boolean
        get() = state == DOG_ARCHIVED
        set(value) {
            if (value && state != DOG_ARCHIVED) {
                archivedState = state
                archivedCode = code
                state = DOG_ARCHIVED
                setVariant("dogCode", Variant.nullValue())
            } else if (!value && state == DOG_ARCHIVED) {
                state = archivedState
                generateCode(archivedCode)
            }
        }


    override var registeredName: String
        get() {
            var pre = if (kcChampion) "CH" else ""
            if (kcAgilityChampion) pre = pre.append("AG CH", " & ")
            var post = when (kcWarrantLevel) {
                1 -> "AW(B)"
                2 -> "AW(S)"
                3 -> "AW(G)"
                4 -> "AW(P)"
                5 -> "AW(D)"
                else -> ""
            }
            if (kcShowCertificateMerit) post = post.append("ShCM")
            if (kcJuniorWarrant) post = post.append("JW")
            if (kcObedienceWarrant) post = post.append("OW")
            post = post.append(
                when (kcRallyLevel) {
                    1 -> "RL1"
                    2 -> "RL2"
                    3 -> "RL3"
                    4 -> "RL4"
                    5 -> "RL5"
                    6 -> "RL6"
                    else -> ""
                }
            )
            if (kcRallyLevel > 0 && kcRallyLevelExcellent) {
                post += "EX"
            }

            var result = pre
            result = result.spaceAppend(_registeredName.naturalCase)
            result = result.spaceAppend(post)
            return result
        }
        set(value) {
            KcNameFilter.process(this, value)
        }

    override var petName: String
        get() {
            if (super.petName.isNotEmpty()) {
                return stripTitles(super.petName).naturalCase
            } else {
                return _registeredName.naturalCase
            }
        }
        set(value) {
            super.petName = value
        }

    val _petName: String
        get() = super.petName

    val hasUkaLevel: Boolean
        get() = super.ukaPerformanceLevel.isNotEmpty() || super.ukaSteeplechaseLevel.isNotEmpty()

    val hasUkaHeight: Boolean
        get() = ukaMeasuredHeight != 0 || ukaDeclaredHeightCode.isNotEmpty()

    override var ukaPerformanceLevel: String
        get() = if (super.ukaPerformanceLevel.isEmpty()) "UKA01" else super.ukaPerformanceLevel
        set(value) {
            super.ukaPerformanceLevel = value
        }

    override var ukaSteeplechaseLevel: String
        get() = if (super.ukaSteeplechaseLevel.isEmpty()) "UKA01" else super.ukaSteeplechaseLevel
        set(value) {
            super.ukaSteeplechaseLevel = value
        }

    fun registerUkaAtShow(idCompetition: Int) {
        ukaDateConfirmed = today
        ukaRegistrationType = UKA_REGISTRATION_SHOW
        ukaRegistrationCompetition = idCompetition
        idUka = code
        post()
    }

    var kcGradeCode: String
        get() = kcEffectiveGradeCode(today.addDays(365))
        set(value) {
            kcSetGradeCode(value, kcGradeUnknownDate)
        }

    val kcGradeCodeHold: String
        get() = kcEffectiveGradeCode(today.addDays(365), hold = true)


    fun kcEffectiveGradeCode(date: Date, hold: Boolean = false): String {
        val gradeArray = if (hold) kcGradeHoldArray else kcGradeArray
        val cutOffDate = date.dateOnly().addDays(-25)
        return when {
            gradeArray[6].asDate.isNotEmpty() && gradeArray[6].asDate < cutOffDate -> "KC07"
            gradeArray[5].asDate.isNotEmpty() && gradeArray[5].asDate < cutOffDate -> "KC06"
            gradeArray[4].asDate.isNotEmpty() && gradeArray[4].asDate < cutOffDate -> "KC05"
            gradeArray[3].asDate.isNotEmpty() && gradeArray[3].asDate < cutOffDate -> "KC04"
            gradeArray[2].asDate.isNotEmpty() && gradeArray[2].asDate < cutOffDate -> "KC03"
            gradeArray[1].asDate.isNotEmpty() && gradeArray[1].asDate < cutOffDate -> "KC02"
            else -> "KC01"
        }
    }

    fun kcQualificationDate(hold: Boolean = false): Date {
        val gradeArray = if (hold) kcGradeHoldArray else kcGradeArray
        val gradeCode = if (hold) kcGradeCodeHold else kcGradeCode
        return when (gradeCode) {
            "KC02" -> gradeArray[1].asDate
            "KC03" -> gradeArray[2].asDate
            "KC04" -> gradeArray[3].asDate
            "KC05" -> gradeArray[4].asDate
            "KC06" -> gradeArray[5].asDate
            "KC07" -> gradeArray[6].asDate
            else -> kcGradeUnknownDate
        }
    }

    fun kcSetGradeCode(gradeCode: String, qualificationDate: Date) {
        when (gradeCode) {
            "KC01" -> kcGrade1 = qualificationDate
            "KC02" -> kcGrade2 = qualificationDate
            "KC03" -> kcGrade3 = qualificationDate
            "KC04" -> kcGrade4 = qualificationDate
            "KC05" -> kcGrade5 = qualificationDate
            "KC06" -> kcGrade6 = qualificationDate
            "KC07" -> kcGrade7 = qualificationDate
        }
        if (gradeCode > "KC01" && kcGrade1.isEmpty()) kcGrade1 = kcGradeUnknownDate
        if (gradeCode > "KC02" && kcGrade2.isEmpty()) kcGrade2 = kcGradeUnknownDate
        if (gradeCode > "KC03" && kcGrade3.isEmpty()) kcGrade3 = kcGradeUnknownDate
        if (gradeCode > "KC04" && kcGrade4.isEmpty()) kcGrade4 = kcGradeUnknownDate
        if (gradeCode > "KC05" && kcGrade5.isEmpty()) kcGrade5 = kcGradeUnknownDate
        if (gradeCode > "KC06" && kcGrade6.isEmpty()) kcGrade6 = kcGradeUnknownDate

        if (gradeCode < "KC02" && kcGrade2.isNotEmpty()) kcGrade2 = nullDate
        if (gradeCode < "KC03" && kcGrade3.isNotEmpty()) kcGrade3 = nullDate
        if (gradeCode < "KC04" && kcGrade4.isNotEmpty()) kcGrade4 = nullDate
        if (gradeCode < "KC05" && kcGrade5.isNotEmpty()) kcGrade5 = nullDate
        if (gradeCode < "KC06" && kcGrade6.isNotEmpty()) kcGrade6 = nullDate
        if (gradeCode < "KC07" && kcGrade7.isNotEmpty()) kcGrade7 = nullDate

    }

    val idTeamList: String
        get() {
            return getIdTeamList(id)
        }

    val ukaMeasuredHeight: Int
        get() =
            if (ukaMeasure3 > 0) ukaMeasure3
            else if (ukaMeasure2 > 0) ukaMeasure2
            else ukaMeasure1

    val ukaMeasuredHeightText: String
        get() = if (ukaMeasuredHeight > 0)
            if (ukaMeasureProvisional && ukaMeasuredHeight <= 501)
                if (dateOfBirth.addYears(2) <= today)
                    "${ukaMeasuredHeight}mm (Re-measure at next show)"
                else
                    "${ukaMeasuredHeight}mm (Re-measure when 2)"
            else
                "${ukaMeasuredHeight}mm"
        else if (ukaHeightCode == "UKA650")
            "Measurement not needed"
        else
            "Must measure at next show"


    var ukaHeightCode: String
        get() {
            if (ukaMeasuredHeight == 0) {
                return if (ukaDeclaredHeightCode.isNotEmpty()) ukaDeclaredHeightCode else "UKA650"
            } else if (ukaMeasuredHeight <= 350) {
                return "UKA300"
            } else if (ukaMeasuredHeight <= 430) {
                return "UKA400"
            } else if (ukaMeasuredHeight <= 500) {
                return "UKA550"
            } else {
                return "UKA650"
            }
        }
        set(value) {
            if (ukaMeasuredHeight == 0) {
                ukaDeclaredHeightCode = if (value != "UKA650") value else ""
            }
        }

    override var ukOpenHeightCode: String
        get() {
            if (super.ukOpenHeightCode.isNotEmpty()) {
                return super.ukOpenHeightCode
            } else if (isUkaRegisteredOldRule) {
                when (ukaHeightCode) {
                    "UKA300" -> return "OP300"
                    "UKA400" -> return "OP400"
                    "UKA550" -> return "OP500"
                    "UKA650" -> return "OP600"
                    else -> return ""
                }
            } else {
                return ""
            }
        }
        set(value) {
            super.ukOpenHeightCode = value
        }

    override var ukaHeightCodePerformance: String
        get() {
            if (super.ukaHeightCodePerformance.isNotEmpty() && super.ukaHeightCodePerformance >= ukaHeightCode) {
                return super.ukaHeightCodePerformance
            } else {
                return ukaHeightCode
            }
        }
        set(value) {
            super.ukaHeightCodePerformance = value
        }

    override var ukaHeightCodeSteeplechase: String
        get() {
            if (super.ukaHeightCodeSteeplechase.isNotEmpty() && super.ukaHeightCodePerformance >= ukaHeightCode) {
                return super.ukaHeightCodeSteeplechase
            } else {
                return ukaHeightCode
            }
        }
        set(value) {
            super.ukaHeightCodeSteeplechase = value
        }

    override var ukaHeightCodeNursery: String
        get() {
            if (super.ukaHeightCodeNursery.isNotEmpty()) {
                return super.ukaHeightCodeNursery
            } else {
                return Height.getCasualHeightCode(ukaHeightCode)
            }
        }
        set(value) {
            super.ukaHeightCodeNursery = value
        }

    override var ukaHeightCodeCasual: String
        get() {
            if (super.ukaHeightCodeCasual.isNotEmpty() && super.ukaHeightCodeCasual < ukaHeightCode) {
                return super.ukaHeightCodeCasual
            } else {
                return Height.getCasualHeightCode(ukaHeightCode)
            }
        }
        set(value) {
            super.ukaHeightCodeCasual = value
        }

    fun ukaCalculateGrades(check: Boolean = false) {
        dbQuery(
            """
            SELECT
                sum(if(agilityClass.gradeCodes="UKA01" and classCode=1, progressionPoints, 0)) as beginnersAgility,
                sum(if(agilityClass.gradeCodes="UKA01" and classCode=2, progressionPoints, 0)) as beginnersJumping,
                sum(if(agilityClass.gradeCodes="UKA01" and classCode>3, progressionPoints, 0)) as beginnersGames,
                sum(if(agilityClass.gradeCodes="UKA01" and classCode=3, progressionPoints, 0)) as beginnersSteeplechase,
                sum(if(agilityClass.gradeCodes="UKA02" and classCode=1, progressionPoints, 0)) as noviceAgility,
                sum(if(agilityClass.gradeCodes="UKA02" and classCode=2, progressionPoints, 0)) as noviceJumping,
                sum(if(agilityClass.gradeCodes="UKA02" and classCode>3, progressionPoints, 0)) as noviceGames,
                sum(if(agilityClass.gradeCodes="UKA02" and classCode=3, progressionPoints, 0)) as noviceSteeplechase,
                sum(if(agilityClass.gradeCodes="UKA03" and classCode=1, progressionPoints, 0)) as seniorAgility,
                sum(if(agilityClass.gradeCodes="UKA03" and classCode=2, progressionPoints, 0)) as seniorJumping,
                sum(if(agilityClass.gradeCodes="UKA03" and classCode>3, progressionPoints, 0)) as seniorGames,
                sum(if(agilityClass.gradeCodes="UKA03" and classCode=3, progressionPoints, 0)) as seniorSteeplechase,
                sum(if(agilityClass.gradeCodes="UKA04" and classCode=1, progressionPoints, 0)) as champAgility,
                sum(if(agilityClass.gradeCodes="UKA04" and classCode=2, progressionPoints, 0)) as champJumping,
                sum(if(agilityClass.gradeCodes="UKA04" and classCode>3, progressionPoints, 0)) as champGames,
                sum(if(agilityClass.gradeCodes="UKA04" and classCode=3, progressionPoints, 0)) as champSteeplechase,
                sum(if(place=1 and agilityClass.gradeCodes="UKA04" and classCode=1, progressionPoints, 0)) as champWinAgility,
                sum(if(place=1 and agilityClass.gradeCodes="UKA04" and classCode=2, progressionPoints, 0)) as champWinJumping,
                sum(if(place=1 and agilityClass.gradeCodes="UKA04" and classCode>3, progressionPoints, 0)) as champWinGames,
                sum(if(place=1 and agilityClass.gradeCodes="UKA04" and classCode=3, progressionPoints, 0)) as champWinSteeplechase
            FROM entry join agilityClass using (idAgilityClass) join team using (idTeam) WHERE team.idDog = $id and progressionPoints>0 and entry.progress=50
        """
        ) {
            ukaBeginnersAgility = getInt("beginnersAgility")
            ukaBeginnersJumping = getInt("beginnersJumping")
            ukaBeginnersGames = getInt("beginnersGames")
            ukaBeginnersSteeplechase = getInt("beginnersSteeplechase")

            ukaNoviceAgility = getInt("noviceAgility")
            ukaNoviceJumping = getInt("noviceJumping")
            ukaNoviceGames = getInt("noviceGames")
            ukaNoviceSteeplechase = getInt("noviceSteeplechase")

            ukaSeniorAgility = getInt("seniorAgility")
            ukaSeniorJumping = getInt("seniorJumping")
            ukaSeniorGames = getInt("seniorGames")
            ukaSeniorSteeplechase = getInt("seniorSteeplechase")

            ukaChampAgility = getInt("champAgility")
            ukaChampJumping = getInt("champJumping")
            ukaChampGames = getInt("champGames")
            ukaChampSteeplechase = getInt("champSteeplechase")

            ukaChampWinAgility = getInt("champWinAgility")
            ukaChampWinJumping = getInt("champWinJumping")
            ukaChampWinGames = getInt("champWinGames")
            ukaChampWinSteeplechase = getInt("champWinSteeplechase")

            var newPerformance = ukaPerformanceLevel
            var newSteeplechase = ukaSteeplechaseLevel


            if (ukaChampAgility > 0 || ukaChampJumping > 0 || ukaChampGames > 0 ||
                (ukaSeniorAgility >= 12 && ukaSeniorJumping >= 12 && ukaSeniorGames >= 12 && ukaSeniorAgility + ukaSeniorJumping + ukaSeniorGames >= 48) ||
                (ukaSeniorAgility >= 30 && ukaSeniorAgility + ukaSeniorJumping + ukaSeniorGames >= 60)
            ) {
                newPerformance = "UKA04"
            } else if (ukaSeniorAgility > 0 || ukaSeniorJumping > 0 || ukaSeniorGames > 0 ||
                (ukaNoviceAgility >= 12 && ukaNoviceJumping >= 6 && ukaNoviceGames >= 6 && ukaNoviceAgility + ukaNoviceJumping + ukaNoviceGames >= 36) ||
                (ukaNoviceAgility >= 24 && ukaNoviceAgility + ukaNoviceJumping + ukaNoviceGames >= 48)
            ) {
                newPerformance = "UKA03"
            } else if (ukaNoviceAgility > 0 || ukaNoviceJumping > 0 || ukaNoviceGames > 0 ||
                (ukaBeginnersAgility >= 12 && ukaBeginnersAgility + ukaBeginnersJumping + ukaBeginnersGames >= 24)
            ) {
                newPerformance = "UKA02"
            } else {
                newPerformance = if (ukaEntryLevel.isNotEmpty()) ukaEntryLevel else "UKA01"
            }
            if (ukaChampSteeplechase > 0 || ukaSeniorSteeplechase >= 48) {
                newSteeplechase = "UKA04"
            } else if (ukaSeniorSteeplechase > 0 || ukaNoviceSteeplechase >= 36) {
                newSteeplechase = "UKA03"
            } else if (ukaNoviceSteeplechase > 0 || ukaBeginnersSteeplechase >= 24) {
                newSteeplechase = "UKA02"
            } else {
                newSteeplechase = if (ukaEntryLevel.isNotEmpty()) ukaEntryLevel else "UKA01"
            }

            if (ukaChampEquiv) {
                newPerformance = "UKA04"
                newSteeplechase = "UKA04"
            }

            if (newPerformance != ukaPerformanceLevel && !check) {
                processUkaLevel(PROGRAMME_PERFORMANCE, newPerformance)
            }
            if (newSteeplechase != ukaSteeplechaseLevel && !check) {
                processUkaLevel(PROGRAMME_STEEPLECHASE, newSteeplechase)
            }
        }
        post()
    }

    fun processUkaLevel(programme: Int, gradeCode: String) {
        val json = Json()

        dbTransaction {

            json["programme"] = programme
            json["oldGradeCode"] = if (programme == PROGRAMME_PERFORMANCE) ukaPerformanceLevel else ukaSteeplechaseLevel
            json["newGradeCode"] = gradeCode


            CompetitionDog().join { competition }.where(
                "idDog=$id AND competition.dateEnd>=curdate() AND competition.idOrganization = $ORGANIZATION_UKA",
                "competition.dateStart"
            ) {
                if (programme == PROGRAMME_PERFORMANCE) {
                    this.ukaPerformanceLevel = gradeCode
                } else {
                    this.ukaSteeplechaseLevel = gradeCode
                }
                this.post()

                val competitionNode = json["competitions"].addElement()
                competitionNode["idCompetition"] = idCompetition
                competitionNode["idAccount"] = idAccount
                competitionNode["name"] = competition.briefName
                competitionNode["modified"] = false
                if (competition.dateStart < today) {
                    competitionNode["state"] = "inProgress"
                } else if (competition.processed) {
                    competitionNode["state"] = "processed"
                } else {
                    competitionNode["state"] = "updatable"
                    val where = "agilityClass.idCompetition=$idCompetition AND team.idDog=$id " +
                            "AND entry.progress <= $PROGRESS_TRANSFERRED " +
                            "AND agilityClass.classCode IN (${ClassTemplate.getProgrammeList(programme)}) " +
                            "AND entry.gradeCode <> ${gradeCode.quoted}"
                    val orderBy = "agilityClass.classDate, agilityClass.classCode, agilityClass.suffix"

                    Entry().join { agilityClass }.join { team }.where(where, orderBy) {
                        competitionNode["modified"] = true
                        val action = StringBuilder()
                        moveToUkaGrade(gradeCode, action)
                        val node = competitionNode["actions"].addElement()
                        node["idAgilityClass"] = agilityClass.id
                        node["classDate"] = agilityClass.date
                        node["className"] = agilityClass.name
                        node["action"] = action.toString()
                    }

                }
            }

            PlazaMessage.ukaLevelChange(idAccount, programme, json["oldGradeCode"].asString, gradeCode, this, json)

            if (programme == PROGRAMME_PERFORMANCE) {
                ukaPerformanceLevel = gradeCode
            } else {
                ukaSteeplechaseLevel = gradeCode
            }
            post()

            for (competitionNode in json["competitions"]) {
                if (competitionNode["modified"].asBoolean) {
                    val idCompetition = competitionNode["idCompetition"].asInt
                    val idAccount = competitionNode["idAccount"].asInt
                    var isPaper = false
                    Ledger().seek("idAccount=$idAccount AND idCompetition=$idCompetition AND type=$LEDGER_ENTRY_FEES_PAPER") {
                        isPaper = true
                    }
                    PlazaMessage.showEntryAcknowledged(idCompetition, idAccount, isPaper)
                }
            }
        }
    }

    fun moveToGradeUkaAtShow(gradeCode: String, programme: Int, idCompetition: Int): Boolean {
        clearMessages()
        var baseDate = today
        var sql = """
            SELECT
                true
            FROM
                Entry
                JOIN agilityClass USING (idAgilityClass)
                JOIN team USING (idTeam)
            WHERE
                team.idDog=$id AND
                Date(runStart)=${baseDate.sqlDate} AND
                classCode IN (${ClassTemplate.getProgrammeList(programme)})
            LIMIT 1
        """
        var query = DbQuery(sql)
        if (query.found()) {
            addMessage("Already run today so upgrade starts tomorrow")
            baseDate = getDay(1)
        }
        sql = """
            SELECT
                idEntry
            FROM
                Entry
                JOIN agilityClass USING (idAgilityClass)
                JOIN team USING (idTeam)
            WHERE
                agilityClass.idCompetition=$idCompetition AND
                team.idDog=$id AND
                Date(classDate)>=${baseDate.sqlDate} AND
                entry.entryType < $ENTRY_LOW_LATE AND
                entry.progress <= $PROGRESS_TRANSFERRED AND
                classCode IN (${ClassTemplate.getProgrammeList(programme)})
            ORDER BY
                agilityClass.classDate, agilityClass.classCode, agilityClass.suffix
        """
        query = DbQuery(sql, id, baseDate.sqlDate, programme)
        val entry = Entry()
        while (query.next()) {
            entry.find(query.getInt("idEntry"))
            entry.moveToUkaGrade(gradeCode)
        }
        return true
    }


    fun moveToGradeFabAtShow(idCompetition: Int, programme: Int, gradeCode: String): Boolean {
        clearMessages()
        val sql = """
            SELECT
                idEntry
            FROM
                Entry
                JOIN agilityClass USING (idAgilityClass)
                JOIN team USING (idTeam)
            WHERE
                agilityClass.idCompetition=$idCompetition AND
                team.idDog=$id AND
                Date(classDate)>=${today.sqlDate} AND
                entry.entryType < $ENTRY_LOW_LATE AND
                entry.progress <= $PROGRESS_RUNNING AND
                entryRule = $programme
            ORDER BY
                agilityClass.classDate, agilityClass.classCode, agilityClass.suffix
        """
        dbQuery(sql) {
            Entry().seek(getInt("idEntry")) {
                moveToFabGrade(gradeCode)
            }
        }
        return true
    }

    fun moveToHeightFabAtShow(idCompetition: Int, organization: Int, heightCode: String): Boolean {
        clearMessages()
        val classCodeList = if (organization == ORGANIZATION_FAB)
            ClassTemplate.fabList
        else
            ClassTemplate.ifcsList
        val sql = """
            SELECT
                idEntry
            FROM
                Entry
                JOIN agilityClass USING (idAgilityClass)
                JOIN team USING (idTeam)
            WHERE
                agilityClass.idCompetition=$idCompetition AND
                team.idDog=$id AND
                Date(classDate)>=${today.sqlDate} AND
                entry.entryType < $ENTRY_LOW_LATE AND
                entry.progress <= $PROGRESS_RUNNING AND
                agilityClass.classCode IN ($classCodeList)
            ORDER BY
                agilityClass.classDate, agilityClass.classCode, agilityClass.suffix
        """
        dbQuery(sql) {
            Entry().join { agilityClass }.seek(getInt("idEntry")) {
                if (this.heightCode != heightCode) {
                    this.heightCode = heightCode
                    this.jumpHeightCode = heightCode
                    runningOrder = agilityClass.nextRunningOrder(heightCode)
                    subClass =
                        agilityClass.chooseSubClass(this.gradeCode, this.heightCode, this.jumpHeightCode, this.subDivision)
                    post()
                }
            }
        }
        return true
    }

    fun changeFabSubdivisionAtShow(idCompetition: Int, subDivision: Int): Boolean {
        clearMessages()
        val sql = """
            SELECT
                idEntry
            FROM
                Entry
                JOIN agilityClass USING (idAgilityClass)
                JOIN team USING (idTeam)
            WHERE
                agilityClass.idCompetition=$idCompetition AND
                team.idDog=$id AND
                Date(classDate)>=${today.sqlDate} AND
                entry.entryType < $ENTRY_LOW_LATE AND
                entry.progress <= $PROGRESS_RUNNING AND
                agilityClass.classCode IN (${ClassTemplate.fabList})
            ORDER BY
                agilityClass.classDate, agilityClass.classCode, agilityClass.suffix
        """
        dbQuery(sql) {
            Entry().join { agilityClass }.seek(getInt("idEntry")) {
                if (this.subDivision != subDivision) {
                    this.subDivision = subDivision
                    subClass =
                        agilityClass.chooseSubClass(this.gradeCode, this.heightCode, this.jumpHeightCode, this.subDivision)
                    post()
                }
            }
        }
        return true
    }

    fun moveToHeightUkOpenAtShow(idCompetition: Int, proposedHeightCode: String) {
        Entry().join { agilityClass }.join { team }
            .where("agilityClass.idCompetition = $idCompetition AND team.idDog = $id") {
                heightCode = proposedHeightCode
                jumpHeightCode = proposedHeightCode
                post()
            }
    }

    fun moveToGroupUkOpenAtShow(idCompetition: Int, proposedGroup: String) {
        Entry().join { agilityClass }.join { team }
            .where("agilityClass.idCompetition = $idCompetition AND team.idDog = $id") {
                group = proposedGroup
                post()
            }
    }

    fun isClearRoundOnly(jumpHeightCode: String): Boolean {
        return when (ukaHeightCode) {
            "UKA650" -> jumpHeightCode < "UKA550"
            "UKA550" -> jumpHeightCode < "UKA400"
            "UKA400" -> jumpHeightCode < "UKA300"
            else -> false
        }
    }

    val ukaPerformanceStatement: String
        get() {
            var statement: String
            var nextGrade = ""
            var totalNeeded = 0
            var agilityNeeded = 0
            var jumpingNeeded = 0
            var gamesNeeded = 0

            if (ukaPerformanceLevel == "UKA01") {
                nextGrade = "Novice"
                totalNeeded = 24 - ukaBeginnersAgility - ukaBeginnersJumping - ukaBeginnersGames
                agilityNeeded = 12 - ukaBeginnersAgility
            } else if (ukaPerformanceLevel == "UKA02") {
                nextGrade = "Senior"
                totalNeeded = 36 - ukaNoviceAgility - ukaNoviceJumping - ukaNoviceGames
                agilityNeeded = 12 - ukaNoviceAgility
                jumpingNeeded = 6 - ukaNoviceJumping
                gamesNeeded = 6 - ukaNoviceGames
            } else if (ukaPerformanceLevel == "UKA03") {
                nextGrade = "Champ"
                totalNeeded = 48 - ukaSeniorAgility - ukaSeniorJumping - ukaSeniorGames
                agilityNeeded = 12 - ukaSeniorAgility
                jumpingNeeded = 12 - ukaSeniorJumping
                gamesNeeded = 12 - ukaSeniorGames
            }
            if (ukaPerformanceLevel == "UKA04") {
                statement = "You are in Champ"
            } else {
                var specific = ""
                val items = ArrayList<String>()
                if (agilityNeeded > 0) {
                    items.add("$agilityNeeded points in Agility")
                }
                if (jumpingNeeded > 0) {
                    items.add("$jumpingNeeded points in Jumping")
                }
                if (gamesNeeded > 0) {
                    items.add("$gamesNeeded points in Games")
                }
                when (items.size) {
                    1 -> specific = items[0]
                    2 -> specific = items[0] + " and " + items[1]
                    3 -> specific = items[0] + ", " + items[1] + " and " + items[2]
                }
                statement = "To progress to $nextGrade, you need "
                if (totalNeeded > 0 && specific.isEmpty()) {
                    statement += "$totalNeeded points in any discipline"
                } else if (totalNeeded > 0 && specific.isNotEmpty()) {
                    statement += "$totalNeeded points overall with at least " + specific
                } else {
                    statement += specific
                }
            }
            return statement + "."
        }

    val ukaSteeplechaseStatement: String
        get() {
            if (ukaSteeplechaseLevel == "UKA01") {
                return "To progress to Novice, you need ${24 - ukaBeginnersSteeplechase} points."
            } else if (ukaSteeplechaseLevel == "UKA02") {
                return "To progress to Senior, you need ${36 - ukaNoviceSteeplechase} points."
            } else if (ukaSteeplechaseLevel == "UKA03") {
                return "To progress to Champ, you need ${48 - ukaSeniorSteeplechase} points."
            }
            if (ukaSteeplechaseLevel == "UKA04") {
                return "You are in Champ."
            }
            return ""
        }

    fun resolveAlias(): Int {
        if (aliasFor == 0) {
            return id
        } else {
            return Dog(aliasFor).resolveAlias()
        }
    }

    fun updateKcGrade(): JsonNode {

        val kcGradeCodeNew = kcGradeCodeHold
        val kcGradeCodeOld = kcGradeCode
        val wonOut = kcQualificationDate(hold = true)
        val log = Json()

        log["kcGradeCodeOld"] = kcGradeCodeOld
        log["kcGradeCodeNew"] = kcGradeCodeNew
        log["wonOut"] = wonOut

        val hasWonOut = wonOut > Dog.kcGradeUnknownDate
        dbTransaction {
            CompetitionDog().join { competition }.where(
                "idDog=$id AND competition.dateStart>=curdate() AND competition.idOrganization = $ORGANIZATION_KC",
                "competition.dateStart"
            ) {


                val effectiveGradeCode = kcEffectiveGradeCode(competition.dateStart, hold = true)
                if (((hasWonOut && competition.dateStart.addDays(-14) >= today) || !competition.hasClosed) && kcGradeCode != kcGradeCodeNew) {
                    val competitionNode = log["competitions"].addElement()
                    competitionNode["idCompetition"] = idCompetition
                    competitionNode["idAccount"] = idAccount
                    competitionNode["name"] = competition.briefName
                    processKcGradeChange(effectiveGradeCode, competitionNode["dates"])
                }
            }

            kcGradeArray.setValue(kcGradeHoldArray)
            post()
            PlazaMessage.competitionGradeChange(
                idAccount,
                Account(idAccount).emailList,
                kcGradeCodeOld,
                kcGradeCodeNew,
                cleanedPetName,
                log["competitions"]
            )
            for (competitionNode in log["competitions"]) {
                val idCompetition = competitionNode["idCompetition"].asInt
                val idAccount = competitionNode["idAccount"].asInt
                var isPaper = false
                Ledger().seek("idAccount=$idAccount AND idCompetition=$idCompetition AND type=$LEDGER_ENTRY_FEES_PAPER") {
                    isPaper = true
                }
                PlazaMessage.showEntryAcknowledged(idCompetition, idAccount, isPaper)
            }
        }
        return log
    }

    fun ActiveAgilityOptions(showDate: Date, g7Owner: Boolean): String {
        var options = ""
        if (kcGradeCode <= "KC05") {
            if (dateOfBirth <= showDate.addMonths(-18)) {
                if (kcGradeCode <= "KC02" && ukaPerformanceLevel <= "UKA02" && ukaSteeplechaseLevel < "UKA04" && !g7Owner) {
                    options += "AC"
                } else {
                    options += "DF"
                }
            }
            if (dateOfBirth <= showDate.addMonths(-16)) {
                if (kcGradeCode <= "KC02" && ukaPerformanceLevel <= "UKA02" && ukaSteeplechaseLevel <= "UKA02" && !g7Owner) {
                    options += "B"
                } else {
                    options += "E"
                }
            }
        }

        return options
    }


    companion object {

        val kcGradeUnknownDate = "2000-01-01".toDate()

        fun select(where: String, orderBy: String = "", limit: Int = 0): Dog {
            val dog = Dog()
            dog.select(where, orderBy, limit)
            return dog
        }


        private var _maxIdUkaDog = 0

        private var dog = Dog()

        fun getPetName(idDog: Int): String {
            dog.find(idDog)
            if (dog.found()) {
                return dog.cleanedPetName
            } else {
                return ""
            }
        }

        fun getIdFromCode(dogCode: Int): Int {
            if (dogCode <= 0) {
                return 0
            }
            dog.find("dogCode", dogCode)
            if (dog.found()) {
                return dog.id
            } else {
                return 0
            }
        }

        fun getIdFromUka(idUka: Int): Int {
            if (idUka <= 0) {
                return 0
            }
            dog.find("idUka", idUka)
            if (dog.found()) {
                return dog.id
            } else {
                return 0
            }
        }

        fun getIdCompetitor(idDog: Int): Int {
            dog.find(idDog)
            if (dog.found()) {
                return dog.idCompetitor
            } else {
                return 0
            }
        }

        fun getMaxIdUkaDog(): Int {
            if (_maxIdUkaDog == 0) {
                val query = DbQuery("SELECT MAX(idUka) AS maxIdUka FROM dog WHERE idCompetitor>0")
                if (query.found()) {
                    _maxIdUkaDog = query.getInt("maxIdUka")
                }
            }
            return _maxIdUkaDog
        }

        fun getIdTeamList(idDog: Int): String {
            val list = StringBuilder()
            //val query = DbQuery("SELECT idTeam FROM Team WHERE idDog=$idDog OR idDog2=$idDog OR idDog3=$idDog")
            val query = DbQuery("SELECT idTeam FROM Team WHERE idDog=$idDog")
            while (query.next()) {
                list.csvAppend(query.getInt("idTeam").toString())
            }
            return list.toString()

        }

        fun getIdTeamListEx(idDog: Int, idAgilityClassList: String): String {
            val list = ArrayList<Int>()
            dbQuery("SELECT idTeam FROM Team WHERE idDog=$idDog") {
                val idTeam = getInt("idTeam")
                list.add(idTeam)
            }
            if (idAgilityClassList.isNotEmpty()) {
                dbQuery(
                    """
                    SELECT
                        idTeam
                    FROM
                        Team
                    WHERE
                        idAgilityClass IN ($idAgilityClassList)
                            AND (JSON_EXTRACT(extra, '${'$'}.members[0].idDog') = $idDog
                            OR JSON_EXTRACT(extra, '${'$'}.members[1].idDog') = $idDog
                            OR JSON_EXTRACT(extra, '${'$'}.members[2].idDog') = $idDog)
                """
                ) {
                    val idTeam = getInt("idTeam")
                    if (!list.contains(idTeam)) list.add(idTeam)
                }
            }
            return list.asCommaList()
        }

        fun stripTitles(fullName: String): String {
            var result = SpaceList()
            val words = fullName.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (word in words) {
                if (word.oneOf("CAP", "CSC", "WCAP", "WCSC", "MS", "MA", "MC", "UWAC", "OAC", "OAA")) {
                    break
                }
                result.add(word)
            }
            return result.toString()
        }

        fun extractTitles(fullName: String): String {
            var result = SpaceList()
            var inTitles = false
            val words = fullName.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (word in words) {
                if (word.oneOf("CAP", "CSC", "WCAP", "WCSC", "MS", "MA", "MC", "UWAC", "OAC", "OAA")) {
                    inTitles = true
                }
                if (inTitles) {
                    result.add(word)
                }
            }
            return result.toString()
        }

        fun getNextIdUka(range: Int = 3): Int {
            var proposedIdUka = random(range * 10000 + 9999, range * 10000)
            var ok = false
            while (!ok) {
                proposedIdUka = random(range * 10000 + 9999, range * 10000)
                val query = DbQuery("SELECT idUka FROM dog WHERE idUka=$proposedIdUka")
                ok = !query.found()
            }
            return proposedIdUka
        }


        fun idUkaExists(idUKA: Int): Boolean {
            if (idUKA > 0) {
                dog.select("idUka=$idUKA OR dogCode=$idUKA")
                if (dog.found()) {
                    return true
                }
            }
            return false
        }


        fun registerNew(idAccount: Int, idCompetitor: Int, petName: String, idUKA: Int): Int {
            dog.append()
            dog.idAccount = idAccount
            dog.idCompetitor = idCompetitor
            dog.petName = petName
            dog.idUka = if (idUKA > 0) idUKA else getNextIdUka(9)
            dog.post()
            dog.owner.refresh()
            return dog.id
        }

        var _maxIdUka = 0
        val maxIDUka: Int
            get() {
                if (_maxIdUka == 0) {
                    val query = DbQuery("select max(idUKA) as idUKA from dog where idUKA<90000")
                    query.first()
                    _maxIdUka = query.getInt("idUKA")
                }
                return _maxIdUka
            }

        fun renumber(from: Int, to: Int) {
            val dog = Dog()
            dog.find(to)
            if (!dog.found()) {
                renumberLinked(from, to)
                dbExecute("UPDATE dog SET idDog=$to WHERE idDog=$from")
            }
        }

        fun codeToIdDog(code: Int): Int {
            if (code > 0) {
                val dog = select("dogCode=$code")
                if (dog.first()) {
                    return dog.id
                }
            }
            return -1
        }

        fun checkLinked() {
            dbExecute("UPDATE IGNORE competitionDog JOIN dog USING (idDog) set competitionDog.idDog=dog.aliasFor WHERE dog.aliasFor > 0")
            dbExecute("DELETE competitionDog.* FROM competitionDog JOIN dog USING (idDog) WHERE dog.aliasFor > 0")
            dbExecute("UPDATE ledgerItem JOIN dog USING (idDog) set ledgerItem.idDog=dog.aliasFor WHERE dog.aliasFor > 0")
            dbExecute("UPDATE team JOIN dog USING (idDog) set team.idDog=dog.aliasFor WHERE dog.aliasFor > 0")
            dbExecute("UPDATE webTransaction JOIN dog USING (idDog) set webTransaction.idDog=dog.aliasFor WHERE dog.aliasFor > 0")
            dbExecute("UPDATE measurement JOIN dog USING (idDog) set measurement.idDog=dog.aliasFor WHERE dog.aliasFor > 0")
        }

        fun renumberLinked(from: Int, to: Int) {
            if (from > 0 && to > 0) {
                dbExecute("UPDATE IGNORE competitionDog SET idDog=$to WHERE idDog=$from")
                dbExecute("DELETE FROM competitionDog WHERE idDog=$from")
                dbExecute("UPDATE ledgerItem SET idDog=$to WHERE idDog=$from")
                dbExecute("UPDATE team SET idDog=$to WHERE idDog=$from")
                dbExecute("UPDATE webTransaction SET idDog=$to WHERE idDog=$from")
                dbExecute("UPDATE measurement SET idDog=$to WHERE idDog=$from")
                dbExecute("UPDATE dog SET aliasFor=$to WHERE aliasFor=$from")
                dbExecute("UPDATE dog SET aliasFor=$to WHERE idDog=$from")
            }
        }

        fun merge(toCode: Int, vararg codes: Int) {
            dbTransaction {
                val target = select("dogCode=$toCode")
                if (target.first()) {
                    val dog = Dog()
                    for (code in codes) {
                        dog.select("dogCode=$code")
                        while (dog.next()) {
                            if (dog.idAccount == target.idAccount) {
                                if (dog.kcRegistered && !target.kcRegistered) {
                                    target.idKC = dog.idKC
                                    target.registeredName = dog.registeredName
                                    target.extra["kc"] = dog.extra["kc"]
                                }
                                renumberLinked(dog.id, target.id)
                                if (dog.idUka > 0) {
                                    if (dog.ukaDateConfirmed.isNotEmpty() && (target.ukaDateConfirmed.isEmpty() || dog.ukaDateConfirmed < target.ukaDateConfirmed)) {
                                        target.ukaDateConfirmed = dog.ukaDateConfirmed
                                    }

                                    target.ukaAliases = target.ukaAliases.append(dog.idUka.toString())
                                    dog.post()
                                } else {
                                    dog.delete()
                                }
                            }
                        }
                    }
                    target.post()
                }
            }
        }


        fun KcRegistrationValid(idKc: String, dogCode: Int = 0): Boolean {
            if (dogCode > 0 && idKc == "NAF$dogCode") {
                return true
            }
            return idKc.matches(Regex("(A[A-Z]0[0-8][0-9]{4}[0-1][0-9])|(A[A-Z]0[09][0-9]{5})|(K[0-2][0-9]{5})|(ATCA[A-Z][0-9]{5}([A-Z]{3})?)|([0-9]{4}[CD][A-Z])"))
        }

    }


}

data class KcName(
    val registeredName: String,
    val kcWarrantLevel: Int,
    val kcChampion: Boolean,
    val kcAgilityChampion: Boolean,
    val kcShowCertificateMerit: Boolean,
    val kcJuniorWarrant: Boolean,
    val kcObedienceWarrant: Boolean,
    val kcRallyLevel: Int,
    val kcRallyLevelExcellent: Boolean
)


object KcNameFilter {

    val map = LinkedHashMap<String, String>()

    init {
        addMap("AW(B), AW (B), AWB, (AW)B")
        addMap("AW(S), AW (S), AWS")
        addMap("AW(G), AW (G), AWG")
        addMap("AW(P), AW (P), AWP")
        addMap("AW(D), AW (D), AWD")
        addMap("AG CH, AGCH, AG.CH, AG.CH.")
        addMap("CH")
        addMap("ShCM")
        addMap("JW")
        addMap("OW")
        addMap("RL1EX")
        addMap("RL2EX")
        addMap("RL3EX")
        addMap("RL4EX")
        addMap("RL5EX")
        addMap("RL6EX")
        addMap("RL1")
        addMap("RL2")
        addMap("RL3")
        addMap("RL4")
        addMap("RL5")
        addMap("RL6")
    }

    fun addMap(data: String) {
        val abbreviations = data.split(",")
        for (abbreviation in abbreviations) {
            map[abbreviation.trim()] = abbreviations[0].trim()
        }
    }

    fun parseName(nameWithTitles: String): KcName {
        var registeredName = " ${nameWithTitles.trim()} "
        var kcWarrantLevel = 0
        var kcChampion = false
        var kcAgilityChampion = false
        var kcShowCertificateMerit = false
        var kcJuniorWarrant = false
        var kcObedienceWarrant = false
        var kcRallyLevel = 0
        var kcRallyLevelExcellent = false

        map.forEach({ match, qualification ->
            var add = ""
            if (registeredName.startsWith(" $match ", ignoreCase = true)) {
                registeredName = registeredName.drop(match.length + 2)
                registeredName = registeredName.replace("  ", " ")
                add = qualification
            } else if (registeredName.contains(" $match ", ignoreCase = true)) {
                registeredName = registeredName.replace(" $match", "", ignoreCase = true)
                registeredName = registeredName.replace("  ", " ")
                add = qualification
            } else if (registeredName.contains("($match)", ignoreCase = true)) {
                registeredName = registeredName.replace("($match)", "", ignoreCase = true)
                registeredName = registeredName.replace("  ", " ")
                add = qualification
            }
            if (add.isNotEmpty()) {
                when (add) {
                    "AW(B)" -> kcWarrantLevel = 1
                    "AW(S)" -> kcWarrantLevel = 2
                    "AW(G)" -> kcWarrantLevel = 3
                    "AW(P)" -> kcWarrantLevel = 4
                    "AW(D)" -> kcWarrantLevel = 5
                    "CH" -> kcChampion = true
                    "AG CH" -> kcAgilityChampion = true
                    "ShCM" -> kcShowCertificateMerit = true
                    "JW" -> kcJuniorWarrant = true
                    "OW" -> kcObedienceWarrant = true
                    "RL1" -> {
                        kcRallyLevel = 1; kcRallyLevelExcellent = false
                    }
                    "RL2" -> {
                        kcRallyLevel = 2; kcRallyLevelExcellent = false
                    }
                    "RL3" -> {
                        kcRallyLevel = 3; kcRallyLevelExcellent = false
                    }
                    "RL4" -> {
                        kcRallyLevel = 4; kcRallyLevelExcellent = false
                    }
                    "RL5" -> {
                        kcRallyLevel = 5; kcRallyLevelExcellent = false
                    }
                    "RL6" -> {
                        kcRallyLevel = 6; kcRallyLevelExcellent = false
                    }
                    "RL1EX" -> {
                        kcRallyLevel = 1; kcRallyLevelExcellent = true
                    }
                    "RL2EX" -> {
                        kcRallyLevel = 2; kcRallyLevelExcellent = true
                    }
                    "RL3EX" -> {
                        kcRallyLevel = 3; kcRallyLevelExcellent = true
                    }
                    "RL4EX" -> {
                        kcRallyLevel = 4; kcRallyLevelExcellent = true
                    }
                    "RL5EX" -> {
                        kcRallyLevel = 5; kcRallyLevelExcellent = true
                    }
                    "RL6EX" -> {
                        kcRallyLevel = 6; kcRallyLevelExcellent = true
                    }
                }
            }
        })
        registeredName = registeredName.replace("  ", " ").trim()
        return KcName(
            registeredName,
            kcWarrantLevel,
            kcChampion,
            kcAgilityChampion,
            kcShowCertificateMerit,
            kcJuniorWarrant,
            kcObedienceWarrant,
            kcRallyLevel,
            kcRallyLevelExcellent
        )
    }

    fun process(dog: Dog, nameWithTitles: String) {
        val parse = parseName(nameWithTitles)
        dog._registeredName = parse.registeredName
        dog.kcWarrantLevel = parse.kcWarrantLevel
        dog.kcChampion = parse.kcChampion
        dog.kcAgilityChampion = parse.kcAgilityChampion
        dog.kcShowCertificateMerit = parse.kcShowCertificateMerit
        dog.kcJuniorWarrant = parse.kcJuniorWarrant
        dog.kcObedienceWarrant = parse.kcObedienceWarrant
        dog.kcRallyLevel = parse.kcRallyLevel
        dog.kcRallyLevelExcellent = parse.kcRallyLevelExcellent
    }
}


