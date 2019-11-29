/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.linux.tools

import org.egility.library.dbobject.*
import org.egility.library.general.*
import org.odftoolkit.odfdom.type.Color
import org.odftoolkit.simple.style.Border
import org.odftoolkit.simple.style.Font
import org.odftoolkit.simple.style.StyleTypeDefinitions

/**
 * Created by mbrickman on 20/06/18.
 */
class KcDocuments(val idCompetition: Int) {


    val condensed = Font("Ubuntu Condensed", StyleTypeDefinitions.FontStyle.REGULAR, 10.0)
    val condensedBlue = Font("Ubuntu Condensed", StyleTypeDefinitions.FontStyle.REGULAR, 10.0, Color.BLUE)
    val condensedBold = Font("Ubuntu Condensed", StyleTypeDefinitions.FontStyle.BOLD, 10.0)
    val arial10 = Font("Arial", StyleTypeDefinitions.FontStyle.REGULAR, 10.0)
    val arialBold10 = Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 10.0)
    val times = Font("Times New Roman", StyleTypeDefinitions.FontStyle.REGULAR, 11.0)
    val timesBold = Font("Times New Roman", StyleTypeDefinitions.FontStyle.BOLD, 11.0)
    val timesBoldItalic = Font("Times New Roman", StyleTypeDefinitions.FontStyle.BOLDITALIC, 11.0)
    val arial11 = Font("Arial", StyleTypeDefinitions.FontStyle.REGULAR, 11.0)
    val arialBold18 = Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 18.0)
    val arialBold14 = Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 14.0)
    val arialBold12 = Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 12.0)
    val arialBold12Maroon = Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 12.0, Color.MAROON)
    val regular = Font("Times New Roman", StyleTypeDefinitions.FontStyle.REGULAR, 11.0)

    val headingBackground = "#fcf8cc"
    val enteredBackground = "#fcf8cc"

    val competition = Competition(idCompetition)

    fun champCatalogues(marked: Boolean = false) {
        AgilityClass().where("agilityClass.idCompetition=$idCompetition AND agilityClass.classCode=${ClassTemplate.KC_CHAMPIONSHIP.code}") {
            val odf=champCatalogue(id, marked)
            val itinerariesPdf = odtToPdf(odf, keep = false)
        }
    }

    data class RunningOrder(var jumping: Int = 0, var agility: Int = 0)

    fun getRunningOrders(idCompetition: Int, heightCodes: String): Map<Int, RunningOrder> {
        val map = HashMap<Int, RunningOrder>()

        Entry().join { agilityClass }
                .where("agilityClass.idCompetition=$idCompetition AND agilityClass.heightCodes=${heightCodes.quoted} " +
                        "AND agilityClass.classCode IN (${ClassTemplate.KC_CHAMPIONSHIP_JUMPING.code}, ${ClassTemplate.KC_CHAMPIONSHIP_AGILITY.code})") {
                    val item = map.getOrElse(idTeam) {
                        val new = RunningOrder()
                        map[idTeam] = new
                        new
                    }
                    if (agilityClass.code == ClassTemplate.KC_CHAMPIONSHIP_JUMPING.code) {
                        item.jumping = runningOrder
                    } else {
                        item.agility = runningOrder
                    }
                }
        return map
    }

    fun champCatalogue(idAgilityClass: Int, marked: Boolean = false): String {

        var odtFile = ""

        AgilityClass().join { competition }.seek(idAgilityClass) {
            val map = getRunningOrders(idCompetition, heightCodes)
            odtFile = Global.showDocumentPath(idCompetition, "championship_catalogue_${Height.getHeightName(heightCodes)}" + if(marked) "_marked" else "", "odt")

            a4Document(odtFile, 10.0, 5.0) {

                p(arialBold18, "Championship Catalogue").alignCenter().marginBottom("0.5cm")
                p(arialBold14, nameLong).alignCenter().marginBottom("0.5cm")

                table(2, 6, borderless = true) {
                    widths(40.0, null)
                    cell(0, 0, arialBold10, "Society:"); cell(1, 0, arial10, competition.society)
                    cell(0, 1, arialBold10, "Show Type:"); cell(1, 1, arial10, "Championship")
                    cell(0, 2, arialBold10, "Venue:"); cell(1, 2, arial10, competition.venueAddress)
                    cell(0, 3, arialBold10, "Date:"); cell(1, 3, arial10, competition.dateRangeYear)
                    cell(0, 4, arialBold10, "Guarantors:"); cell(1, 4, arial10, competition.guarantors.replace(";", "\n"))
                    cell(0, 5, arialBold10, "Show Secretary:"); cell(1, 5, arial10, competition.secretary)
                }.marginBottom("1.0cm")

                if (marked) {
                    var row = 0
                    table(2, 1, borderless = true) {
                        widths(null, 20.0)

                        cellCenter(1, row++, condensedBold, "Award")

                        Entry().join { team }.join { team.dog }.join { team.competitor }
                                .where("entry.idAgilityClass=$idAgilityClass",
                                        "competitor.familyName, competitor.givenName, dog.registeredName") {
                                    val item = map.getOrDefault(idTeam, RunningOrder())
                                    cell(0, row) {
                                        p(condensed, "${team.competitor.familyName.upperCase}, ${team.competitor.givenName.naturalCase}").marginTop("0.1cm")
                                        p(condensed, "${team.dog.code}, ${team.dog.registeredName}, ${team.dog.idKC}, ${Breed.getBreedName(team.dog.idBreed)}, " +
                                                "${team.dog.dateOfBirth.shortText}, ${team.dog.genderText}, J${item.jumping}, A${item.agility}"
                                        ).marginLeft("0.5cm").marginBottom("0.1cm")

                                    }
                                    cell(1, row++).setBorders(StyleTypeDefinitions.CellBordersType.BOTTOM, Border(Color.BLACK, 0.5, StyleTypeDefinitions.SupportedLinearMeasure.PT))
                                }

                    }.keepRowsTogether()

                    p(arialBold14, "Awards").alignCenter().marginBottom("0.5cm").keepWithNext().marginTop("1.0cm").marginBottom("1.0cm")

                    table(5, 3, borderless = true) {
                        widths(10.0, 15.0, null, 15.0, 30.0)
                        for (place in 1..3) {
                            row = (place - 1) * 2
                            val top = if (place == 1) "0.0cm" else "2.0cm"
                            cell(0, row) { p(condensed, place.ordinal()).marginTop(top) }
                            cell(1, row) { p(condensed, "Dog:").marginTop(top) }
                            cell(2, row).setBorders(StyleTypeDefinitions.CellBordersType.BOTTOM, Border(Color.BLACK, 0.5, StyleTypeDefinitions.SupportedLinearMeasure.PT))
                            cell(3, row) { p(condensed, "Faults:").marginTop(top).marginLeft("0.2cm") }
                            cell(4, row).setBorders(StyleTypeDefinitions.CellBordersType.BOTTOM, Border(Color.BLACK, 0.5, StyleTypeDefinitions.SupportedLinearMeasure.PT))

                            cell(1, row + 1) { p(condensed, "Owner:").marginTop("1.0cm") }
                            cell(2, row + 1).setBorders(StyleTypeDefinitions.CellBordersType.BOTTOM, Border(Color.BLACK, 0.5, StyleTypeDefinitions.SupportedLinearMeasure.PT))
                            cell(3, row + 1) { p(condensed, "Time:").marginTop("1.0cm").marginLeft("0.2cm") }
                            cell(4, row + 1).setBorders(StyleTypeDefinitions.CellBordersType.BOTTOM, Border(Color.BLACK, 0.5, StyleTypeDefinitions.SupportedLinearMeasure.PT))
                        }

                    }.keepTogether()
                } else {
                    Entry().join { team }.join { team.dog }.join { team.competitor }
                            .where("entry.idAgilityClass=$idAgilityClass",
                                    "competitor.familyName, competitor.givenName, dog.registeredName") {
                                val item = map.getOrDefault(idTeam, RunningOrder())
                                p(condensed, "${team.competitor.familyName.upperCase}, ${team.competitor.givenName.naturalCase}").keepWithNext()
                                p(condensed, "${team.dog.code}, ${team.dog.registeredName}, ${team.dog.idKC}, ${Breed.getBreedName(team.dog.idBreed)}, " +
                                        "${team.dog.dateOfBirth.shortText}, ${team.dog.genderText}, J${item.jumping}, A${item.agility}"
                                ).marginLeft("0.5cm")
                            }
                }
            }


        }

        return odtFile

    }
}

