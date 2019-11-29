/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.reports

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression
import net.sf.dynamicreports.report.builder.DynamicReports.*
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment
import net.sf.dynamicreports.report.constant.Markup
import net.sf.dynamicreports.report.constant.SplitType
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.dynamicreports.report.definition.ReportParameters
import net.sf.jasperreports.engine.JRDataSource
import org.egility.library.dbobject.AgilityClass
import org.egility.library.dbobject.Entry
import org.egility.library.dbobject.Grade
import org.egility.library.dbobject.Height
import org.egility.library.general.*
import java.io.File
import java.io.OutputStream

class ResultsReport(val idAgilityClass: Int, val subResultsFlag: Int = 0, finalize: Boolean = false, val tournament: Boolean, outfile: String = "", copies: Int = 0, outStream: OutputStream? = null) :
    Report(outfile, copies, outStream) {

    private var mainReport = report()
    private var agilityClass = AgilityClass()
    private var className = ""
    private var height = Height()
    private var entry = Entry()
    private var preEntries = 0
    private val subGroups = ArrayList<Int>()
    var pdfFile = ""

    init {
        try {
            agilityClass.find(idAgilityClass)
            if (agilityClass.found()) {
                if (finalize) {
                    agilityClass.finalizeClass()
                }
                className =
                    if (agilityClass.isUka || agilityClass.isUkOpen) agilityClass.name else agilityClass.describeClass(short = false)
                if (agilityClass.ringNumber > 0) {
                    className = "${className} - Ring ${agilityClass.ringNumber}"
                }
            }
        } catch (e: Throwable) {
            panic(e)
        }

        build()
    }

    fun build() {

        val subReport = cmp.subreport(SubReport())
        subReport.setDataSource(ResultsDataSource())
        try {
            mainReport.setTemplate(reportTemplate)

            if (tournament) {
                val competitionImage = Global.imagesFolder + "/competition/" + agilityClass.competition.logo
                val sponsorImage = Global.imagesFolder + "/sponsor/" + agilityClass.template.logo
                mainReport.pageHeader(createTournamentTitleComponent(competitionImage, agilityClass.describeClassUka(false), "Results"))
                mainReport.detail(subReport, cmp.pageBreak())
                mainReport.pageFooter(createTournamentFooterComponent(sponsorImage, agilityClass.template.website))
            } else {
                mainReport.pageHeader(createTitleComponent(agilityClass.competitionNameDate, className, "Results"))
                mainReport.detail(subReport, cmp.verticalGap(20))
                mainReport.pageFooter(footerComponent)
            }

            mainReport.dataSource = createDataSource()
            mainReport.generate()

        } catch (e: Throwable) {
            panic(e)
        }

    }

    private fun createDataSource(): JRDataSource {
        if (agilityClass.isUka || agilityClass.isUkOpen) {
            if (agilityClass.combineHeights) {
                val dataSource = DRDataSource("heightCode")
                dataSource.add()
                return dataSource
            } else {
                height.selectClassHeights(idAgilityClass, false)
                val dataSource = DRDataSource("heightCode")
                while (height.next()) {
                    dataSource.add(height.code)
                }
                return dataSource
            }
        } else {
            subGroups.clear()
            val dataSource = DRDataSource("subGroup")
            dbQuery("SELECT DISTINCT subClass FROM entry WHERE idAgilityClass=$idAgilityClass AND (progress=$PROGRESS_RUN OR hasRun) ORDER BY subClass") {
                dataSource.add(getInt("subClass"))
                subGroups.add(getInt("subClass"))
            }
            return dataSource

        }
    }

    private inner class SubReport : AbstractSimpleExpression<JasperReportBuilder>() {


        override fun evaluate(reportParameters: ReportParameters): JasperReportBuilder {
            var eliminated = ""
            var nfc = ""
            var report = report()
            report.setTemplate(reportTemplate)
            report.setSummarySplitType(SplitType.PREVENT)

            try {
                val scoreColumn = if (agilityClass.isScoreBasedGame) "Score" else "Faults"
                var titleCentre = ""
                var titleRight = ""

                if (agilityClass.isUka || agilityClass.isUkOpen) {
                    if (agilityClass.combineHeights) {
                        entry.selectResultsUka(agilityClass, subResultsFlag = subResultsFlag)
                    } else {
                        height.cursor = reportParameters.reportRowNumber - 1
                        entry.selectResultsUka(agilityClass, height.code, subResultsFlag = subResultsFlag)
                        preEntries = AgilityClass.getPreEntryCountUka(idAgilityClass, height.code)

                        titleCentre = "Course Time: %d seconds".format(agilityClass.getCourseTime(height.code) / 1000)
                        titleRight = if (agilityClass.template.isRegular) "Pre-Entries: %d".format(preEntries) else ""

                    }

                    while (entry.next()) {
                        if (entry.isNFC) {
                            nfc = nfc.append(entry.teamDescription)
                        } else if (entry.isEffectivelyEliminated && agilityClass.template.summarizeEliminations) {
                            val score = entry.getRunDataEliminated()

                            val teamDescription = if (agilityClass.template == ClassTemplate.TEAM_INDIVIDUAL) {
                                "${entry.teamDescription} (${entry.team.getCompetitorDog(entry.teamMember)})"
                            } else {
                                entry.teamDescription
                            }

                            eliminated = if (score.isEmpty()) {
                                eliminated.append(teamDescription)
                            } else {
                                eliminated.append("${teamDescription} ($score)")
                            }
                        }
                    }

                    if (!agilityClass.combineHeights) {
                        if (tournament) {
                            var text = height.name
                            if (!agilityClass.hasChildren) {
                                text += " ($titleCentre)"
                            }
                            report.title(
                                cmp.verticalList(
                                    cmp.filler().setHeight(10),
                                    cmp.text(text)
                                        .setStyle(root18Style)
                                        .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER),
                                    cmp.filler().setHeight(10)
                                )
                            )
                        } else {
                            if (agilityClass.hasChildren) {
                                report.title(
                                    cmp.horizontalList().add(
                                        cmp.text(height.name).setStyle(bold12Style).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT)
                                    )
                                )

                            } else if (titleRight.isEmpty()) {
                                report.title(
                                    cmp.horizontalList().add(
                                        cmp.text(height.name).setStyle(bold12Style).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT),
                                        cmp.text(titleCentre).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)
                                    )
                                )
                            } else {
                                report.title(
                                    cmp.horizontalList().add(
                                        cmp.text(height.name).setStyle(bold12Style).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT),
                                        cmp.text(titleCentre).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER),
                                        cmp.text(titleRight).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)
                                    )
                                )
                            }
                        }
                    }

                    val listBuilder = cmp.verticalList()
                    if (eliminated.isNotEmpty()) {
                        listBuilder.add(cmp.text("<b>Eliminations:</b> $eliminated").setMarkup(Markup.HTML))
                    }
                    if (nfc.isNotEmpty()) {
                        listBuilder.add(cmp.text("<b>NFC:</b> $nfc").setMarkup(Markup.HTML))
                    }
                    if (eliminated.isNotEmpty() || nfc.isNotEmpty()) {
                        report.summary(listBuilder)
                    }


                    if (
                        (agilityClass.template.parent?.oneOf(
                            ClassTemplate.MASTERS, ClassTemplate.TRY_OUT_PENTATHLON, ClassTemplate.JUNIOR_MASTERS,
                            ClassTemplate.UK_OPEN_PENTATHLON
                        ) == true) ||
                        (agilityClass.template.oneOf(ClassTemplate.UK_OPEN_STEEPLECHASE_ROUND1, ClassTemplate.UK_OPEN_STEEPLECHASE_ROUND2))
                    ) {
                        if (agilityClass.template.hasChildren && !agilityClass.template.isSuperClass) {
                            report.columns(
                                col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                                col.column("Competitor", "competitorDog", type.stringType()),
                                col.column("Faults", "faults", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                                col.column("Time", "time", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter),
                                col.column("Score", "points", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleRight)
                            )
                        } else {
                            report.columns(
                                col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                                col.column("Competitor", "competitorDog", type.stringType()),
                                col.column("Run Data", "runData", type.stringType()).setFixedWidth(15 * 7).setStyle(smallItalicStyle),
                                col.column("Faults", "faults", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                                col.column("Time", "time", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter),
                                col.column("Score", "points", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleRight)
                            )
                        }
                    } else if (agilityClass.template.oneOf(
                            ClassTemplate.GRAND_PRIX_SEMI_FINAL, ClassTemplate.GRAND_PRIX_FINAL,
                            ClassTemplate.BEGINNERS_STEEPLECHASE_SEMI_FINAL, ClassTemplate.BEGINNERS_STEEPLECHASE_FINAL,
                            /*
                                    ClassTemplate.SW_STEEPLECHASE_SEMI_FINAL1, ClassTemplate.SW_STEEPLECHASE_FINAL1,
                                    ClassTemplate.SW_STEEPLECHASE_SEMI_FINAL2, ClassTemplate.SW_STEEPLECHASE_FINAL2,
                                    ClassTemplate.SW_CHALLENGE_JUMPING1, ClassTemplate.SW_CHALLENGE_AGILITY1,
                                    ClassTemplate.SW_CHALLENGE_JUMPING2, ClassTemplate.SW_CHALLENGE_AGILITY2,
                                    */
                            ClassTemplate.UK_OPEN_STEEPLECHASE_ROUND1,
                            ClassTemplate.UK_OPEN_STEEPLECHASE_ROUND2,
                            ClassTemplate.UK_OPEN_CHAMPIONSHIP_JUMPING,
                            ClassTemplate.UK_OPEN_CHAMPIONSHIP_AGILITY,
                            ClassTemplate.UK_OPEN_CHAMPIONSHIP_FINAL,
                            ClassTemplate.UK_OPEN_BIATHLON_JUMPING,
                            ClassTemplate.UK_OPEN_BIATHLON_AGILITY,
                            ClassTemplate.UK_OPEN_CHALLENGER
                        )) {
                        if (agilityClass.template.hasChildren && !agilityClass.template.isSuperClass) {
                            report.columns(
                                col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                                col.column("Competitor", "competitorDog", type.stringType()),
                                col.column("Faults", "faults", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                                col.column("Time", "time", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter)
                            )

                        } else {
                            report.columns(
                                col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                                col.column("Competitor", "competitorDog", type.stringType()),
                                col.column("Run Data", "runData", type.stringType()).setFixedWidth(15 * 7).setStyle(smallItalicStyle),
                                col.column("Faults", "faults", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                                col.column("Time", "time", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter)
                            )
                        }
                    } else if (agilityClass.template.parent?.oneOf(ClassTemplate.JUNIOR_OPEN, ClassTemplate.JUNIOR_OPEN_FINAL) == true) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Competitor", "competitorDog", type.stringType()),
                            col.column("Run Data", "runData", type.stringType()).setFixedWidth(15 * 7).setStyle(smallItalicStyle),
                            col.column("Faults", "faults", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "time", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter),
                            col.column("C/Time", "courseTime", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter),
                            col.column("Score", "points", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleRight)
                        )
                    } else if (agilityClass.template.parent?.oneOf(ClassTemplate.CHALLENGE_FINAL) == true) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Competitor", "competitorDog", type.stringType()),
                            col.column("Run Data", "runData", type.stringType()).setFixedWidth(15 * 7).setStyle(smallItalicStyle),
                            col.column("Faults", "faults", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "time", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter)
                        )
                    } else if (agilityClass.template.oneOf(ClassTemplate.MASTERS, ClassTemplate.JUNIOR_OPEN, ClassTemplate.JUNIOR_OPEN_FINAL, ClassTemplate.JUNIOR_MASTERS)) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Competitor", "competitorDog", type.stringType()),
                            col.column("Jumping", "score1", type.stringType()).setFixedWidth(8 * 7).setStyle(columnStyleRight),
                            col.column("Agility", "score2", type.stringType()).setFixedWidth(8 * 7).setStyle(columnStyleRight),
                            col.column("Combined", "points", type.stringType()).setFixedWidth(8 * 7).setStyle(columnStyleRight)
                        )
                    } else if (agilityClass.template.oneOf(ClassTemplate.CHALLENGE, ClassTemplate.CHALLENGE_FINAL)) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Faults", "faults", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "timeText", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter),
                            col.column("Competitor", "competitorDog", type.stringType()),
                            col.column("Jumping", "class1", type.stringType()).setFixedWidth(12 * 7).setStyle(columnStyleRight),
                            col.column("Agility", "class2", type.stringType()).setFixedWidth(12 * 7).setStyle(columnStyleRight)
                        )
                    } else if (agilityClass.template.oneOf(ClassTemplate.TRY_OUT_PENTATHLON, ClassTemplate.UK_OPEN_PENTATHLON)) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Competitor", "competitorDog", type.stringType()),
                            col.column("Agility 1", "score1", type.stringType()).setFixedWidth(8 * 7).setStyle(columnStyleRight),
                            col.column("Jumping 1", "score2", type.stringType()).setFixedWidth(8 * 7).setStyle(columnStyleRight),
                            col.column("Jumping 2", "score3", type.stringType()).setFixedWidth(8 * 7).setStyle(columnStyleRight),
                            col.column("Agility 2", "score4", type.stringType()).setFixedWidth(8 * 7).setStyle(columnStyleRight),
                            col.column("S/stakes", "score5", type.stringType()).setFixedWidth(8 * 7).setStyle(columnStyleRight),
                            col.column("Combined", "points", type.stringType()).setFixedWidth(8 * 7).setStyle(columnStyleRight)
                        )
                    } else if (agilityClass.template == ClassTemplate.TEAM) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Team", "team", type.stringType()),
                            col.column("Dog 1", "faults1", type.stringType()).setFixedWidth(8 * 7).setStyle(columnStyleRight),
                            col.column("Dog 2", "faults2", type.stringType()).setFixedWidth(8 * 7).setStyle(columnStyleRight),
                            col.column("Dog 3", "faults3", type.stringType()).setFixedWidth(8 * 7).setStyle(columnStyleRight),
                            col.column("Relay", "faults4", type.stringType()).setFixedWidth(8 * 7).setStyle(columnStyleRight),
                            col.column("Time", "time", type.stringType()).setFixedWidth(8 * 7).setStyle(columnStyleRight),
                            col.column("Score", "points", type.stringType()).setFixedWidth(8 * 7).setStyle(columnStyleRight)
                        )

                    } else if (agilityClass.template == ClassTemplate.TEAM_INDIVIDUAL) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Team", "team", type.stringType()),
                            col.column("Competitor", "teamMember", type.stringType()),
                            col.column("Run Data", "runData", type.stringType()).setFixedWidth(20 * 7).setStyle(smallItalicStyle),
                            col.column(scoreColumn, "score", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "timeText", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter)
                        )
                    } else if (agilityClass.template == ClassTemplate.TEAM_RELAY) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Team", "team", type.stringType()),
                            col.column("Run Data", "runData", type.stringType()).setFixedWidth(15 * 7).setStyle(smallItalicStyle),
                            col.column("Faults", "faults", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "time", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter)
                        )
                    } else if (agilityClass.template == ClassTemplate.SPLIT_PAIRS) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Pair", "pair", type.stringType()),
                            col.column("Run Data", "runData", type.stringType()).setFixedWidth(10 * 7).setStyle(smallItalicStyle),
                            col.column("Faults", "faults", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "time", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter),
                            col.column("Score", "points", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleRight)
                        )
                    } else if (agilityClass.template == ClassTemplate.GRAND_PRIX) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Faults", "faults", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "timeText", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter),
                            col.column("Competitor", "competitorDog", type.stringType())
                        )
                    } else if (agilityClass.template == ClassTemplate.UK_OPEN_BIATHLON) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Competitor", "competitorDog", type.stringType()),
                            col.column("Jumping", "faults1", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleRight),
                            col.column("Agility", "faults2", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleRight),
                            col.column("Faults", "faults", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "timeText", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter)
                        )
                    } else if (agilityClass.template.parent?.oneOf(
                            ClassTemplate.GAMES_CHALLENGE, ClassTemplate.TRY_OUT_GAMES,
                            ClassTemplate.UK_OPEN_GAMES, ClassTemplate.UK_OPEN_GAMES_SNOOKER
                        ) == true) {
                        report.columns(
                            col.column("Rank", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column(scoreColumn, "score", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "timeText", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter),
                            col.column("Competitor", "competitorDog", type.stringType()),
                            col.column("Run Data", "runData", type.stringType()).setFixedWidth(20 * 7).setStyle(smallItalicStyle)
                        )
                    } else if (agilityClass.template.oneOf(
                            ClassTemplate.GAMES_CHALLENGE, ClassTemplate.TRY_OUT_GAMES,
                            /*ClassTemplate.SW_GAMES_CHALLENGE, */ClassTemplate.UK_OPEN_GAMES
                        )) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Competitor", "competitorDog", type.stringType()),
                            col.column("Snooker", "points1", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleRight),
                            col.column("Gamblers", "points2", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleRight),
                            col.column("Total", "pointsInt", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleRight),
                            col.column("Time", "time", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter)
                        )

                    } else {
                        report.columns(
                            col.column("Prize", "prize", type.stringType()).setFixedWidth(6 * 7),
                            col.column(scoreColumn, "score", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "timeText", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter),
                            col.column("Competitor", "competitorDog", type.stringType()),
                            col.column("Run Data", "runData", type.stringType()).setFixedWidth(20 * 7).setStyle(smallItalicStyle),
                            col.column("Points", "progressionPoints", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight)
                        )
                    }
                } else /* is KC */ {
                    val subClass = subGroups[reportParameters.reportRowNumber - 1]
                    entry.selectResults(agilityClass, subClass)
                    if (!agilityClass.hasChildren) {
                        val time = if (agilityClass.isFabStyle) 
                            agilityClass.getCourseTime(agilityClass.subClassHeight(subClass)) / 1000 
                        else 
                            agilityClass.getSubClassCourseTime(subClass) / 1000
                        titleRight = "Course Time: $time seconds (${agilityClass.courseLength}m)"
                        titleCentre = "Judge: ${agilityClass.judge}"
                    } else if (agilityClass.template == ClassTemplate.KC_CHAMPIONSHIP_HEAT) {
                        titleRight = "Entries: ${AgilityClass.getEntryCount(idAgilityClass)}"
                    }
                    while (entry.next()) {
                        if (entry.isNFC) {
                            nfc = nfc.append(entry.teamDescription)
                        } else if (entry.isEffectivelyEliminated && agilityClass.template.summarizeEliminations) {
                            val score = entry.getRunDataEliminated()
                            val name = if (agilityClass.isKc) entry.teamDescriptionFormal else entry.teamDescription
                            if (score.isEmpty()) {
                                eliminated = eliminated.append(name)
                            } else {
                                eliminated = eliminated.append("$name ($score)")
                            }
                        }
                    }


                    if (agilityClass.subClassCount > 1 || titleCentre.isNotEmpty() || titleRight.isNotEmpty()) {
                        val titleList = cmp.horizontalList()
                        if (agilityClass.subClassCount > 1) {
                            titleList.add(
                                cmp.text(agilityClass.subClassDescription(subClass, shortGrade = false)).setStyle(bold12Style).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT)
                            )
                        }
                        if (titleCentre.isNotEmpty()) {
                            titleList.add(
                                cmp.text(titleCentre).setStyle(rootStyle).setHorizontalTextAlignment(if (agilityClass.subClassCount > 1) HorizontalTextAlignment.CENTER else HorizontalTextAlignment.LEFT)
                            )
                        }
                        if (titleRight.isNotEmpty()) {
                            titleList.add(
                                cmp.text(titleRight).setStyle(rootStyle).setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)
                            )
                        }
                        report.title(titleList)
                    }

                    val listBuilder = cmp.verticalList()
                    if (eliminated.isNotEmpty()) {
                        listBuilder.add(cmp.text("<b>Eliminations:</b> $eliminated").setMarkup(Markup.HTML))
                    }
                    if (nfc.isNotEmpty()) {
                        listBuilder.add(cmp.text("<b>NFC:</b> $nfc").setMarkup(Markup.HTML))
                    }
                    if (eliminated.isNotEmpty() || nfc.isNotEmpty()) {
                        report.summary(listBuilder)
                    }

                    if (agilityClass.template.oneOf(ClassTemplate.KC_CHAMPIONSHIP_JUMPING, ClassTemplate.KC_CHAMPIONSHIP_AGILITY, ClassTemplate.KC_CHAMPIONSHIP_FINAL)) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Competitor", "competitorDogFormal", type.stringType()),
                            col.column("Run Data", "runData", type.stringType()).setFixedWidth(10 * 7).setStyle(smallItalicStyle),
                            col.column("Faults", "faults", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "time", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter)
                        )
                    } else if (agilityClass.template.oneOf(ClassTemplate.KC_CHAMPIONSHIP_HEAT, ClassTemplate.KC_GRAMPION_HEAT)) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Competitor", "competitorDogFormal", type.stringType()),
                            col.column("Jumping", "points1", type.stringType()).setFixedWidth(12 * 4).setStyle(columnStyleRight),
                            col.column("Agility", "points2", type.stringType()).setFixedWidth(12 * 4).setStyle(columnStyleRight),
                            col.column("Points", "pointsInt", type.stringType()).setFixedWidth(12 * 4).setStyle(columnStyleRight),
                            col.column("Faults", "faults", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "time", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter)
                        )
                    } else if (agilityClass.template.oneOf(ClassTemplate.KC_GRAMPION_AGILITY, ClassTemplate.KC_GRAMPION_JUMPING)) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column(scoreColumn, "score", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "timeText", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter),
                            col.column("Competitor", if (agilityClass.isKc) "competitorDogFormal" else "competitorDog", type.stringType()),
                            col.column("Run Data", "runData", type.stringType()).setFixedWidth(10 * 7).setStyle(smallItalicStyle),
                            col.column("Points", "progressionPoints", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight)
                        )
                    } else if (agilityClass.template.oneOf(ClassTemplate.KC_CHALLENGE)) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Competitor", "competitorDog", type.stringType()),
                            col.column("Jumping", "score1", type.stringType()).setFixedWidth(12 * 4).setStyle(columnStyleRight),
                            col.column("Agility", "score2", type.stringType()).setFixedWidth(12 * 4).setStyle(columnStyleRight),
                            col.column("Combined", "points", type.stringType()).setFixedWidth(8 * 7).setStyle(columnStyleRight)
                        )
                    } else if (agilityClass.template.oneOf(ClassTemplate.KC_CRUFTS_TEAM)) {
                        report.columns(
                            col.column("Place", "place", type.stringType()).setFixedWidth(6 * 7),
                            col.column("Team", "team", type.stringType()),
                            col.column("Run Data", "runData", type.stringType()).setFixedWidth(15 * 7).setStyle(smallItalicStyle),
                            col.column("Faults", "faults", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "time", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter)
                        )
                    } else if (agilityClass.template.isIfcs) {
                        report.columns(
                            col.column("Place", "prize", type.stringType()).setFixedWidth(6 * 7),
                            col.column(scoreColumn, "score", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "timeText", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter),
                            col.column("Competitor", "competitorDog", type.stringType()),
                            col.column("Run Data", "runData", type.stringType()).setFixedWidth(10 * 7).setStyle(smallItalicStyle),
                            col.column("Points", "progressionPoints", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight)
                        )
                    } else if (agilityClass.template.isFab) {
                        report.columns(
                            col.column("Place", "prize", type.stringType()).setFixedWidth(6 * 7),
                            col.column(scoreColumn, "score", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "timeText", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter),
                            col.column("Competitor", "competitorDog", type.stringType()),
                            col.column("Run Data", "runData", type.stringType()).setFixedWidth(10 * 7).setStyle(smallItalicStyle)
                        )
                    } else if (agilityClass.gradeCodes.contains(",")) {
                        report.columns(
                            col.column("Place", "prize", type.stringType()).setFixedWidth(6 * 7),
                            col.column(scoreColumn, "score", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "timeText", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter),
                            col.column("Competitor", if (agilityClass.isKc) "competitorDogFormal" else "competitorDog", type.stringType()),
                            col.column("Grade", "grade", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter),
                            col.column("Run Data", "runData", type.stringType()).setFixedWidth(10 * 7).setStyle(smallItalicStyle),
                            col.column("Points", "progressionPoints", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight)
                        )
                    } else {
                        report.columns(
                            col.column("Place", "prize", type.stringType()).setFixedWidth(6 * 7),
                            col.column(scoreColumn, "score", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight),
                            col.column("Time", "timeText", type.stringType()).setFixedWidth(7 * 7).setStyle(columnStyleCenter),
                            col.column("Competitor", if (agilityClass.isKc) "competitorDogFormal" else "competitorDog", type.stringType()),
                            col.column("Run Data", "runData", type.stringType()).setFixedWidth(10 * 7).setStyle(smallItalicStyle),
                            col.column("Points", "progressionPoints", type.stringType()).setFixedWidth(6 * 7).setStyle(columnStyleRight)
                        )
                    }
                }
                report.setDataSource(createDataSource())
                report.setDetailOddRowStyle(oddStyle)
            } catch (e: Throwable) {
                panic(e)
            }

            return report
        }
    }

    private inner class ResultsDataSource : AbstractSimpleExpression<JRDataSource>() {

        override fun evaluate(reportParameters: ReportParameters): JRDataSource {
            var haveABC = false
            var scoringRuns = 0
            var calcPlace = 1
            val dataSource = DRDataSource(
                "prize", "score", "timeText", "competitorDog", "competitorDogFormal", "runData", "progressionPoints",
                "place", "points", "pointsInt", "faults", "time", "courseTime", "scoreCodes",
                "faults1", "faults2", "faults3", "faults4", "faults5",
                "class1", "class2", "class3", "class4", "class5",
                "score1", "score2", "score3", "score4", "score5",
                "points1", "points2", "points3", "points4", "points5",
                "team", "teamMember", "pair", "grade"
            )
            try {
                entry.beforeFirst()
                while (entry.next()) {
                    var prizeText = entry.prizeText
//                    if (entry.agilityClass.competition.isFabKc && !entry.team.dog.isCollie && !entry.isEliminated && !haveABC) {
                     if (entry.agilityClass.fastestABC && !entry.team.dog.isCollie && !entry.isEliminated && !haveABC) {
                        prizeText+= "*"
                        haveABC = true
                    }
                    val template = agilityClass.template
                    var progressionPoints = ""
                    var hasRun = if (subResultsFlag > 0)
                        entry.subResultsFlag and subResultsFlag == subResultsFlag
                    else
                        entry.hasRun
                    if (entry.progressionPoints > 0 && !entry.clearRoundOnly) {
                        progressionPoints = Integer.toString(entry.progressionPoints)
                    }
                    if (entry.progress == PROGRESS_VOID) {
                        progressionPoints = "VOID"
                    }

                    val teamName = if (agilityClass.isUka) entry.teamMemberNameAndUkaTitle else entry.teamMemberName


                    if (entry.isNFC) {

                    } else if (entry.isEffectivelyEliminated && template.summarizeEliminations) {

                    } else {
                        scoringRuns++
                        dataSource.add(
                            prizeText,
                            if (template == ClassTemplate.UK_OPEN_GAMES_GAMBLERS) entry.points.toString() else entry.scoreText,
                            entry.simpleTimeText,
                            if (entry.clearRoundOnly) teamName + " (CRO)" else teamName,
                            entry.teamDescriptionFormal,
                            entry.getRunData(true),
                            if (hasRun) progressionPoints else "",
                            if (hasRun) if (subResultsFlag > 0) calcPlace++.toString() else entry.place.toString() else "",
                            if (hasRun) entry.points.dec3 else "",
                            if (hasRun) entry.points.toString() else "",
                            if (hasRun) entry.faults.dec3Int else "",
                            if (hasRun) entry.time.dec3 else "",
                            entry.courseTime.dec3,
                            entry.scoreCodes,
                            entry.subResults[0]["faults"].asInt.dec3,
                            entry.subResults[1]["faults"].asInt.dec3,
                            entry.subResults[2]["faults"].asInt.dec3,
                            entry.subResults[3]["faults"].asInt.dec3,
                            entry.subResults[4]["faults"].asInt.dec3,
                            if (entry.subResults[0]["hasRun"].asBoolean) "${entry.subResults[0]["faults"].asInt.dec3Int} (${entry.subResults[0]["time"].asInt.dec3})" else "",
                            if (entry.subResults[1]["hasRun"].asBoolean) "${entry.subResults[1]["faults"].asInt.dec3Int} (${entry.subResults[1]["time"].asInt.dec3})" else "",
                            if (entry.subResults[2]["hasRun"].asBoolean) "${entry.subResults[2]["faults"].asInt.dec3Int} (${entry.subResults[2]["time"].asInt.dec3})" else "",
                            if (entry.subResults[3]["hasRun"].asBoolean) "${entry.subResults[3]["faults"].asInt.dec3Int} (${entry.subResults[3]["time"].asInt.dec3})" else "",
                            if (entry.subResults[4]["hasRun"].asBoolean) "${entry.subResults[4]["faults"].asInt.dec3Int} (${entry.subResults[4]["time"].asInt.dec3})" else "",

                            if (entry.subResults[0]["hasRun"].asBoolean) entry.subResults[0]["points"].asInt.dec3 else "",
                            if (entry.subResults[1]["hasRun"].asBoolean) entry.subResults[1]["points"].asInt.dec3 else "",
                            if (entry.subResults[2]["hasRun"].asBoolean) entry.subResults[2]["points"].asInt.dec3 else "",
                            if (entry.subResults[3]["hasRun"].asBoolean) entry.subResults[3]["points"].asInt.dec3 else "",
                            if (entry.subResults[4]["hasRun"].asBoolean) entry.subResults[4]["points"].asInt.dec3 else "",

                            if (entry.subResults[0]["hasRun"].asBoolean) entry.subResults[0]["points"].asInt.toString() else "",
                            if (entry.subResults[1]["hasRun"].asBoolean) entry.subResults[1]["points"].asInt.toString() else "",
                            if (entry.subResults[2]["hasRun"].asBoolean) entry.subResults[2]["points"].asInt.toString() else "",
                            if (entry.subResults[3]["hasRun"].asBoolean) entry.subResults[3]["points"].asInt.toString() else "",
                            if (entry.subResults[4]["hasRun"].asBoolean) entry.subResults[4]["points"].asInt.toString() else "",
                            entry.team.teamName,
                            entry.team.getCompetitorDog(entry.teamMember),
                            "${entry.team.getCompetitorDog(1)} / ${entry.team.getCompetitorDog(2)}",
                            Grade.getGradeShort(entry.gradeCode)
                        )
                    }
                }
                if (scoringRuns == 0) {
                    dataSource.add()
                }
            } catch (e: Throwable) {
                panic(e)
            }


            return dataSource
        }

    }

    companion object : ReportCompanion {

        override val keyword = Reports.RESULTS

        override fun generateJson(reportRequest: Json, pdfFile: Param<String>) {
            pdfFile.value = generate(
                idAgilityClass = reportRequest["idAgilityClass"].asInt,
                subResultsFlag = reportRequest["subResultsFlag"].toInt(),
                finalize = reportRequest["finalize"].asBoolean,
                tournament = reportRequest["tournament"].asBoolean,
                copies = reportRequest["copies"].asInt,
                pdf = reportRequest["pdf"].asBoolean,
                regenerate = reportRequest["regenerate"].asBoolean
            )
        }

        fun generate(idAgilityClass: Int, subResultsFlag: Int = 0, finalize: Boolean = false, tournament: Boolean, copies: Int = 1, pdf: Boolean = false, regenerate: Boolean = true): String {
            val outFile = if (tournament) {
                nameOutfile("${keyword}_tournament", pdf, idAgilityClass = idAgilityClass)
            } else {
                nameOutfile(keyword, pdf, idAgilityClass = idAgilityClass)
            }
            if (outFile.isEmpty() || regenerate || !File(outFile).cached()) {
                ResultsReport(idAgilityClass, subResultsFlag, finalize, tournament, outFile, copies)
            }
            return outFile
        }
    }

}
 