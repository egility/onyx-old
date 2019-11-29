/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.general

import org.egility.library.dbobject.*
import java.util.*

/**
 * Created by mbrickman on 11/06/18.
 */
object UkOpenUtils {

    fun setUpClass(idCompetition: Int, template: ClassTemplate, baseDate: Date, idAgilityClassParent: Int?) {
        val agilityClass = AgilityClass()

        if (!agilityClass.find("idCompetition=$idCompetition AND classCode=${template.code}")) {
            agilityClass.append()
            agilityClass.idCompetition = idCompetition
            agilityClass.code = template.code
            agilityClass.date = baseDate.addDays(template.day - 1)
            agilityClass.heightCodes = "OP300,OP400,OP500,OP600"
            agilityClass.jumpHeightCodes = "OP500,OP600,OP300,OP400"
            agilityClass.heightRunningOrder = when (template.day) {
                1 -> "OP600,OP500,OP400,OP300"
                2 -> "OP500,OP600,OP300,OP400"
                else -> "OP300,OP400,OP500,OP600"
            }
            if (idAgilityClassParent != null) agilityClass.idAgilityClassParent = idAgilityClassParent
            agilityClass.name = agilityClass.description
            agilityClass.nameLong = agilityClass.description
            agilityClass.post()
        }

        if (template.hasChildren) {
            val idParent = agilityClass.id
            for (child in template.children) {
                setUpClass(idCompetition, child, baseDate, idParent)
            }
        }
        if (template.isSeries && template.next != null) {
            val next = template.next
            if (next != null) {
                setUpClass(idCompetition, next, baseDate, null)
            }
        }


    }

    fun getAgilityClass(idCompetition: Int, template: ClassTemplate): AgilityClass {
        val agilityClass = AgilityClass()
        agilityClass.select("idCompetition=$idCompetition AND classCode = ${template.code}", "part").first()
        return agilityClass
    }

    fun getAgilityClassList(idCompetition: Int, vararg templates: ClassTemplate): String {
        var result = ""
        var codes = ""
        for (template in templates) {
            codes = codes.append(template.code.toString())
        }
        AgilityClass().where("idCompetition=$idCompetition AND classCode IN ($codes)") {
            result = result.append(id.toString())
        }
        return result
    }


