import org.egility.library.dbobject.*
import org.egility.library.general.*
import org.egility.linux.tools.EnglandTryouts
import java.lang.Thread.sleep

/*
 * Copyright (c) Mike Brickman 2014-2017
 */

/**
 * Created by mbrickman on 04/09/17.
 */

object TestShow {

    val CROOKED_OAK_SEP_17 = 608
    val GT_MAY_17 = 583
    val LYDIARD_APR_17 = 580
    val TEEJAY_17 = 602
    val A4E = 1516581846
    val WEEK_17 = 600

    var baseDate = "2000-01-01".toDate()
    var TEST_ID = 999999

    var realTimeFactor = 0
    var noWithdraws = false

    fun nextDay() {
        baseDate = baseDate.addDays(1)
    }

    fun setup(idCompetition: Int = TEST_ID, dateString: String = "2000-01-01", updateControl: Boolean = true) {
        baseDate = dateString.toDate()
        TEST_ID = idCompetition
        if (updateControl) {
            val control = Control()
            control.find(1)
            if (control.found()) {
                control.idCompetition = TEST_ID
                control.effectiveDate = baseDate
                control.post()
            }
            Competition.reset()
        }
    }

    fun wipe() {
        dbExecute("DELETE entry.* FROM entry JOIN agilityClass USING (idAgilityClass) WHERE idCompetition=$TEST_ID")
        dbExecute("DELETE FROM agilityClass WHERE idCompetition=$TEST_ID")
        dbExecute("DELETE FROM ring WHERE idCompetition=$TEST_ID")
        dbExecute("DELETE FROM competitionLedger WHERE idCompetition=$TEST_ID")
        dbExecute("DELETE FROM competitionDay WHERE idCompetition=$TEST_ID")
        dbExecute("DELETE FROM competition WHERE idCompetition=$TEST_ID")
    }


    fun initialize(idOrganization: Int = 2, name: String = "UKA Test", pdf: Boolean = Global.alwaysToPdf, updateControl: Boolean = true) {
        Global.alwaysToPdf = pdf
        Global.automatedTest = true

        wipe()

        val competition = Competition()
        competition
        competition.append()
        competition.id = TEST_ID
        competition.idOrganization = idOrganization
        competition.name = name
        competition.uniqueName = name.noSpaces
        competition.dateStart = baseDate
        competition.dateEnd = baseDate
        competition.dateOpens = baseDate
        competition.dateCloses = baseDate
        competition.post()

        if (updateControl) {
            val control = Control()
            control.find(1)
            if (control.found()) {
                control.idCompetition = TEST_ID
                control.effectiveDate = baseDate
                control.post()
            }
        }
    }

    fun createTryOut(idCompetition: Int = A4E, ringNumber: Int = 1): Int {
        val idTryOut = findIdAgilityClass(idCompetition, ClassTemplate.TRY_OUT)
        val idTryPentathlon = findIdAgilityClass(idCompetition, ClassTemplate.TRY_OUT_PENTATHLON)
        val idGames = findIdAgilityClass(idCompetition, ClassTemplate.TRY_OUT_GAMES)


        val idAgility1 = findIdAgilityClass(idCompetition, ClassTemplate.TRY_OUT_PENTATHLON_AGILITY1)
        val idJumping1 = findIdAgilityClass(idCompetition, ClassTemplate.TRY_OUT_PENTATHLON_JUMPING1)
        val idJumping2 = findIdAgilityClass(idCompetition, ClassTemplate.TRY_OUT_PENTATHLON_JUMPING2)
        val idAgility2 = findIdAgilityClass(idCompetition, ClassTemplate.TRY_OUT_PENTATHLON_AGILITY2)
        val idSpeedstakes = findIdAgilityClass(idCompetition, ClassTemplate.TRY_OUT_PENTATHLON_SPEEDSTAKES)

        val idSnooker = findIdAgilityClass(idCompetition, ClassTemplate.TRY_OUT_GAMES_SNOOKER)
        val idGamblers = findIdAgilityClass(idCompetition, ClassTemplate.TRY_OUT_GAMES_GAMBLERS)


        val tryout = addClass(idTryOut, 0, 0, 0)
        val pentathlon = addClass(idTryPentathlon, tryout, 0, 0)
        val games = addClass(idGames, tryout, 0, 0)
        addClass(idAgility1, pentathlon, ringNumber, 1)
        addClass(idSnooker, games, ringNumber, 2)
        addClass(idJumping1, pentathlon, ringNumber, 3)
        addClass(idJumping2, pentathlon, ringNumber, 4)
        addClass(idGamblers, games, ringNumber, 5)
        addClass(idAgility2, pentathlon, ringNumber, 6)
        addClass(idSpeedstakes, pentathlon, ringNumber, 7)
        return 7
    }

    fun createFinals(idCompetition: Int = A4E, ringNumber: Int = 1): Int {
        addClass(findIdAgilityClass(idCompetition, ClassTemplate.FINAL_ROUND_1), 0, ringNumber, 1)
        addClass(findIdAgilityClass(idCompetition, ClassTemplate.FINAL_ROUND_2), 0, ringNumber, 2)
        addClass(findIdAgilityClass(idCompetition, ClassTemplate.FINAL_ROUND_3), 0, ringNumber, 3)
        return 3
    }

    fun createAll(idCompetition: Int, template: ClassTemplate, ringNumber: Int): Int {
        val agilityClass = AgilityClass()
        val child = AgilityClass()
        agilityClass.find("idCompetition=$idCompetition AND classCode=${template.code}")
        child.select("idCompetition=$idCompetition AND idAgilityClassParent=${agilityClass.id}", "ringNumber, ringOrder")

        val parent = addClass(agilityClass.id, 0, 0, 0)
        var ringOrder = 1
        while (child.next()) {
            addClass(child.id, parent, ringNumber, ringOrder++)
        }
        return ringOrder - 1
    }

    fun createTest(idCompetition: Int, classTemplate: ClassTemplate, ringNumber: Int = 1): Int {
        val ringClasses =
            if (classTemplate == ClassTemplate.TRY_OUT)
                createTryOut(idCompetition, ringNumber)
            else if (classTemplate == ClassTemplate.FINAL_ROUND_1)
                createFinals(idCompetition, ringNumber)
            else
                createAll(idCompetition, classTemplate, ringNumber)
        Competition.fixUpClasses()
        return ringClasses
    }

    fun findIdAgilityClass(idCompetition: Int, template: ClassTemplate): Int {
        val agilityClass = AgilityClass()
        agilityClass.find("idCompetition=$idCompetition AND classCode=${template.code}")
        return agilityClass.id
    }


    fun addClass(idAgilityClass: Int, idAgilityClassParent: Int, ringNumber: Int, ringOrder: Int): Int {
        val source = AgilityClass()
        val target = AgilityClass()
        source.find(idAgilityClass)
        target.append()

        target.idCompetition = TEST_ID
        target.idAgilityClassParent = idAgilityClassParent
        target.date = baseDate
        target.ringNumber = ringNumber
        target.ringOrder = ringOrder
        target.progress = CLASS_PENDING
        target.closedForLateEntries = false
        target.readyToRun = false
        target.finalized = false
        target.walkingOverLunch = false
        target.closedForLateEntries = false
        target.closedForLateEntries = false
        target.idUka = source.id

        target.number = source.number
        target.numberSuffix = source.numberSuffix
        target.part = source.part
        target.partType = source.partType
        target.code = source.code
        target.groupColumn = source.groupColumn
        target.prefix = source.prefix
        target.suffix = source.suffix
        target.sponsor = source.sponsor
        target.name = source.name
        target.nameLong = source.nameLong
        target.judge = source.judge
        target.heightOptions = source.heightOptions
        target.gradeCodes = source.gradeCodes
        target.heightCodes = source.heightCodes
        target.jumpHeightCodes = source.jumpHeightCodes
        target.heightRunningOrder = source.heightRunningOrder
        target.courseLength = source.courseLength
        target.entryFee = source.entryFee
        target.lateEntryFee = source.lateEntryFee
        target.entryRule = source.entryRule
        target.combineHeights = source.combineHeights
        target.dataProviderName = source.dataProviderName
        target.heights = source.heights
        target.subClasses = source.subClasses
        target.extra = source.extra
        target.entryRule = source.entryRule
        target.post()
        return target.id

    }