    fun setUpGamblers(idCompetition: Int, host: String = "localhost") {
        Global.databaseHost = host
        with(getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_GAMES_GAMBLERS)) {
            openingTime = 30000
            heights.searchElement("heightCode", "OP300", create = true)["courseTime"] = 46000
            heights.searchElement("heightCode", "OP400", create = true)["courseTime"] = 45000
            heights.searchElement("heightCode", "OP500", create = true)["courseTime"] = 43000
            heights.searchElement("heightCode", "OP600", create = true)["courseTime"] = 43000
            /*
            gambleTimeLarge = 13000
            gambleTimeSmall = 15000
            courseTime = openingTime + gambleTimeLarge
            courseTimeSmall = openingTime + gambleTimeSmall
            */
            obstaclePoints = "55422222222111111111"
            gambleBonusObstacles = "$OBSTACLE_4$OBSTACLE_5$OBSTACLE_6$OBSTACLE_7"
            gambleBonusScore = 8
            post()
        }
    }

    fun setUpClasses(idCompetition: Int) {
        val competition = Competition(idCompetition)
        competition.logo = "uk_open.png"
        competition.post()

        dbExecute("DELETE entry.* FROM entry JOIN agilityClass USING (idAgilityClass) WHERE idCompetition=$idCompetition")
        dbExecute("DELETE FROM agilityClass WHERE idCompetition=$idCompetition")

        setUpClass(idCompetition, ClassTemplate.UK_OPEN_BIATHLON, Competition(idCompetition).dateStart, null)
        setUpClass(idCompetition, ClassTemplate.UK_OPEN_CHALLENGER, Competition(idCompetition).dateStart, null)
        setUpClass(idCompetition, ClassTemplate.UK_OPEN_CHAMPIONSHIP_JUMPING, Competition(idCompetition).dateStart, null)
        setUpClass(idCompetition, ClassTemplate.UK_OPEN_GAMES, Competition(idCompetition).dateStart, null)
        setUpClass(idCompetition, ClassTemplate.UK_OPEN_PENTATHLON, Competition(idCompetition).dateStart, null)
        setUpClass(idCompetition, ClassTemplate.UK_OPEN_STEEPLECHASE_ROUND1, Competition(idCompetition).dateStart, null)


        // Thursday
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_PENTATHLON_AGILITY1).withPost {
            ringNumber = 1
            ringOrder = 1
            groupRunningOrder = "A,B"
            heightRunningOrder = "OP600,OP500,OP400,OP300"
        }
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_PENTATHLON_JUMPING1).withPost {
            ringNumber = 2
            ringOrder = 1
            groupRunningOrder = "B,A"
            heightRunningOrder = "OP600,OP500,OP400,OP300"
        }
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_GAMES_SNOOKER).withPost {
            ringNumber = 1
            ringOrder = 2
            groupRunningOrder = "B,A"
            heightRunningOrder = "OP600,OP500,OP400,OP300"
        }
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_STEEPLECHASE_ROUND1).withPost {
            ringNumber = 2
            ringOrder = 2
            groupRunningOrder = "A,B"
            heightRunningOrder = "OP600,OP500,OP400,OP300"
        }

        // Friday
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_PENTATHLON_AGILITY2).withPost {
            ringNumber = 1
            ringOrder = 1
            groupRunningOrder = "B,A"
            heightRunningOrder = "OP500,OP600,OP300,OP400"
        }
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_PENTATHLON_JUMPING2).withPost {
            ringNumber = 2
            ringOrder = 1
            groupRunningOrder = "A,B"
            heightRunningOrder = "OP500,OP600,OP300,OP400"
        }
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_BIATHLON_JUMPING).withPost {
            ringNumber = 1
            ringOrder = 2
            groupRunningOrder = "B,A"
            heightRunningOrder = "OP500,OP600,OP300,OP400"
        }
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_CHAMPIONSHIP_JUMPING).withPost {
            ringNumber = 2
            ringOrder = 2
            groupRunningOrder = "A,B"
            heightRunningOrder = "OP500,OP600,OP300,OP400"
        }
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_STEEPLECHASE_ROUND2).withPost {
            ringNumber = 1
            ringOrder = 3
            heightRunningOrder = "OP300,OP400,OP500,OP600"
        }

        // Saturday
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_CHAMPIONSHIP_AGILITY).withPost {
            ringNumber = 1
            ringOrder = 1
            heightRunningOrder = "OP400,OP500,OP600,OP300"
        }
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_GAMES_GAMBLERS).withPost {
            ringNumber = 1
            ringOrder = 2
            heightRunningOrder = "OP500,OP600,OP300,OP400"
        }
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_BIATHLON_AGILITY).withPost {
            ringNumber = 1
            ringOrder = 3
            heightRunningOrder = "OP300,OP400,OP500,OP600"
        }

        // Sunday
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_CHALLENGER).withPost {
            ringNumber = 1
            ringOrder = 1
            heightRunningOrder = "OP600,OP300,OP400,OP500"
        }
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_PENTATHLON_SPEEDSTAKES).withPost {
            ringNumber = 1
            ringOrder = 2
            heightRunningOrder = "OP300,OP400,OP500,OP600"
        }
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_CHAMPIONSHIP_FINAL).withPost {
            ringNumber = 1
            ringOrder = 3
            heightRunningOrder = "OP300,OP400,OP500,OP600"
        }


    }

    fun getIdTeam(competitionDog: CompetitionDog): Int {
        with(competitionDog) {
            return if (ukOpenHandler.eq(dog.owner.fullName))
                Team.getIndividualId(dog.idCompetitor, dog.id)
            else if (ukOpenHandler.eq(dog.handler.fullName))
                Team.getIndividualId(dog.idCompetitorHandler, dog.id)
            else
                Team.getIndividualNamedId(dog.id, ukOpenHandler)
        }

    }

    fun generateClassRunningOrders(idCompetition: Int, template: ClassTemplate, print: Boolean=false) {
        dbTransaction {
            val groupHeightCode = ChangeMonitor("?")
            var proposedRunningOrder = 0
            val agilityClass = getAgilityClass(idCompetition, template)
            agilityClass.first()
            agilityClass.runningOrdersGenerated = true
            agilityClass.closedForLateEntries = true
            agilityClass.post()
            val idAgilityClass = agilityClass.id
            val extraClause = if (template == ClassTemplate.UK_OPEN_CHALLENGER) " AND competitionDog.flag=0" else ""
            var orderBy = if (template == ClassTemplate.UK_OPEN_CHALLENGER)
                "json_extract(competitionDog.extra, '\$.ukOpen.heightCode'), rand()"
            else
                "json_extract(competitionDog.extra, '\$.ukOpen.group'), json_extract(competitionDog.extra, '\$.ukOpen.heightCode'), rand()"

            // Step 1 - create entries if not already there
            CompetitionDog().join { dog }.join { dog.owner }.join { dog.handler }
                .where("idCompetition=$idCompetition $extraClause", orderBy) {
                    if (!ukOpenWithdrawn) {
                        agilityClass.enter(
                            idTeam = getIdTeam(this),
                            idAccount = idAccount,
                            heightCode = ukOpenHeightCode,
                            jumpHeightCode = ukOpenHeightCode,
                            entryType = ENTRY_AGILITY_PLAZA,
                            timeEntered = now,
                            group = ukOpenGroup
                        )
                    }
                }

            // Step 2 - adjust running orders
            orderBy = "entry.idAgilityClass, entry.group, entry.jumpHeightCode, entry.runningOrder, rand()"

            Entry().join { this.agilityClass }
                .where("entry.idAgilityClass=$idAgilityClass and entry.progress<$PROGRESS_WITHDRAWN", orderBy) {
                    if (groupHeightCode.hasChanged(group + jumpHeightCode)) {
                        proposedRunningOrder = 1
                    }
                    runningOrder = proposedRunningOrder++
                    post()
                }
            
            if (print) {
                Reports.printRunningOrders(idAgilityClass)
            }
        }

    }

    fun lockEntries(idCompetition: Int, final: Boolean = false) {
        dbTransaction {
            Competition().seek(idCompetition) {
                ukOpenLocked = true
                post()
            }
            generateRunningOrders(idCompetition)
        }
    }

    fun uklockEntries(idCompetition: Int, final: Boolean = false) {
        dbTransaction {
            Competition().seek(idCompetition) {
                ukOpenLocked = false
                post()
            }
        }
    }

    fun generateRunningOrders(idCompetition: Int, print: Boolean=false) {
        dbTransaction {
            // prime event entries
/*
            generateClassRunningOrders(idCompetition, ClassTemplate.UK_OPEN_BIATHLON)
            generateClassRunningOrders(idCompetition, ClassTemplate.UK_OPEN_GAMES)
            generateClassRunningOrders(idCompetition, ClassTemplate.UK_OPEN_PENTATHLON)
*/
            generateClassRunningOrders(idCompetition, ClassTemplate.UK_OPEN_PENTATHLON_AGILITY1, print)
            generateClassRunningOrders(idCompetition, ClassTemplate.UK_OPEN_PENTATHLON_JUMPING1, print)
            generateClassRunningOrders(idCompetition, ClassTemplate.UK_OPEN_PENTATHLON_AGILITY2, print)
            generateClassRunningOrders(idCompetition, ClassTemplate.UK_OPEN_PENTATHLON_JUMPING2, print)
            generateClassRunningOrders(idCompetition, ClassTemplate.UK_OPEN_GAMES_SNOOKER, print)
            generateClassRunningOrders(idCompetition, ClassTemplate.UK_OPEN_STEEPLECHASE_ROUND1, print)
            generateClassRunningOrders(idCompetition, ClassTemplate.UK_OPEN_BIATHLON_JUMPING, print)
            generateClassRunningOrders(idCompetition, ClassTemplate.UK_OPEN_CHAMPIONSHIP_JUMPING, print)

        }
    }


    fun changeHeight(idCompetition: Int, idDog: Int, heightCode: String) {
        CompetitionDog().seek("idCompetition=$idCompetition AND idDog=$idDog") {
            val idTeam = getIdTeam(this)
            Entry().join { agilityClass }
                .where("agilityClass.idCompetition=$idCompetition AND entry.idTeam=$idTeam") {
                    shiftHeight(heightCode)
                }
            ukOpenHeightCode = heightCode
            post()

        }
    }

    fun swapDog(idCompetition: Int, idAgilityClass: Int, idDogFrom: Int, idDogTo: Int) {
        var idTeamFrom = 0
        var idTeamTo = 0
        CompetitionDog().seek("idCompetition=$idCompetition AND idDog=$idDogFrom") {
            idTeamFrom = getIdTeam(this)
        }
        CompetitionDog().seek("idCompetition=$idCompetition AND idDog=$idDogTo") {
            idTeamTo = getIdTeam(this)
        }
        if (idTeamFrom>0 && idTeamTo>0) {
            Entry().where("idAgilityClass=$idAgilityClass AND entry.idTeam=$idTeamFrom") {
                idTeam = idTeamTo
                post()
                moveToRunningOrder(1)
            }
        }
        println("==================")
    }

    fun drop(idCompetition: Int, idDog: Int) {
        CompetitionDog().seek("idCompetition=$idCompetition AND idDog=$idDog") {
            val idTeam = getIdTeam(this)
            println("dropping $ukOpenHandler, ${dog.cleanedPetName}")
            Entry().join { agilityClass }
                .where("agilityClass.idCompetition=$idCompetition AND entry.idTeam=$idTeam") {
                    withdrawRunningOrder()
                }
            ukOpenWithdrawn = true
            post()
        }
    }

    fun undrop(idCompetition: Int, idDog: Int) {
        CompetitionDog().seek("idCompetition=$idCompetition AND idDog=$idDog") {
            val idTeam = getIdTeam(this)
            println("undropping $ukOpenHandler, ${dog.cleanedPetName}")
            Entry().join { agilityClass }
                .where("agilityClass.idCompetition=$idCompetition AND entry.idTeam=$idTeam") {
                    unwithdrawRunningOrder()
                }
            ukOpenWithdrawn = false
            post()
        }
    }

    fun prepareSpeedstakes(agilityClass: AgilityClass) {
        agilityClass.finalizeClass(child = null, subResultsFlag = 0.setToBit(4 - 1))
        AgilityClass.withCompetitionTemplate(agilityClass.idCompetition, ClassTemplate.UK_OPEN_PENTATHLON_SPEEDSTAKES) {
            prepareClass()
        }
    }

    fun prepareChallenge(idCompetition: Int) {
        dbExecute("UPDATE competitionDog SET flag=0 WHERE idCompetition=$idCompetition")

        val finals =
            getAgilityClassList(idCompetition, ClassTemplate.UK_OPEN_PENTATHLON_SPEEDSTAKES, ClassTemplate.UK_OPEN_CHAMPIONSHIP_FINAL)
        dbExecute(
            """
                UPDATE entry
                JOIN team USING (idTeam)
                JOIN competitionDog ON competitionDog.idCompetition=$idCompetition AND competitionDog.idDog=team.idDog
                SET competitionDog.flag=1
                WHERE entry.idAgilityClass IN ($finals)
            """
        )

        val rounds = getAgilityClassList(
            idCompetition,
            ClassTemplate.UK_OPEN_PENTATHLON_AGILITY1,
            ClassTemplate.UK_OPEN_PENTATHLON_JUMPING1,
            ClassTemplate.UK_OPEN_PENTATHLON_AGILITY2,
            ClassTemplate.UK_OPEN_PENTATHLON_JUMPING2,
            ClassTemplate.UK_OPEN_CHAMPIONSHIP_JUMPING,
            ClassTemplate.UK_OPEN_CHAMPIONSHIP_AGILITY
        )
        dbExecute(
            """
                UPDATE entry
                JOIN team USING (idTeam)
                JOIN competitionDog ON competitionDog.idCompetition=$idCompetition AND competitionDog.idDog=team.idDog
                SET competitionDog.flag=1
                WHERE entry.idAgilityClass IN ($rounds) AND entry.progress<>$PROGRESS_RUN
            """
        )

        generateClassRunningOrders(idCompetition, ClassTemplate.UK_OPEN_CHALLENGER)
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_CHALLENGER)
            .forEach { Reports.printRunningOrders(it.id) }
    }

    fun challengersToFinal(idCompetition: Int) {
        val final = getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_CHAMPIONSHIP_FINAL)
        final.first()
        getAgilityClass(idCompetition, ClassTemplate.UK_OPEN_CHALLENGER)
            .withFirst {
                Entry().where("idAgilityClass=${it.id} AND place=1") {
                    final.enter(
                        idTeam = idTeam,
                        idAccount = idAccount,
                        heightCode = heightCode,
                        jumpHeightCode = heightCode,
                        entryType = ENTRY_DEPENDENT_CLASS,
                        timeEntered = now,
                        group = group,
                        runningOrder = 1
                    )
                }
            }

    }

}