    fun populate() {
        val agilityClass = AgilityClass()
        agilityClass.select("idCompetition=$TEST_ID")
        while (agilityClass.next()) {
            if (agilityClass.template.canEnterDirectly) {
                copyEntries(agilityClass.idUka, agilityClass)

            }
        }
    }

    fun processRing(ringNumber: Int = 1) {
        val ring = Ring()
        ring.select(TEST_ID, baseDate, ringNumber)
        val agilityClass = AgilityClass()
        agilityClass.find(ring.idAgilityClass)
        if (agilityClass.found()) {
            agilityClass.progress = CLASS_RUNNING
            copyResults(agilityClass.idUka, agilityClass.id)
            agilityClass.post()
        }
    }

    fun simulateRing(ringNumber: Int, remain: Int = 0, groups: Int = 0) {
        val ring = Ring()
        ring.select(TEST_ID, baseDate, ringNumber)
        val agilityClass = AgilityClass()
        agilityClass.find(ring.idAgilityClass)
        if (agilityClass.found()) {
            agilityClass.progress = CLASS_RUNNING
            agilityClass.post()
            simulateResults(agilityClass.id, remain, groups)
        }
    }

    fun closeClass(ringNumber: Int = 1) {
        val ring = Ring()
        ring.select(TEST_ID, baseDate, ringNumber)
        val agilityClass = AgilityClass()
        agilityClass.find(ring.idAgilityClass)
        if (agilityClass.found()) {
            agilityClass.closeClass()
        }
    }

    private fun entriesClosed(template: ClassTemplate) {
        val agilityClass = AgilityClass()
        agilityClass.select("idCompetition=$TEST_ID AND classCode=${template.code}")
        if (agilityClass.found()) {
            agilityClass.entriesClosed()
        }
    }

    private fun copyEntries(idSource: Int, target: AgilityClass) {
        val entry = Entry()
        entry.select("idAgilityClass=$idSource")
        while (entry.next()) {
            target.enter(
                idTeam = entry.idTeam,
                idAccount = entry.idAccount,
                heightCode = entry.heightCode,
                clearRoundOnly = entry.clearRoundOnly,
                entryType = entry.type,
                progress = if (!target.template.isSpecialClass && entry.progress > PROGRESS_ENTERED) PROGRESS_BOOKED_IN else PROGRESS_ENTERED,
                timeEntered = now
            )
        }
    }

    private fun copyResults(idSource: Int, idTarget: Int) {
        val source = Entry()
        val target = Entry()
        source.select("idAgilityClass=$idSource")
        while (source.next()) {
            target.seekEntry(idTarget, source.idTeam, source.teamMember)
            if (target.found()) {
                if (source.time != 0) {
                    if (target.found()) {
                        target.startRun()
                        target.scoreCodes = source.scoreCodes
                        target.time = source.time
                        target.endRun()
                    }
                } else {
                    target.progress = source.progress
                    target.post()
                }
            }
        }
    }

    fun simulateResults(idAgilityClass: Int, remain: Int = 0, groups: Int = 0) {
        var jumpHeightCode = ChangeMonitor("")
        var groupLetter = ChangeMonitor("~")
        val agilityClass = AgilityClass()
        agilityClass.find(idAgilityClass)

        val ring = Ring()
        ring.select(agilityClass.idCompetition, agilityClass.date, agilityClass.ringNumber)
        ring.first()
        ring.idAgilityClass = idAgilityClass
        ring.post()

        var groupList = ""
        for (index in 0..groups - 1) {
            groupList = groupList.append(agilityClass.groupRunningOrder.split(",")[index].quoted)
        }
        val whereGroups = if (groupList.isNotEmpty()) " AND `group` IN ($groupList)" else ""

        println()
        println(agilityClass.name)
        println("====================================================================")
        val entry = Entry()
        entry.select("idAgilityClass=$idAgilityClass AND progress<$PROGRESS_RUNNING$whereGroups", "`group`, FIND_IN_SET(entry.jumpHeightCode, ${agilityClass.heightRunningOrder.quoted}), runningOrder")

        if (agilityClass.template.discipline.oneOf(DISCIPLINE_SNOOKER, DISCIPLINE_GAMBLERS) || agilityClass.template.oneOf(ClassTemplate.TEAM_RELAY, ClassTemplate.SPLIT_PAIRS)) {
            val test = Entry()
            test.agilityClass.joinToParent()
            val where =
                when {
                    agilityClass.template.discipline == DISCIPLINE_SNOOKER -> "agilityClass.classCode=${ClassTemplate.SNOOKER.code}"
                    agilityClass.template.discipline == DISCIPLINE_GAMBLERS -> "agilityClass.classCode=${ClassTemplate.GAMBLERS.code}"
                    agilityClass.template == ClassTemplate.TEAM_RELAY -> "agilityClass.classCode=${ClassTemplate.TEAM_RELAY.code}"
                    agilityClass.template == ClassTemplate.SPLIT_PAIRS -> "agilityClass.classCode=${ClassTemplate.SPLIT_PAIRS.code}"
                    else -> "false"
                }
            test.select(where + " AND entry.progress=50 AND NOT scoreCodes LIKE '%N%'", limit = entry.rowCount)
            test.first()
            agilityClass.extra = test.agilityClass.extra
            agilityClass.progress = CLASS_RUNNING
            agilityClass.post()
            ring.group = entry.group
            ring.heightCode = entry.jumpHeightCode
            ring.post()
            test.beforeFirst()
            while (entry.next()) {
                if (entry.cursor < entry.rowCount - remain) {
                    test.next()
                    entry.startRun()
                    entry.scoreCodes = test.scoreCodes
                    entry.time = test.time
                    entry.endRun()
                    println("  ${entry.id} ${entry.teamDescription} (${entry.jumpHeightText}) ${entry.result}")
                } else {
                    println("  ${entry.id} ${entry.teamDescription} (${entry.jumpHeightText}) REMAIN")
                }
            }

        } else {
            if (agilityClass.extra["specification"]["courseTime"].asInt == 0) {
                agilityClass.courseLength = 155
                if (agilityClass.isUkaStyle || agilityClass.isFabStyle) {
                    agilityClass.extra["specification"]["courseTime"] = 49000
                    agilityClass.extra["specification"]["courseTimeSmall"] = 54000
                } else {
                    for (index in 0..agilityClass.subClassCount - 1) {
                        if (agilityClass.teamSize>1) {
                            agilityClass.setSubClassCourseTime(index, agilityClass.teamSize * 40000)
                        } else {
                            agilityClass.setSubClassCourseTime(index, 49000)
                        }
                    }
                }
                agilityClass.progress = CLASS_RUNNING
                agilityClass.post()
            }
            while (entry.next()) {
                if (entry.cursor < entry.rowCount - remain) {
                    if (groupLetter.hasChanged(entry.group)) {
                        ring.heightCode = entry.jumpHeightCode
                        ring.group = entry.group
                        ring.post()
                    }
                    if (jumpHeightCode.hasChanged(entry.jumpHeightCode)) {
                        ring.heightCode = entry.jumpHeightCode
                        ring.group = entry.group
                        ring.post()
                    }
                    val ct =
                        if (agilityClass.isUkaStyle || agilityClass.isFabStyle) agilityClass.getCourseTime(entry.jumpHeightCode) else agilityClass.getSubClassCourseTime(entry.subClass)
                    val r = if (noWithdraws) 1 else random(30)
                    if (r == 30 && agilityClass.template.parent != ClassTemplate.TEAM) {
                        entry.progress = PROGRESS_WITHDRAWN
                        entry.post()
                        println("  ${entry.id} ${entry.teamDescription} (${entry.jumpHeightText}) WITHDRAWN")
                    } else if (r < 28 || agilityClass.template.parent == ClassTemplate.TEAM || Competition.isGrandFinals) {
                        if (agilityClass.strictRunningOrder) {
                            ring.idEntry = entry.id
                            ring.runningOrder = entry.runningOrder
                            ring.runner = entry.teamDescription
                            ring.runnerHeightCode = entry.jumpHeightCode
                            ring.post()
                        }
                        entry.startRun()
                        
                        if (agilityClass.isRelay) {
                            entry.scoreCodes = ""
                            for (i in agilityClass.template.teamSize downTo 1) {
                                when (random(10)) {
                                    1, 2 -> when (random(10)) {
                                        1 -> entry.scoreCodes += "5D"
                                        2 -> entry.scoreCodes += "RD"
                                        3 -> entry.scoreCodes += "55D"
                                        4 -> entry.scoreCodes += "RRD"
                                        5 -> entry.scoreCodes += "5RD"
                                        6 -> entry.scoreCodes += "RRRD"
                                        else -> entry.scoreCodes += "D"
                                    } 
                                    3 -> entry.scoreCodes += "RF"
                                    4 -> entry.scoreCodes += "F"
                                    5 -> entry.scoreCodes += "R"
                                    6 -> entry.scoreCodes += "RF"
                                    7 -> entry.scoreCodes += "F"
                                    else -> entry.scoreCodes += ""
                                }
                                if (i>1) {
                                    if (random(20)==1) {
                                        entry.scoreCodes += "|B"
                                    } else {
                                        entry.scoreCodes += "|"
                                    }
                                }
                            }
                            
                        } else {
                            when (random(10)) {
                                1, 2 -> entry.scoreCodes = "E"
                                3 -> entry.scoreCodes = "RF"
                                4 -> entry.scoreCodes = "F"
                                5 -> entry.scoreCodes = "R"
                                6 -> entry.scoreCodes = "FR"
                                7 -> entry.scoreCodes = "F"
                                else -> entry.scoreCodes = "C"
                            }
                        }
                        entry.time = random(ct * 110 / 100, ct / 2)
                        if (realTimeFactor > 0) sleep((entry.time / realTimeFactor).toLong())
                        
                        entry.endRun()
                        println("  ${entry.id} ${entry.teamDescription} (${entry.jumpHeightText}) ${entry.result}")
                    } else {
                        println("  ${entry.id} ${entry.teamDescription} (${entry.jumpHeightText}) NO SHOW")
                    }
                } else {
                    println("  ${entry.id} ${entry.teamDescription} (${entry.jumpHeightText}) REMAIN")
                }
            }
        }
    }

    fun test(idCompetition: Int, classTemplate: ClassTemplate) {
        initialize()
        val ringClasses = createTest(idCompetition, classTemplate)
        populate()
        if (classTemplate == ClassTemplate.TRY_OUT) {
            EnglandTryouts.prepareShow(TEST_ID)
        }
        if (classTemplate.isSpecialParent) {
            entriesClosed(classTemplate)
        }
        for (i in 1..ringClasses) {
            processRing()
            closeClass()
        }
    }

    fun testMasters() {
        test(GT_MAY_17, ClassTemplate.MASTERS)
    }

    fun testChamp() {
        test(TUFFLEY, ClassTemplate.KC_CHAMPIONSHIP)
    }

    fun testTryout() {
        test(A4E, ClassTemplate.TRY_OUT)
    }

    fun testChallenge() {
        test(CROOKED_OAK_SEP_17, ClassTemplate.CHALLENGE)
    }

    fun testTeam() {
        test(LYDIARD_APR_17, ClassTemplate.TEAM)
    }

    fun testGandPrix() {
        TestShow.test(TEEJAY_17, ClassTemplate.GRAND_PRIX)
    }

    fun testFinals() {
        TestShow.test(WEEK_17, ClassTemplate.FINAL_ROUND_1)
    }

    fun testGames() {
        TestShow.test(WEEK_17, ClassTemplate.GAMES_CHALLENGE)
    }

    fun testJunior() {
        TestShow.test(WEEK_17, ClassTemplate.JUNIOR_OPEN)
    }

}