/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.linux.tools

import com.itextpdf.text.Document
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfSmartCopy
import jxl.CellType
import jxl.Sheet
import jxl.Workbook
import jxl.format.Border
import jxl.format.BorderLineStyle
import jxl.format.Colour
import jxl.format.UnderlineStyle
import jxl.write.*
import jxl.write.Number
import org.egility.library.database.DbQuery
import org.egility.library.dbobject.*
import org.egility.library.dbobject.Competition.Companion.kc2020RulesBaseDate
import org.egility.library.dbobject.LedgerItem
import org.egility.library.dbobject.TabletLog
import org.egility.library.general.*
import org.egility.library.general.ClassTemplate.Companion.IND_MISC
import org.egility.linux.reports.Report
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


/**
 * Created by mbrickman on 27/09/17.
 */

object PlazaAdmin {

    var MAX_RINGS = 6

/*
    val UNIQUE_NAME = 10
    val SHOW_NAME = 20
    val SHOW_MANAGER = 40
    val SHOW_SECRETARY = 40
    val VENUE = 50
    val VENUE_POSTCODE = 60

    val DATE_OPENS = 100
    val DATE_CLOSES = 110
    val DATE_START = 120
    val DATE_END = 130

    val CAPPING_LEVEL = 200
    val LATE_ENTRY_RESTRICTED = 210
    val ENTRY_FEE = 220
    val LATE_ENTRY_FEE = 230

    val CAMPING_CAP = 300
    val CAMPING_START = 310
    val CAMPING_END = 320
    val CAMPING_RATE_DAY = 330
    val CAMPING_RATE = 340

    val SHEET_UKA_SHOW_TEMPLATE = 1
    val SHEET_UKA_CLASS_TEMPLATE = 2
    val SHEET_RING_PLAN_TEMPLATE = 3

    val DATE_GENERATED = 30
    val GENERATED_VERSION = 40
*/

    val SHOW_ID = 10
    val UNIQUE_NAME = 20
    val DATE_GENERATED = 30
    val GENERATED_VERSION = 40
    val SHOW_TYPE = 50
    val SHOW_HIDDEN = 60
    val SHOW_PROVISIONAL = 70

    val ENTITY_KEY = 80

    val SHOW_NAME = 100
    val BRIEF_NAME = 102
    val SHOW_MANAGER = 110
    val SHOW_MANAGER_EMAIL = 111
    val SHOW_MANAGER_PHONE = 112
    val SHOW_SECRETARY = 120
    val SHOW_SECRETARY_EMAIL = 121
    val SHOW_SECRETARY_PHONE = 122
    val SHOW_SECRETARY_CODES = 123
    val VENUE_NAME = 128
    val VENUE_ADDRESS = 130
    val VENUE_POSTCODE = 140

    val DATE_OPENS = 200
    val DATE_CLOSES = 210
    val DATE_START = 220
    val DATE_END = 230

    val CAPPING_LEVEL = 300
    val LATE_ENTRY_RESTRICTED = 310
    val ENTRY_FEE = 320
    val ENTRY_FEE_MEMBERS = 321
    val MINIMUM_FEE = 322
    val MINIMUM_FEE_MEMBERS = 323
    val MAXIMUM_FEE = 324
    val MAXIMUM_FEE_MEMBERS = 325
    val LATE_ENTRY_FEE = 330
    val IFCS_ENTRY_FEE = 331

    val BANK_ACCOUNT = 340
    val BANK_SORT = 342
    val BANK_NUMBER = 344

    val ADMIN_FEE = 350

    val CAMPING_PITCHES = 400
    val CAMPING_CAP_SYSTEM = 401
    val CAMPING_MAIN_BLOCK = 402
    val CAMPING_START = 410
    val CAMPING_END = 420
    val CAMPING_RATE_DAY = 430
    val CAMPING_RATE = 440
    val CAMPING_NOTE = 450

    val PLACE_RULE = 500
    val TROPHY_RULE = 501
    val AWARD_RULE = 502
    val LH_ORDER = 511
    val MAX_RUNS = 520
    val PROCESSING_FEE = 530
    val SWAP_FEE = 531
    val ANYSIZE_RULE = 540
    val LHO_RULE = 550
    val AGE_BASE_DATE = 560
    val ALL_OR_NOTHING = 570
    val SHOW_OPTIONS = 580
    val BONUS_CATEGORIES = 590
    val NOTES = 900


    val SHEET_SHOW_TEMPLATE = 3
    val SHEET_UKA_CLASS_TEMPLATE = 4
    val SHEET_RING_PLAN_OVERVIEW_TEMPLATE = 5
    val SHEET_RING_PLAN_DAY_TEMPLATE = 6

    val SHEET_KC_CLASS_TEMPLATE = 7
    val SHEET_FAB_CLASS_TEMPLATE = 8
    val SHEET_INDEPENDENT_CLASS_TEMPLATE = 9

    val SHEET_AWARD_TEMPLATE = 10

    ///////////////////////////////////////////////////////

    val SHEET_ENTRIES_TEMPLATE = 200
    val SHEET_RUNNING_ORDERS_TEMPLATE = 201

    val SHEET_CRUFTS_TEAMS_TEMPLATE = 210
    val SHEET_CRUFTS_TEAMS_DETAIL = 211

    val SHEET_UKA_MEMBERS = 301
    val SHEET_UKA_MEMBERS_LAPSED = 302
    val SHEET_UKA_MEMBERS_PENDING = 303
    val SHEET_UKA_DOGS = 310
    val SHEET_UKA_DOGS_PENDING = 311

    val SHEET_UKA_FINALS_QUALIFIED = 400
    val SHEET_UKA_FINALS_QUALIFIED_SHOW = 401

    val SHEET_UKA_FINAL_INVITED = 402
    val SHEET_UKA_FINAL_INVITED_CLASS = 403

    val SHEET_UK_OPEN_GROUPS = 500
    val SHEET_UK_OPEN_GROUPS_DOG = 501


    fun exportShowTemplate(idCompetition: Int, advanceYear: Boolean = false): String {
        var path = ""

        Competition().join { entity }.seek(idCompetition) {

            val thisYear = dateStart.format("YY")
            val nextYear = dateStart.addDays(364).format("YY")
            val uniqueName = if (advanceYear)
                if (thisYear == "17")
                    historicNameFix(uniqueName, thisYear, nextYear).replace("$thisYear", "_$nextYear")
                else
                    uniqueName.replace("$thisYear", "_$nextYear")
            else uniqueName

            var dataVersion = 1
            if (!advanceYear) {
                withPost {
                    dataLastGenerated = maxOf(dataVersion, dataLastGenerated) + 1
                    dataVersion = dataLastGenerated
                }
            }

            val daysAdvance = if (advanceYear) 364 else 0
            val dateStart = dateStart.addDays(daysAdvance)
            val dateEnd = dateEnd.addDays(daysAdvance)
            val dateOpens = if (advanceYear) dateStart.addDays(-120) else dateOpens
            val dateCloses = if (advanceYear) dateStart.addDays(-28) else dateCloses
            val kc2020Rules = dateStart.addDays(daysAdvance).after(kc2020RulesBaseDate)


            for (index in 0..campingBlocks - 1) {
                val block = getCampingBlock(index)
                block.start = block.start.addDays(daysAdvance)
                block.end = block.end.addDays(daysAdvance)
                setCampingBlock(index, block)
            }

            // create workbook
            path = Global.showDocumentPath(uniqueName, "show_template", "xls")
            val workbook = createWorkbook(path)
            val overview = workbook.headedSheet("Overview", 0, SHEET_SHOW_TEMPLATE, 1, "Show Template")
            overview.setWidths(0.5, 1.8, 3.0)

            // populate overview sheet
            var row = 2
            overview.addHeading(1, row++, "System Control Data")
            overview.addLabeledCell(0, row++, SHOW_ID, "Identifier", if (advanceYear) "" else id)
            overview.addLabeledCell(0, row++, UNIQUE_NAME, "Unique Name", uniqueName)
            overview.addLabeledCell(0, row++, DATE_GENERATED, "Date Generated", today)
            overview.addLabeledCell(0, row++, GENERATED_VERSION, "Version", dataVersion)
            overview.addLabeledCell(
                0, row++, SHOW_TYPE, "Show Type", when (idOrganization) {
                    ORGANIZATION_UKA -> "UKA"
                    ORGANIZATION_KC -> "KC"
                    ORGANIZATION_UK_OPEN -> "UK Open"
                    ORGANIZATION_FAB -> "FAB"
                    ORGANIZATION_INDEPENDENT -> "IND/${independentType}"
                    else -> "KC"
                }
            )
            overview.addLabeledCell(0, row++, SHOW_HIDDEN, "Hidden", hidden)
            overview.addLabeledCell(0, row++, SHOW_PROVISIONAL, "Provisional", provisional)

            overview.addHeading(1, row++, "Show Details")
            if (isKc) {
                overview.addLabeledCell(0, row++, ENTITY_KEY, "Club Key", entity.key)
            }
            overview.addLabeledCell(0, row++, SHOW_NAME, "Show Name", name)
            overview.addLabeledCell(0, row++, BRIEF_NAME, "Brief (Diary) Name", briefName)
            overview.addLabeledCell(0, row++, SHOW_MANAGER, "Manager", showManager)
            overview.addLabeledCell(0, row++, SHOW_MANAGER_EMAIL, "Manager Email", showManagerEmail)
            overview.addLabeledCell(0, row++, SHOW_MANAGER_PHONE, "Manager Phone", showManagerPhone)
            overview.addLabeledCell(0, row++, SHOW_SECRETARY, "Secretary", showSecretary)
            overview.addLabeledCell(0, row++, SHOW_SECRETARY_EMAIL, "Secretary Email", showSecretaryEmail)
            overview.addLabeledCell(0, row++, SHOW_SECRETARY_PHONE, "Secretary Phone", showSecretaryPhone)
            overview.addLabeledCell(0, row++, SHOW_SECRETARY_CODES, "Secretary Codes", showSecretaryCodes)

            if (isKc) {
                overview.addLabeledCell(0, row++, PLACE_RULE, "Place Rule", rosetteRule)
                overview.addLabeledCell(0, row++, TROPHY_RULE, "Trophy Rule", trophyRule)
                overview.addLabeledCell(0, row++, AWARD_RULE, "Award Rule", awardRule)
                if (!kc2020Rules) overview.addLabeledCell(0, row++, LH_ORDER, "LHO first/last/mixed", lhoOrder)
                overview.addLabeledCell(0, row++, MAX_RUNS, "Max Runs per Dog", maxRuns)
                overview.addLabeledCell(0, row++, ANYSIZE_RULE, "Any Size Rule", anySizeRule)
//            overview.addLabeledCell(0, row++, LHO_RULE, "LHO Rule", lhoRule)
                overview.addLabeledCell(0, row++, AGE_BASE_DATE, "Age Base Date", ageBaseDate)
            }
            overview.addLabeledCell(0, row++, SHOW_OPTIONS, "Options", options)
            if (isIndependent) {
                overview.addLabeledCell(0, row++, BONUS_CATEGORIES, "Bonus Categories", bonusCategories)
            }
            if (isKc) {
                overview.addHeading(1, row++, "Bank Account")
                overview.addLabeledCell(0, row++, BANK_ACCOUNT, "Account Name", bankAccountName)
                overview.addLabeledCell(0, row++, BANK_SORT, "Sort Code", bankAccountSort)
                overview.addLabeledCell(0, row++, BANK_NUMBER, "Account Number", bankAccountNumber)
            }
            overview.addHeading(1, row++, "Venue")
            overview.addLabeledCell(0, row++, VENUE_NAME, "Brief Name", venue)
            overview.addLabeledCell(0, row++, VENUE_ADDRESS, "Full Address", venueAddress)
            overview.addLabeledCell(0, row++, VENUE_POSTCODE, "Postcode", venuePostcode)
            overview.addHeading(1, row++, "Timeline")
            overview.addLabeledCell(0, row++, DATE_OPENS, "Entries Open", dateOpens)
            overview.addLabeledCell(0, row++, DATE_CLOSES, "Entries Close", dateCloses)
            overview.addLabeledCell(0, row++, DATE_START, "First Day", dateStart)
            overview.addLabeledCell(0, row++, DATE_END, "Last Day", dateEnd)
            overview.addHeading(1, row++, "Entries")
            overview.addLabeledCell(0, row++, CAPPING_LEVEL, "Capping Level", cappingLevel)
            if (isUka) {
                overview.addLabeledCell(0, row++, LATE_ENTRY_RESTRICTED, "Late Entry Allowed", !lateEntryRestricted)
                overview.addLabeledCell(0, row++, ENTRY_FEE, "Class Entry Fee", Money(entryFee))
                overview.addLabeledCell(0, row++, LATE_ENTRY_FEE, "Late Entry Fee", Money(lateEntryFee))
            } else if (isFab) {
                overview.addLabeledCell(0, row++, LATE_ENTRY_RESTRICTED, "Late Entry Allowed", !lateEntryRestricted)
                overview.addLabeledCell(0, row++, ENTRY_FEE, "Class Entry Fee", Money(entryFee))
                overview.addLabeledCell(0, row++, LATE_ENTRY_FEE, "Late Entry Fee", Money(lateEntryFee))
                overview.addLabeledCell(0, row++, IFCS_ENTRY_FEE, "IFCS Entry Fee", Money(ifcsFee))

                overview.addHeading(1, row++, "Processing")
                overview.addLabeledCell(0, row++, PROCESSING_FEE, "Processing Fee", Money(if (processingFee > 0) processingFee else 30))
                overview.addLabeledCell(0, row++, SWAP_FEE, "Processing Fee (SWAP)", Money(if (processingFeeSwap > 0) processingFeeSwap else 8))
            } else {
                overview.addLabeledCell(0, row++, ENTRY_FEE, "Class Fee", Money(entryFee))
                overview.addLabeledCell(0, row++, ENTRY_FEE_MEMBERS, "Class Fee (members)", Money(entryFeeMembers))
                overview.addLabeledCell(0, row++, MINIMUM_FEE, "Minimum Fee", Money(minimumFee))
                overview.addLabeledCell(0, row++, MINIMUM_FEE_MEMBERS, "Minimum Fee (members)", Money(minimumFeeMembers))
                overview.addLabeledCell(0, row++, MAXIMUM_FEE, "Maximum Fee", Money(maximumFee))
                overview.addLabeledCell(0, row++, MAXIMUM_FEE_MEMBERS, "Maximum Fee (members)", Money(maximumFeeMembers))
                overview.addHeading(1, row++, "Processing")
                overview.addLabeledCell(0, row++, ADMIN_FEE, "Paper Admin Fee", Money(if (adminFee > 0) adminFee else 200))
                overview.addLabeledCell(0, row++, PROCESSING_FEE, "Processing Fee", Money(if (processingFee > 0) processingFee else 30))
                overview.addLabeledCell(0, row++, SWAP_FEE, "Processing Fee (SWAP)", Money(if (processingFeeSwap > 0) processingFeeSwap else 8))
            }

            val block1 = getCampingBlock(0)

            overview.addHeading(1, row++, "Camping")
            overview.addLabeledCell(0, row++, CAMPING_PITCHES, "Total Pitches", campingPitches)
            overview.addLabeledCell(
                0,
                row++,
                CAMPING_CAP_SYSTEM,
                "Camping Cap System",
                campingCapToText(campingCapSystem)
            )
            overview.addLabeledCell(0, row++, CAMPING_MAIN_BLOCK, "Main Block", mainCampingBlock + 1)
            overview.addLabeledCell(0, row++, CAMPING_NOTE, "Note", block1.note, condition = hasCamping)

            val campingBlocks = if (isKc) 3 else 1

            for (index in 0..campingBlocks - 1) {
                val block = getCampingBlock(index)
                overview.addHeading(1, row++, "Camping - Block ${index + 1}")
                overview.addLabeledCell(
                    0,
                    row++,
                    CAMPING_START + index,
                    "First Night",
                    block.start,
                    condition = hasCamping
                )
                overview.addLabeledCell(
                    0,
                    row++,
                    CAMPING_END + index,
                    "Last Night",
                    block.end,
                    condition = hasCamping
                )
                overview.addLabeledCell(
                    0,
                    row++,
                    CAMPING_RATE_DAY + index,
                    "Day Fee",
                    Money(block.dayRate),
                    condition = hasCamping
                )
                overview.addLabeledCell(
                    0,
                    row++,
                    CAMPING_RATE + index,
                    "Block Fee",
                    Money(block.blockRate),
                    condition = hasCamping
                )
            }

            when (idOrganization) {
                ORGANIZATION_KC -> exportShowTemplateClassesKc(workbook, this, daysAdvance)
                ORGANIZATION_UKA, ORGANIZATION_UK_OPEN -> exportShowTemplateClassesUka(workbook, this, dateStart, dateEnd)
                ORGANIZATION_FAB -> exportShowTemplateClassesFab(workbook, this, dateStart, dateEnd)
                ORGANIZATION_INDEPENDENT -> exportShowTemplateClassesIndependent(workbook, this, daysAdvance)
            }

            workbook.quit()

        }


        return path

    }


    fun exportShowTemplateClassesUka(
        workbook: WritableWorkbook,
        competition: Competition,
        dateStart: Date,
        dateEnd: Date
    ) {

        val profile = ClassProfile(competition)
        val classes = workbook.headedSheet("Classes", 1, SHEET_UKA_CLASS_TEMPLATE, 1, "Class Template")
        classes.setWidths(0.5, 2.0)


        // populate classes sheet
        for (day in 0..dateEnd.daysSince(dateStart)) {
            classes.addCell(day + 2, 1, dateStart.addDays(day), WorkbookFormats.default.dayFormat)
        }
        var row = 2
        classes.addHeading(1, row++, "Regular Classes")
        for (template in ClassTemplate.members) {
            if (template.isUka && (template.canEnterDirectly || template.isHarvestedGroup) && !template.defunct) {
                if (template == ClassTemplate.MASTERS) {
                    classes.addHeading(1, row++, "Heats")
                } else if (template == ClassTemplate.FINAL_ROUND_1) {
                    classes.addHeading(1, row++, "Special Classes")
                }
                val label = template.nameTemplate.replace("<grade> ", "")
                classes.addCell(0, row, template.code)
                classes.addCell(1, row, label)

                for (day in 0..dateEnd.daysSince(dateStart)) {
                    val item = profile.getClasses(template.code, day)
                    if (item.classes > 0) {
                        if (template == ClassTemplate.CIRCULAR_KNOCKOUT) {
                            when (item.heightCodes) {
                                "UKA300;UKA650" -> classes.addCell(day + 2, row, "A")
                                "UKA400;UKA550" -> classes.addCell(day + 2, row, "B")
                                else -> classes.addCell(day + 2, row, item.classes)
                            }
                        } else {
                            classes.addCell(day + 2, row, item.classes)
                        }
                    }
                }
                row++
            }
        }
    }

    fun exportShowTemplateClassesFab(
        workbook: WritableWorkbook,
        competition: Competition,
        dateStart: Date,
        dateEnd: Date
    ) {

        val profile = ClassProfile(competition)
        val classes = workbook.headedSheet("Classes", 1, SHEET_FAB_CLASS_TEMPLATE, 1, "Class Template")
        classes.setWidths(0.5, 2.0)

        // populate classes sheet
        for (day in 0..dateEnd.daysSince(dateStart)) {
            classes.addCell(day + 2, 1, dateStart.addDays(day), WorkbookFormats.default.dayFormat)
        }
        var row = 2
        classes.addHeading(1, row++, "Regular Classes")
        var inIfcs = false
        var inFinals = false
        for (template in ClassTemplate.members) {
            if ((template.isFab || template.isIfcs) && (template.canEnterDirectly || template.isHarvestedGroup) && !template.defunct) {
                if (template.nameTemplate.toLowerCase().contains("final") && !inFinals) {
                    inFinals = true
                    classes.addHeading(1, row++, "Finals Classes")
                }
                if (template.isIfcs && !inIfcs) {
                    inIfcs = true
                    classes.addHeading(1, row++, "IFCS Classes")
                }
                val label = template.nameTemplate.replace("<grade> ", "")
                classes.addCell(0, row, template.code)
                classes.addCell(1, row, label)

                for (day in 0..dateEnd.daysSince(dateStart)) {
                    val item = profile.getClasses(template.code, day)
                    if (item.classes > 0) {
                        classes.addCell(day + 2, row, item.classes)
                    }
                }
                row++
            }
        }
    }

    fun exportShowTemplateClassesIndependent(workbook: WritableWorkbook, competition: Competition, daysAdvance: Int) {

        // populate classes sheet
        dbExecute("SET SESSION group_concat_max_len = 1000000")

        val agilityClass = AgilityClass()
        val orderBy = "classDate, classNumber, classNumberSuffix, heightCodes DESC, gradeCodes"
        agilityClass.select("idCompetition=${competition.id}", orderBy)

        var sheet = workbook.getSheet(0)

        var row = 2
        var column = 0
        var sheetNumber = 1

        var thisDate = competition.dateStart
        while (thisDate <= competition.dateEnd) {
            val actualDate = thisDate.addDays(daysAdvance)
            sheet =
                workbook.headedSheet(actualDate.format("EEE"), sheetNumber++, SHEET_INDEPENDENT_CLASS_TEMPLATE, 1, actualDate.shortText)
            row = 2
            column = 0
            sheet.addHeading(column++, row, "Class", 0.5)
            sheet.addHeading(column++, row, "Grades", 1.5)
            sheet.addHeading(column++, row, "C", 0.2)
            sheet.addHeading(column++, row, "Heights", 1.5)
            sheet.addHeading(column++, row, "+", 0.2)
            sheet.addHeading(column++, row, "Name", 3.0)
            sheet.addHeading(column++, row, "Rules", 1.0)
            sheet.addHeading(column, row, "Id", 1.0)
            row = 3
            column = 0
            thisDate = thisDate.addDays(1)
        }

        val classDate = ChangeMonitor(nullDate)

        while (agilityClass.next()) {
            if (classDate.hasChanged(agilityClass.date)) {
                sheetNumber = agilityClass.date.daysSince(competition.dateStart) + 1
                sheet = workbook.getSheet(sheetNumber)
                row = 3
                column = 0
            }


            val gradeMap = Grade.indGradesMap(competition.independentType)
            var gradeCodes = ""
            agilityClass.gradeCodes.replace(";", ",").split(",")
                .forEach { gradeCodes = gradeCodes.append(gradeMap.getDef(it, "")) }

            val heightMap = Height.indHeightsMap(competition.independentType)
            var heightCodes = ""
            agilityClass.heightCodes.replace(";", ",").split(",")
                .forEach { heightCodes = heightCodes.append(heightMap.getDef(it, "")) }

            sheet.addCell(column++, row, "${agilityClass.number}${agilityClass.numberSuffix}")
            sheet.addCell(column++, row, gradeCodes)
            sheet.addCell(column++, row, if (agilityClass.gradesCombined) "C" else "")
            sheet.addCell(column++, row, heightCodes)
            sheet.addCell(column++, row, if (agilityClass.heightsCombined) "+" else "")
            sheet.addCell(column++, row, agilityClass.nameLong.substringAfter(" "))
            sheet.addCell(column++, row, agilityClass.rules)
            sheet.addCell(column, row, if (daysAdvance > 0) -1 else agilityClass.id)
            row++
            column = 0
        }
    }

    fun exportShowTemplateClassesKc(workbook: WritableWorkbook, competition: Competition, daysAdvance: Int) {

        val newHeights = competition.dateStart.addDays(daysAdvance).after(Competition.kc2020RulesBaseDate)

        // populate classes sheet
        dbExecute("SET SESSION group_concat_max_len = 1000000")

        val agilityClass = AgilityClass()
//        val orderBy = "classNumber, classDate, classCode, block, heightCodes DESC, gradeCodes"
        val orderBy = "classDate, block, if(block>0, 0, classCode), heightCodes DESC, gradeCodes"
        if (daysAdvance > 0) {
            val query =
                DbQuery("SELECT GROUP_CONCAT(idAgilityClass) AS list FROM (SELECT idAgilityClass FROM agilityClass WHERE idCompetition = ${competition.id} GROUP BY classNumber) AS t1").toFirst()
            agilityClass.select("idAgilityClass IN (${query.getString("list")})", orderBy)
        } else {
            agilityClass.select("idCompetition=${competition.id}", orderBy)
        }

        var sheet = workbook.getSheet(0)

        var row = 2
        var column = 0
        var sheetNumber = 1

        var thisDate = competition.dateStart
        while (thisDate <= competition.dateEnd) {
            val actualDate = thisDate.addDays(daysAdvance)
            sheet =
                workbook.headedSheet(actualDate.format("EEE"), sheetNumber++, SHEET_KC_CLASS_TEMPLATE, 1, actualDate.shortText)
            row = 2
            column = 0
            sheet.addHeading(column++, row, "Class", 0.5)
            sheet.addHeading(column++, row, "Sub", 0.5)
            sheet.addHeading(column++, row, "Code", 0.5)
            sheet.addHeading(column++, row, "Bk", 0.3)
            sheet.addHeading(column++, row, "Category", 2.0)
            sheet.addHeading(column++, row, "L", 0.2)
            if (newHeights) {
                sheet.addHeading(column++, row, "I", 0.2)
            }
            sheet.addHeading(column++, row, "M", 0.2)
            sheet.addHeading(column++, row, "S", 0.2)
            sheet.addHeading(column++, row, "C", 0.2)

            sheet.addHeading(column++, row, "1", 0.2)
            sheet.addHeading(column++, row, "2", 0.2)
            sheet.addHeading(column++, row, "3", 0.2)
            sheet.addHeading(column++, row, "4", 0.2)
            sheet.addHeading(column++, row, "5", 0.2)
            sheet.addHeading(column++, row, "6", 0.2)
            sheet.addHeading(column++, row, "7", 0.2)

            if (!newHeights) {
                sheet.addHeading(column++, row, "F", 0.2)
                sheet.addHeading(column++, row, "L", 0.2)
            }
            sheet.addHeading(column++, row, "+", 0.2)
            sheet.addHeading(column++, row, "R", 0.2)
            sheet.addHeading(column++, row, "Q", 0.3)
            sheet.addHeading(column++, row, "Fee", 0.5)
            sheet.addHeading(column++, row, "Members", 0.8)

            sheet.addHeading(column++, row, "Prefix", 1.0)
            sheet.addHeading(column++, row, "Suffix", 1.0)
            sheet.addHeading(column++, row, "Sponsor", 1.0)
            sheet.addHeading(column++, row, "Short", 3.0)
            sheet.addHeading(column++, row, "Long", 3.0)
            sheet.addHeading(column++, row, "Judge", 2.0)
            sheet.addHeading(column++, row, "Rules", 1.0)
            sheet.addHeading(column, row, "Id", 1.0)
            row = 3
            column = 0
            thisDate = thisDate.addDays(1)
        }

        val classDate = ChangeMonitor(nullDate)

        while (agilityClass.next()) {
            if (classDate.hasChanged(agilityClass.date)) {
                sheetNumber = agilityClass.date.daysSince(competition.dateStart) + 1
                sheet = workbook.getSheet(sheetNumber)
                row = 3
                column = 0
            }

            val fullHeight = agilityClass.hasFullHeight
            val lowHeight = agilityClass.hasLowHeight
            val heightCombined = agilityClass.template.isAnySize || agilityClass.lhoCombined

            sheet.addCell(column++, row, agilityClass.number)
            sheet.addCell(column++, row, agilityClass.numberSuffix)
            sheet.addCell(column++, row, agilityClass.code)
            sheet.addCell(column++, row, agilityClass.block)
            sheet.addCell(column++, row, agilityClass.template.rawName)
            sheet.addCell(column++, row, if (agilityClass.heightCodes.contains("KC650")) "L" else "")
            if (newHeights) {
                sheet.addCell(column++, row, if (agilityClass.heightCodes.contains("KC500")) "I" else "")
            }
            sheet.addCell(column++, row, if (agilityClass.heightCodes.contains("KC450")) "M" else "")
            sheet.addCell(column++, row, if (agilityClass.heightCodes.contains("KC350")) "S" else "")
            sheet.addCell(column++, row, if (agilityClass.gradesCombined) "C" else "")

            sheet.addCell(column++, row, if (agilityClass.gradeCodes.contains("KC01")) "1" else "")
            sheet.addCell(column++, row, if (agilityClass.gradeCodes.contains("KC02")) "2" else "")
            sheet.addCell(column++, row, if (agilityClass.gradeCodes.contains("KC03")) "3" else "")
            sheet.addCell(column++, row, if (agilityClass.gradeCodes.contains("KC04")) "4" else "")
            sheet.addCell(column++, row, if (agilityClass.gradeCodes.contains("KC05")) "5" else "")
            sheet.addCell(column++, row, if (agilityClass.gradeCodes.contains("KC06")) "6" else "")
            sheet.addCell(column++, row, if (agilityClass.gradeCodes.contains("KC07")) "7" else "")

            if (!newHeights) {
                sheet.addCell(column++, row, if (fullHeight) "F" else "")
                sheet.addCell(column++, row, if (lowHeight) "L" else "")
            }
            sheet.addCell(column++, row, if (heightCombined) "+" else "")
            sheet.addCell(column++, row, agilityClass.runCount)
            sheet.addCell(column++, row, agilityClass.qCode)

            sheet.addCell(
                column++,
                row,
                Money(agilityClass.rawEntryFee),
                format = WorkbookFormats.default.zeroMoneyFormat
            )
            sheet.addCell(
                column++,
                row,
                Money(agilityClass.rawEntryFeeMembers),
                format = WorkbookFormats.default.zeroMoneyFormat
            )
            sheet.addCell(column++, row, agilityClass.prefix)
            sheet.addCell(column++, row, agilityClass.suffix)
            sheet.addCell(column++, row, agilityClass.sponsor)
            sheet.addCell(column++, row, agilityClass.describeClass(noPrefix = true, omitNumber = true))
            sheet.addCell(column++, row, agilityClass.describeClass(short = false, omitNumber = true))
            sheet.addCell(column++, row, agilityClass.judge)
            sheet.addCell(column++, row, agilityClass.rules)
            sheet.addCell(column, row, if (daysAdvance > 0) -1 else agilityClass.id)
            row++
            column = 0
        }
    }

    fun exportShowClasses(idCompetition: Int, rings: Int = MAX_RINGS): String {

        val competition = Competition(idCompetition)
        val agilityClass = AgilityClass()

        competition.withPost {
            ringPlanLastGenerated = maxOf(ringVersion, ringPlanLastGenerated) + 1
        }

        agilityClass.select(
            "idCompetition=$idCompetition",
            "classDate,ringNumber,if(ringNumber=0, judge, ''),ringOrder,classNumber,if(classCode=130, 3, classCode), block, gradeCodes"
        )

        val path = Global.showDocumentPath(competition.uniqueName, "ring_plan", "xls")
        val workbook = createWorkbook(path)

        // create overview sheet
        val overview = workbook.headedSheet("Overview", 0, SHEET_RING_PLAN_OVERVIEW_TEMPLATE, 1, "Ring Plan Template")
        overview.setWidths(0.5, 1.5, 3.0)
        var row = 2
        overview.addHeading(1, row++, "Ring Plan Details")
        overview.addLabeledCell(0, row++, SHOW_ID, "Identifier", competition.id)
        overview.addLabeledCell(0, row++, UNIQUE_NAME, "Unique Name", competition.uniqueName)
        overview.addLabeledCell(0, row++, DATE_GENERATED, "Date Generated", today)
        overview.addLabeledCell(0, row++, GENERATED_VERSION, "Version", competition.ringPlanLastGenerated)

        // create plan sheet for each day
        for (day in 0..competition.dateEnd.daysSince(competition.dateStart)) {
            val date = competition.dateStart.addDays(day)
            val sheet = workbook.headedSheet(
                date.fullDate(),
                day + 1,
                SHEET_RING_PLAN_DAY_TEMPLATE,
                1,
                "Ring Plan: ${date.dateText}"
            )

            sheet.addHeading(1, 2, "Unassigned")
            sheet.setWidths(0.5, 3.0)
            for (ringNumber in 1..rings) {
                sheet.addHeading(ringNumber + 1, 2, "Ring $ringNumber")
                sheet.addCell(ringNumber + 1, 3, "Judge: ")
                sheet.setColumnWidth(ringNumber + 1, 3.0)
            }
        }

        // populate day sheets
        val sheetIndex = ChangeMonitor(-1)
        val ringNumber = ChangeMonitor(-1)
        val currentJudge = ChangeMonitor("")
        var ringJudge = ""
        var note = ""
        var classRunningOrder = 0
        while (agilityClass.next()) {
            if (!agilityClass.hasChildren && !agilityClass.isHarvested && agilityClass.template != ClassTemplate.UKA_PRODUCT_SALE) {
                if (sheetIndex.hasChanged(agilityClass.date.daysSince(competition.dateStart) + 1)) {
                    ringNumber.value = -1
                }
                if (ringNumber.hasChanged(agilityClass.ringNumber)) {
                    Ring().where("idCompetition=$idCompetition AND date=${agilityClass.date.sqlDate} AND ringNumber=${ringNumber.value}") {
                        ringJudge = this.judge
                        note = this.note
                        currentJudge.value = "***"
                    }
                    row = 3
                    classRunningOrder = 0
                }
                val sheet = workbook.getSheet(sheetIndex.value)
                val classJudge = if (agilityClass.judge.isEmpty()) ringJudge else agilityClass.judge
                if (currentJudge.hasChanged(classJudge)) {
                    if (row > 3) row++
                    sheet.addLabel(agilityClass.ringNumber + 1, row++, "Judge: $classJudge")
                    if (note.isNotEmpty()) {
                        sheet.addLabel(agilityClass.ringNumber + 1, row++, "Note: $note")
                        note = ""
                    }
                    row++
                }
                if (classRunningOrder == 0 && (competition.isUka || competition.isFab || competition.isIndependent)) {
                    val heightOrderText = agilityClass.heightRunningOrderTextJump
                    sheet.addLabel(agilityClass.ringNumber + 1, row++, "Heights: $heightOrderText")
                    row++
                }
                sheet.addLabel(agilityClass.ringNumber + 1, row++, "${agilityClass.id}: ${agilityClass.name}")
                classRunningOrder++
            }
        }
        workbook.quit()
        return path
    }


    fun importRunningOrders(workbook: Workbook) {
        val map = HashMap<String, Int>()


        fun getInt(sheet: Sheet, row: Int, heading: String): Int {
            val column = map[heading]
            return if (column != null) sheet.getCell(column, row).asInt else -1
        }



        for (sheet in workbook.sheets) {
            val idAgilityClass = if (sheet.columns < 4) 0 else sheet.getCell(3, 0).asInt

            map.clear()
            for (column in 0..sheet.columns - 1) {
                var heading = sheet.getCell(column, 2).asString
                map[heading] = column
            }

            for (row in 3..sheet.rows - 1) {
                val idEntry = getInt(sheet, row, "Id")
                val moveTo = getInt(sheet, row, "Move To")
                if (moveTo > 0) {
                    Entry(idEntry).shiftRunningOrder(moveTo)
                }
            }

        }
    }


    fun exportRunningOrders(idCompetition: Int): String {
        val competition = Competition(idCompetition)
        val path = Global.showDocumentPath(competition.uniqueName, "running_orders", "xls")
        val workbook = createWorkbook(path)

        var sheetIndex = 0
        var row = 0
        var column = 0
        var sheet = workbook.createSheet("dummy", 0)
        workbook.removeSheet(0)
        val handlers = HashMap<String, Int>()

        var classList = ""
        dbQuery("SELECT DISTINCT idAgilityClass, classCode FROM entry JOIN agilityClass USING (idAgilityClass) WHERE idCompetition=$idCompetition AND classCode<>${ClassTemplate.TEAM.code} AND classProgress<$CLASS_WALKING") {
            if (competition.isUkOpen) {
                val template = ClassTemplate.select(getInt("classCode"))
                if (!template.hasChildren) {
                    classList = classList.append(getString("idAgilityClass"))
                }
            } else {
                classList = classList.append(getString("idAgilityClass"))
            }
        }

        val thisClass = ChangeMonitor<Int>(0)
        val thisHeightGroup = ChangeMonitor<String>("")
        val orderBy = when {
            competition.isUkOpen ->
                "classCode, entry.group, entry.jumpHeightCode, entry.runningOrder"
            else ->
                "agilityClass.classDate, agilityClass.ringOrder, classCode, entry.jumpHeightCode, entry.runningOrder"
        }

        Entry().join { agilityClass }.join { team }.join { team.dog }.join { team.competitor }
            .where("entry.idAgilityClass IN ($classList) AND progress<${PROGRESS_REMOVED}", orderBy) {
                if (thisClass.hasChanged(idAgilityClass)) {
                    sheet = workbook.headedSheet(
                        agilityClass.name,
                        sheetIndex++,
                        SHEET_RUNNING_ORDERS_TEMPLATE,
                        1,
                        agilityClass.name
                    )

                    sheet.addCell(3, 0, idAgilityClass)
                    row = 2
                    column = 0
                    if (agilityClass.groupRunningOrder.isNotEmpty()) {
                        sheet.addHeading(column++, row, "Group", 0.5)
                    }
                    sheet.addHeading(column++, row, "Height", 0.6)
                    sheet.addHeading(column++, row, "RO", 0.5)
                    when (agilityClass.template) {
                        ClassTemplate.SPLIT_PAIRS -> {
                            sheet.addHeading(column++, row, "Handler 1", 2.0)
                            sheet.addHeading(column++, row, "Dog 1", 1.0)
                            sheet.addHeading(column++, row, "Handler 2", 2.0)
                            sheet.addHeading(column++, row, "Dog 2", 1.0)
                        }
                        ClassTemplate.TEAM_RELAY -> {
                            sheet.addHeading(column++, row, "Team", 2.0)
                        }
                        ClassTemplate.TEAM_INDIVIDUAL -> {
                            sheet.addHeading(column++, row, "Handler", 2.0)
                            sheet.addHeading(column++, row, "Dog", 1.0)
                            sheet.addHeading(column++, row, "Team", 2.0)
                        }
                        else -> {
                            sheet.addHeading(column++, row, "Handler", 2.0)
                            sheet.addHeading(column++, row, "Dog", 1.0)
                        }
                    }
                    sheet.addHeading(column++, row, "Gap", 0.5)
                    sheet.addHeading(column++, row, "Id", 1.0)
                    sheet.addHeading(column++, row, "Move To", 1.0)
                    row++
                }

                column = 0
                var handler = competitorName

                if (thisHeightGroup.hasChanged(group + heightCode)) {
                    if (row > 3) row++
                    if (agilityClass.groupRunningOrder.isNotEmpty()) {
                        sheet.addCell(column++, row, group)
                    }
                    sheet.addCell(column++, row, Height.getHeightJumpName(heightCode))
                    handlers.clear()
                } else {
                    if (agilityClass.groupRunningOrder.isNotEmpty()) {
                        column += 2
                    } else {
                        column++
                    }
                }
                sheet.addCell(column++, row, runningOrder)
                when (agilityClass.template) {
                    ClassTemplate.SPLIT_PAIRS -> {
                        sheet.addCell(column++, row, team.getCompetitorName(1))
                        sheet.addCell(column++, row, team.getDogName(1))
                        sheet.addCell(column++, row, team.getCompetitorName(2))
                        sheet.addCell(column++, row, team.getDogName(2))
                    }
                    ClassTemplate.TEAM_RELAY -> {
                        handler = team.teamName
                        sheet.addCell(column++, row, team.teamName)
                    }
                    ClassTemplate.TEAM_INDIVIDUAL -> {
                        handler = team.getCompetitorName(teamMember)
                        sheet.addCell(column++, row, team.getCompetitorName(teamMember))
                        sheet.addCell(column++, row, team.getDogName(teamMember))
                        sheet.addCell(column++, row, team.teamName)
                    }
                    else -> {
                        sheet.addCell(column++, row, competitorName)
                        sheet.addCell(column++, row, dogName)
                    }
                }
                var gap = 0
                if (handlers.containsKey(handler)) {
                    gap = runningOrder - (handlers[handler] ?: runningOrder)
                }
                handlers[handler] = runningOrder
                if (gap > 0) sheet.addCell(column++, row, gap) else column++
                sheet.addCell(column++, row, id)
                row++
            }
        workbook.quit()

        return path

    }

    fun exportShowEntriesBlank(idCompetition: Int): String {
        val competition = Competition(idCompetition)
        val agilityClass = AgilityClass()
        agilityClass.competition.joinToParent()
        agilityClass.select("agilityClass.idCompetition=$idCompetition", "agilityClass.classCode")

        val path = Global.showDocumentPath(competition.uniqueName, "entries", "xls")
        val workbook = createWorkbook(path)

        var sheetIndex = 0
        var sheet = workbook.headedSheet("Overview", sheetIndex++, SHEET_ENTRIES_TEMPLATE, 1, "Show Entries")
        sheet.setWidths(0.5, 1.5, 3.0)
        var row: Int

        var idAgilityClass = -1



        while (agilityClass.next()) {
            if (agilityClass.template.entryRule != ENTRY_RULE_CLOSED) {
                sheet = workbook.headedSheet(
                    agilityClass.template.nameTemplate,
                    sheetIndex++,
                    SHEET_ENTRIES_TEMPLATE,
                    1,
                    "Show Entries"
                )
                sheet.addCell(2, 0, agilityClass.id)
                row = 2
                idAgilityClass = idAgilityClass
                when (agilityClass.template) {
                    ClassTemplate.SPLIT_PAIRS -> {
                        sheet.setWidths(1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 1.0, 1.0, 1.0, 2.0, 2.0, 1.0)
                        sheet.addHeading(0, row, "id")
                        sheet.addHeading(1, row, "1 Height")
                        sheet.addHeading(2, row, "1 Dog ID")
                        sheet.addHeading(3, row, "1 Dog")
                        sheet.addHeading(4, row, "1 Owner")
                        sheet.addHeading(5, row, "1 Handler")
                        sheet.addHeading(6, row, "2 Height")
                        sheet.addHeading(7, row, "2 Dog ID")
                        sheet.addHeading(8, row, "2 Dog")
                        sheet.addHeading(9, row, "2 Owner")
                        sheet.addHeading(10, row, "2 Handler")
                        sheet.addHeading(11, row, "Scratch")
                    }
                    ClassTemplate.TEAM -> {
                        sheet.setWidths(
                            1.0,
                            2.0,
                            1.0,
                            1.0,
                            1.0,
                            2.0,
                            2.0,
                            1.0,
                            1.0,
                            1.0,
                            2.0,
                            2.0,
                            1.0,
                            1.0,
                            1.0,
                            2.0,
                            2.0,
                            1.0
                        )
                        sheet.addHeading(0, row, "id")
                        sheet.addHeading(1, row, "Team")
                        sheet.addHeading(2, row, "1 Height")
                        sheet.addHeading(3, row, "1 Dog ID")
                        sheet.addHeading(4, row, "1 Dog")
                        sheet.addHeading(5, row, "1 Owner")
                        sheet.addHeading(6, row, "1 Handler")
                        sheet.addHeading(7, row, "2 Height")
                        sheet.addHeading(8, row, "2 Dog ID")
                        sheet.addHeading(9, row, "2 Dog")
                        sheet.addHeading(10, row, "2 Owner")
                        sheet.addHeading(11, row, "2 Handler")
                        sheet.addHeading(12, row, "3 Height")
                        sheet.addHeading(13, row, "3 Dog ID")
                        sheet.addHeading(14, row, "3 Dog")
                        sheet.addHeading(15, row, "3 Owner")
                        sheet.addHeading(16, row, "3 Handler")
                        sheet.addHeading(17, row, "Scratch")
                    }
                    else -> {
                        sheet.setWidths(1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 1.0)
                        sheet.addHeading(0, row, "id")
                        sheet.addHeading(1, row, "Height")
                        sheet.addHeading(2, row, "Dog ID")
                        sheet.addHeading(3, row, "Dog")
                        sheet.addHeading(4, row, "Owner")
                        sheet.addHeading(5, row, "Handler")
                        sheet.addHeading(6, row, "Scratch")
                    }
                }

            }
        }

        workbook.quit()

        return path
    }

    fun exportShowEntries(idCompetition: Int): String {
        val competition = Competition(idCompetition)
        val entry = Entry()
        entry.agilityClass.joinToParent()
        entry.team.joinToParent()
        entry.team.dog.joinToParent()
        entry.team.competitor.joinToParent()
        entry.select(
            "agilityClass.idCompetition=$idCompetition AND agilityClass.classCode<9000 AND entry.progress<$PROGRESS_REMOVED",
            "agilityClass.classDate, agilityClass.classCode, agilityClass.suffix, entry.jumpHeightCode, givenName, familyName"
        )

        val path = Global.showDocumentPath(competition.uniqueName, "entries", "xls")
        val workbook = createWorkbook(path)

        var sheetIndex = 0
        var sheet = workbook.headedSheet("Overview", sheetIndex++, SHEET_ENTRIES_TEMPLATE, 1, "Show Entries")
        sheet.setWidths(0.5, 1.5, 3.0)
        var row = 2

        var idAgilityClass = -1



        while (entry.next()) {
            if (entry.agilityClass.template.entryRule != ENTRY_RULE_CLOSED) {
                if (entry.idAgilityClass != idAgilityClass) {
                    sheet = workbook.headedSheet(
                        entry.agilityClass.template.nameTemplate,
                        sheetIndex++,
                        SHEET_ENTRIES_TEMPLATE,
                        1,
                        "Show Entries"
                    )
                    sheet.addCell(2, 0, entry.idAgilityClass)
                    row = 2
                    idAgilityClass = entry.idAgilityClass
                    when (entry.agilityClass.template) {
                        ClassTemplate.SPLIT_PAIRS -> {
                            sheet.setWidths(1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 1.0, 1.0, 1.0, 2.0, 2.0, 1.0)
                            sheet.addHeading(0, row, "id")
                            sheet.addHeading(1, row, "1 Height")
                            sheet.addHeading(2, row, "1 Dog ID")
                            sheet.addHeading(3, row, "1 Dog")
                            sheet.addHeading(4, row, "1 Owner")
                            sheet.addHeading(5, row, "1 Handler")
                            sheet.addHeading(6, row, "2 Height")
                            sheet.addHeading(7, row, "2 Dog ID")
                            sheet.addHeading(8, row, "2 Dog")
                            sheet.addHeading(9, row, "2 Owner")
                            sheet.addHeading(10, row, "2 Handler")
                            sheet.addHeading(11, row, "Scratch")
                        }
                        ClassTemplate.TEAM -> {
                            sheet.setWidths(
                                1.0,
                                2.0,
                                1.0,
                                1.0,
                                1.0,
                                2.0,
                                2.0,
                                1.0,
                                1.0,
                                1.0,
                                2.0,
                                2.0,
                                1.0,
                                1.0,
                                1.0,
                                2.0,
                                2.0,
                                1.0
                            )
                            sheet.addHeading(0, row, "id")
                            sheet.addHeading(1, row, "Team")
                            sheet.addHeading(2, row, "1 Height")
                            sheet.addHeading(3, row, "1 Dog ID")
                            sheet.addHeading(4, row, "1 Dog")
                            sheet.addHeading(5, row, "1 Owner")
                            sheet.addHeading(6, row, "1 Handler")
                            sheet.addHeading(7, row, "2 Height")
                            sheet.addHeading(8, row, "2 Dog ID")
                            sheet.addHeading(9, row, "2 Dog")
                            sheet.addHeading(10, row, "2 Owner")
                            sheet.addHeading(11, row, "2 Handler")
                            sheet.addHeading(12, row, "3 Height")
                            sheet.addHeading(13, row, "3 Dog ID")
                            sheet.addHeading(14, row, "3 Dog")
                            sheet.addHeading(15, row, "3 Owner")
                            sheet.addHeading(16, row, "3 Handler")
                            sheet.addHeading(17, row, "Scratch")
                        }
                        else -> {
                            sheet.setWidths(1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 1.0)
                            sheet.addHeading(0, row, "id")
                            sheet.addHeading(1, row, "Height")
                            sheet.addHeading(2, row, "Dog ID")
                            sheet.addHeading(3, row, "Dog")
                            sheet.addHeading(4, row, "Owner")
                            sheet.addHeading(5, row, "Handler")
                            sheet.addHeading(6, row, "Scratch")
                        }
                    }

                }
                row++
                when (entry.agilityClass.template) {
                    ClassTemplate.SPLIT_PAIRS -> {
                        sheet.addCell(0, row, entry.id)
                        sheet.addCell(1, row, entry.team.getHeightName(1))
                        sheet.addCell(2, row, entry.team.getDogCode(1))
                        sheet.addCell(3, row, entry.team.getPetName(1))
                        sheet.addCell(4, row, entry.team.getCompetitorName(1))
                        sheet.addCell(5, row, entry.team.getCompetitorName(1))
                        sheet.addCell(6, row, entry.team.getHeightName(2))
                        sheet.addCell(7, row, entry.team.getDogCode(2))
                        sheet.addCell(8, row, entry.team.getPetName(2))
                        sheet.addCell(9, row, entry.team.getCompetitorName(2))
                        sheet.addCell(10, row, entry.team.getCompetitorName(2))

                    }
                    ClassTemplate.TEAM -> {
                        sheet.addCell(0, row, entry.id)
                        sheet.addCell(1, row, entry.team.teamName)


                        sheet.addCell(2, row, entry.team.getHeightName(1))
                        sheet.addCell(3, row, entry.team.getDogCode(1))
                        sheet.addCell(4, row, entry.team.getPetName(1))
                        sheet.addCell(5, row, entry.team.getCompetitorName(1))
                        sheet.addCell(6, row, entry.team.getCompetitorName(1))
                        sheet.addCell(7, row, entry.team.getHeightName(2))
                        sheet.addCell(8, row, entry.team.getDogCode(2))
                        sheet.addCell(9, row, entry.team.getPetName(2))
                        sheet.addCell(10, row, entry.team.getCompetitorName(2))
                        sheet.addCell(11, row, entry.team.getCompetitorName(2))

                        sheet.addCell(12, row, entry.team.getHeightName(3))
                        sheet.addCell(13, row, entry.team.getDogCode(3))
                        sheet.addCell(14, row, entry.team.getPetName(3))
                        sheet.addCell(15, row, entry.team.getCompetitorName(3))
                        sheet.addCell(16, row, entry.team.getCompetitorName(3))
                    }
                    else -> {
                        sheet.addCell(0, row, entry.id)
                        sheet.addCell(1, row, entry.jumpHeightText)
                        sheet.addCell(2, row, entry.team.dog.idUka)
                        sheet.addCell(3, row, entry.team.dog.petName)
                        sheet.addCell(4, row, entry.team.competitor.fullName)
                        sheet.addCell(5, row, entry.team.competitorName)

                    }
                }
            }
        }

        workbook.quit()

        return path
    }

    fun importWorkbook(path: String, idCompetitionForce: Int = 0, force: Boolean = false): Int {
        val workbook = openWorkbook(path)
        when (workbook.identifier) {
            SHEET_RING_PLAN_OVERVIEW_TEMPLATE -> importShowClasses(workbook, idCompetitionForce)
            SHEET_SHOW_TEMPLATE -> importShowTemplate(workbook, force)
            SHEET_ENTRIES_TEMPLATE -> importShowEntries(workbook)
            SHEET_RUNNING_ORDERS_TEMPLATE -> importRunningOrders(workbook)
            SHEET_AWARD_TEMPLATE -> importPlacesSheet(workbook)
            SHEET_UKA_FINALS_QUALIFIED -> importUkaFinalsInvites(workbook)
            SHEET_UK_OPEN_GROUPS -> importUkOpenGroups(workbook)
            SHEET_CRUFTS_TEAMS_TEMPLATE -> importCruftsTeams(workbook)
        }
        return workbook.identifier
    }

    fun importShowClasses(workbook: Workbook, idCompetitionForce: Int = 0) {
        val agilityClass = AgilityClass()
        val ring = Ring()

        val overview = workbook.getSheet(0)
        val idCompetition = overview.getLabeledCell(0, SHOW_ID)?.asInt ?: -1
        mandate(idCompetition > 0, "Spreadsheet does not have a valid competition identifier")
        mandate(idCompetitionForce.oneOf(0, idCompetition), "Spreadsheet does not match this competition")

        dbExecute("UPDATE agilityClass SET RingNumber=0, RingOrder=0 WHERE idCompetition=$idCompetition")
        dbExecute("UPDATE ring SET flag=TRUE WHERE idCompetition=$idCompetition")

        val competition = Competition(idCompetition)
        val organizationData = competition.organizationData
        val version = overview.getLabeledCell(0, GENERATED_VERSION)?.asInt ?: -1
//        mandate(version > competition.ringVersion, "This spreadsheet (or a later one) has already been imported. Please re-generate and try again")

        competition.withPost {
            ringVersion = version
        }

        for (day in 0..competition.dateEnd.daysSince(competition.dateStart)) {
            val sheet = workbook.getSheet(day + 1)
            val sheetDate = competition.dateStart.addDays(day)
            mandate(sheet.identifier == SHEET_RING_PLAN_DAY_TEMPLATE, "Inconsistent ring plan sheet identifier")
            mandate(
                sheet.title.substringAfter(":").toDate("dd/MM/yyyy") == sheetDate,
                "Inconsistent ring plan sheet date"
            )

            for (column in 2..sheet.columns - 1) {
                var ringJudge = ""
                var ringManager = ""
                var note = ""
                var heightBase = ""
                var classJudge = ""
                var idAgilityClassFirst = 0
                var firstHeight = ""
                var firstGroup = ""
                var ringOrder = 1
                val ringNumber = column - 1

                for (row in 1..sheet.rows - 1) {
                    val cell = sheet.getCell(column, row).asString
                    val label = cell.substringBefore(":", "").toLowerCase().noSpaces
                    val data = cell.substringAfter(":", "")
                    when (label.toLowerCase()) {
                        "judge" -> {
                            classJudge = data.trim()
                            if (!ringJudge.contains(classJudge)) ringJudge = ringJudge.append(classJudge, " / ")
                        }
                        "ringmanager" -> {
                            ringManager = if (ringManager.isEmpty()) data else ringManager
                        }
                        "note" -> {
                            note = if (note.isEmpty()) data else note
                        }
                        "heights" -> {
                            if (heightBase.isEmpty()) {
                                val heights = data.replace(" ", "").split(",")
                                for (height in heights) {
                                    if (competition.isUka) {
                                        heightBase = heightBase.append(Height.getHeightCode(height, "UKA"), ",")
                                    }
                                    if (competition.isFab) {
                                        heightBase = heightBase.append(Height.getHeightCode(height, "FAB"), ",")
                                    }
                                    if (competition.isIndependent) {
                                        heightBase = heightBase.append(Height.getHeightCode(height, "IND", competition.independentType), ",")
                                    }
                                }
                            }
                        }
                        else -> {
                            val idAgilityClass = label.toIntDef(-1)
                            if (idAgilityClass > 0 && agilityClass.find(idAgilityClass)) {
                                if (idAgilityClassFirst == 0) {
                                    idAgilityClassFirst = idAgilityClass
                                    firstHeight = agilityClass.firstJumpHeightCode
                                    firstGroup = agilityClass.firstGroup
                                    agilityClass.startTime = agilityClass.date.addHours(8).addMinutes(30)
                                } else {
                                    agilityClass.startTime = nullDate
                                }
                                agilityClass.date = sheetDate
                                agilityClass.ringNumber = ringNumber
                                agilityClass.ringOrder = ringOrder++
                                agilityClass.judge = classJudge
                                agilityClass.post()
                            }
                        }
                    }
                }
                if (idAgilityClassFirst > 0) {
                    if (!ring.seek(idCompetition, sheetDate, ringNumber)) {
                        ring.append()
                        ring.idCompetition = idCompetition
                        ring.date = sheetDate
                        ring.number = ringNumber
                    }
                    ring.judge = ringJudge.trim()
                    ring.manager = ringManager.trim()
                    ring.note = note.trim()
                    ring.idAgilityClass = idAgilityClassFirst
                    ring.group = firstGroup
                    ring.heightCode = firstHeight
                    ring.heightBase = heightBase
                    ring.flag = false
                    ring.post()
                }
            }
        }

        if (competition.isUka) {
            generateUKAHeights(idCompetition)
        }
        if (competition.isFab) {
            generateFabHeights(idCompetition)
        }
        if (competition.isIndependent) {
            generateIndHeights(idCompetition)
        }

        ring.select("idCompetition=$idCompetition")
        while (ring.next()) {
            ring.heightCode = ring.agilityClass.firstJumpHeightCode
            ring.group = ring.agilityClass.firstGroup
        }
        competition.checkCardSet()
        dbExecute("DELETE FROM ring WHERE idCompetition=$idCompetition AND flag=TRUE")
    }

    private fun importShowTemplate(workbook: Workbook, force: Boolean = false) {
        dbTransaction {

            val competition = Competition()
            val overview = workbook.getSheet(0)
            val idCompetition = overview.getLabeledCell(0, SHOW_ID)?.asInt ?: -1
            val _uniqueName = (overview.getLabeledCell(0, UNIQUE_NAME)?.asString
                ?: "").replace("/", "_").replace(" ", "_")

            mandate(
                idCompetition > 0 || _uniqueName.isNotEmpty(),
                "Spreadsheet does not have a valid competition identifier"
            )
            if (idCompetition > 0) {
                mandate(competition.find(idCompetition), "Spreadsheet does not have a valid competition identifier")
            } else {
                if (!competition.find("uniqueName=${_uniqueName.quoted}")) {
                    competition.append()
                }
            }

            val version = overview.getLabeledCell(0, GENERATED_VERSION)?.asInt ?: -1

//            mandate(version > competition.dataVersion, "This spreadsheet (or a later one) has already been imported. Please re-generate and try again")

            var _dateOpens = overview.getLabeledCell(0, DATE_OPENS)?.asDate ?: nullDate
            var _dateCloses = overview.getLabeledCell(0, DATE_CLOSES)?.asDate ?: nullDate
            val _dateStart = overview.getLabeledCell(0, DATE_START)?.asDate ?: nullDate
            var _dateEnd = overview.getLabeledCell(0, DATE_END)?.asDate ?: nullDate
            val _campingPitches = overview.getLabeledCell(0, CAMPING_PITCHES)?.asInt ?: 0
            val _mainCampingBlock = (overview.getLabeledCell(0, CAMPING_MAIN_BLOCK)?.asInt ?: 1) - 1

            if (_dateEnd.isEmpty() && _dateStart.isNotEmpty()) {
                _dateEnd = _dateStart
            }
            if (_dateOpens.isEmpty() && _dateStart.isNotEmpty()) {
                _dateOpens = _dateStart.addDays(-120)
            }
            if (_dateCloses.isEmpty() && _dateStart.isNotEmpty()) {
                _dateCloses = _dateStart.addDays(-28)
            }

            val showType = (overview.getLabeledCell(0, SHOW_TYPE)?.asString ?: "").substringBefore("/")
            val independentType = (overview.getLabeledCell(0, SHOW_TYPE)?.asString ?: "").substringAfter("/", "")


            with(competition) {
                idOrganization = when (showType) {
                    "UKA" -> ORGANIZATION_UKA
                    "KC" -> ORGANIZATION_KC
                    "UK Open" -> ORGANIZATION_UK_OPEN
                    "FAB" -> ORGANIZATION_FAB
                    "IND" -> ORGANIZATION_INDEPENDENT
                    else -> ORGANIZATION_KC
                }
                this.independentType = independentType
                val entityKey = overview.getLabeledCell(0, ENTITY_KEY)?.asString ?: ""
                if (entityKey.isNotEmpty()) {
                    idEntity = Entity.keyToId(entityKey, default = idEntity)
                    Entity().seek(idEntity) {
                        idProcessor = PROCESSOR_PLAZA
                    }
                }
                name = overview.getLabeledCell(0, SHOW_NAME)?.asString ?: ""
                briefName = overview.getLabeledCell(0, BRIEF_NAME)?.asString ?: ""
                uniqueName = _uniqueName
                showManager = overview.getLabeledCell(0, SHOW_MANAGER)?.asString ?: ""
                showManagerEmail = overview.getLabeledCell(0, SHOW_MANAGER_EMAIL)?.asString ?: ""
                showManagerPhone = overview.getLabeledCell(0, SHOW_MANAGER_PHONE)?.asString ?: ""
                showSecretary = overview.getLabeledCell(0, SHOW_SECRETARY)?.asString ?: ""
                showSecretaryEmail = overview.getLabeledCell(0, SHOW_SECRETARY_EMAIL)?.asString ?: ""
                showSecretaryPhone = overview.getLabeledCell(0, SHOW_SECRETARY_PHONE)?.asString ?: ""
                showSecretaryCodes = overview.getLabeledCell(0, SHOW_SECRETARY_CODES)?.asString?.noSpaces ?: ""
                venue = overview.getLabeledCell(0, VENUE_NAME)?.asString ?: ""
                venueAddress = overview.getLabeledCell(0, VENUE_ADDRESS)?.asString ?: ""
                venuePostcode = overview.getLabeledCell(0, VENUE_POSTCODE)?.asString ?: ""
                if (!venueAddress.contains(venuePostcode)) venueAddress = venueAddress.append(venuePostcode)
                dateOpens = _dateOpens
                dateCloses = _dateCloses
                dateStart = _dateStart
                dateEnd = _dateEnd
                cappingLevel = overview.getLabeledCell(0, CAPPING_LEVEL)?.asInt ?: 0
                lateEntryRestricted = !(overview.getLabeledCell(0, LATE_ENTRY_RESTRICTED)?.asBoolean
                    ?: true)
                entryFee = overview.getLabeledCell(0, ENTRY_FEE)?.asDouble?.pence ?: 0
                adminFee = overview.getLabeledCell(0, ADMIN_FEE)?.asDouble?.pence ?: 0
                entryFeeMembers = overview.getLabeledCell(0, ENTRY_FEE_MEMBERS)?.asDouble?.pence ?: 0
                minimumFee = overview.getLabeledCell(0, MINIMUM_FEE)?.asDouble?.pence ?: 0
                minimumFeeMembers = overview.getLabeledCell(0, MINIMUM_FEE_MEMBERS)?.asDouble?.pence ?: 0
                maximumFee = overview.getLabeledCell(0, MAXIMUM_FEE)?.asDouble?.pence ?: 0
                maximumFeeMembers = overview.getLabeledCell(0, MAXIMUM_FEE_MEMBERS)?.asDouble?.pence ?: 0
                processingFee = overview.getLabeledCell(0, PROCESSING_FEE)?.asDouble?.pence ?: 0
                processingFeeSwap = overview.getLabeledCell(0, SWAP_FEE)?.asDouble?.pence ?: 0
                lateEntryFee = overview.getLabeledCell(0, LATE_ENTRY_FEE)?.asDouble?.pence ?: 0
                ifcsFee = overview.getLabeledCell(0, IFCS_ENTRY_FEE)?.asDouble?.pence ?: 0
                campingPitches = _campingPitches
                campingCapSystem = textToCampingCap(
                    overview.getLabeledCell(0, CAMPING_CAP_SYSTEM)?.asString
                        ?: ""
                )
                mainCampingBlock = _mainCampingBlock
                if (idOrganization == ORGANIZATION_UKA && uniqueName.startsWith("GrandFinals")) {
                    grandFinals = true
                    logo = "UKA Grand Final.jpg"
                }

                if (_campingPitches == 0) {
                    camping.clear()
                } else {
                    for (index in 0..2) {
                        if (overview.getLabeledCell(0, CAMPING_START + index) != null) {
                            var start = overview.getLabeledCell(0, CAMPING_START + index)?.asDate ?: nullDate
                            var end = overview.getLabeledCell(0, CAMPING_END + index)?.asDate ?: nullDate
                            val dayRate = overview.getLabeledCell(0, CAMPING_RATE_DAY + index)?.asDouble?.pence ?: 0
                            val blockRate = overview.getLabeledCell(0, CAMPING_RATE + index)?.asDouble?.pence ?: 0
                            val note = overview.getLabeledCell(0, CAMPING_NOTE + index)?.asString ?: ""
                            if (index == 0 && start.isEmpty() && _dateStart.isNotEmpty()) {
                                start = _dateStart.addDays(-1)
                            }
                            if (index == 0 && end.isEmpty() && _dateEnd.isNotEmpty()) {
                                end = _dateEnd.addDays(-1)
                            }
                            if (start.isNotEmpty()) {
                                setCampingBlock(
                                    index,
                                    CampingBlock(competition, start, end, dayRate > 0, blockRate, dayRate, note)
                                )
                            }
                        }
                    }
                }

                hidden = overview.getLabeledCell(0, SHOW_HIDDEN)?.asBoolean ?: false
                provisional = overview.getLabeledCell(0, SHOW_PROVISIONAL)?.asBoolean ?: false
                dataVersion = version

                rosetteRule = overview.getLabeledCell(0, PLACE_RULE)?.asString ?: ""
                trophyRule = overview.getLabeledCell(0, TROPHY_RULE)?.asString ?: ""
                awardRule = overview.getLabeledCell(0, AWARD_RULE)?.asString ?: ""
                lhoOrder = (overview.getLabeledCell(0, LH_ORDER)?.asString?.toLowerCase() ?: "last")
                maxRuns = overview.getLabeledCell(0, MAX_RUNS)?.asInt ?: 0
                anySizeRule = overview.getLabeledCell(0, ANYSIZE_RULE)?.asString?.noSpaces ?: ""
//            lhoRule = overview.getLabeledCell(0, LHO_RULE)?.asString?.noSpaces ?: ""
                ageBaseDate = overview.getLabeledCell(0, AGE_BASE_DATE)?.asDate ?: dateStart
                options = overview.getLabeledCell(0, SHOW_OPTIONS)?.asString ?: ""
                bonusCategories = overview.getLabeledCell(0, BONUS_CATEGORIES)?.asString ?: ""

                bankAccountName = overview.getLabeledCell(0, BANK_ACCOUNT)?.asString ?: ""
                bankAccountSort = overview.getLabeledCell(0, BANK_SORT)?.asString ?: ""
                bankAccountNumber = overview.getLabeledCell(0, BANK_NUMBER)?.asString ?: ""

                if (venue.isEmpty()) {
                    venue = venueAddress
                }

                post() // to get idCompetition
                when (idOrganization) {
                    ORGANIZATION_KC -> importShowTemplateClassesKc(workbook, this)
                    ORGANIZATION_UKA -> if (!hasEntries || force) importShowTemplateClassesUka(workbook, this)
                    ORGANIZATION_UK_OPEN -> UkOpenUtils.setUpClasses(idCompetition)
                    ORGANIZATION_FAB -> if (!hasEntries || force) importShowTemplateClassesFab(workbook, this)
                    ORGANIZATION_INDEPENDENT -> importShowTemplateClassesIndependent(workbook, this)
                }
                post()
                if (competition.cappingLevel > 0) {
                    competition.checkCap()
                }
                tidy()
            }
        }
    }

    private fun importShowTemplateClassesUka(workbook: Workbook, competition: Competition) {
        val profile = ClassProfile(competition)
        val classes = workbook.getSheet(1)
        mandate(classes.identifier == SHEET_UKA_CLASS_TEMPLATE, "Inconsistent class template identifier")

        for (day in 0..competition.dateEnd.daysSince(competition.dateStart)) {
            val date = competition.dateStart.addDays(day)
            val column = day + 2
            for (row in 3..classes.rows - 1) {
                val classCode = classes.getCell(0, row).asInt
                val classLetter = classes.getCell(column, row).asString.toUpperCase()
                val classCount = if (classLetter.oneOf("A", "B")) 1 else classes.getCell(column, row).asInt
                if (classCode > 0 && classCount > 0) {
                    val item = profile.getClasses(classCode, day)
                    profile.dropClasses(classCode, day)
                    if (item.classes != classCount) {
                        UkaAdmin.adjustClassCountUka(competition.id, classCode, date, item.classes, classCount)
                    }
                    if (classCode == ClassTemplate.CIRCULAR_KNOCKOUT.code) {
                        val agilityClass =
                            AgilityClass.select("idCompetition=${competition.id} AND classCode=$classCode AND classDate=${date.sqlDate}")
                        if (agilityClass.first()) {
                            when (classLetter) {
                                "A" -> agilityClass.heightCodes = "UKA300;UKA650"
                                "B" -> agilityClass.heightCodes = "UKA400;UKA550"
                                else -> agilityClass.heightCodes = "UKA300;UKA400;UKA550;UKA650"
                            }
                        }
                        agilityClass.jumpHeightCodes = agilityClass.heightCodes.replace(";", ",")
                        agilityClass.post()
                    }
                }
            }
        }

        profile.map.forEach { pair, item ->
            val classCode = pair.first
            val day = pair.second
            val date = competition.dateStart.addDays(day)
            val template = ClassTemplate.select(classCode)
            if (template.isUka && (template.canEnterDirectly || template.isHarvestedGroup)) {
                UkaAdmin.adjustClassCountUka(competition.id, classCode, date, item.classes, 0)
            }
        }

        UkaAdmin.checkHarvested(competition.id)
    }

    private fun importShowTemplateClassesFab(workbook: Workbook, competition: Competition) {
        val profile = ClassProfile(competition)
        val classes = workbook.getSheet(1)
        mandate(classes.identifier == SHEET_FAB_CLASS_TEMPLATE, "Inconsistent class template identifier")

        for (day in 0..competition.dateEnd.daysSince(competition.dateStart)) {
            val date = competition.dateStart.addDays(day)
            val column = day + 2
            for (row in 3..classes.rows - 1) {
                val classCode = classes.getCell(0, row).asInt
                val classCount = classes.getCell(column, row).asInt
                if (classCode > 0 && classCount > 0) {
                    val item = profile.getClasses(classCode, day)
                    profile.dropClasses(classCode, day)
                    if (item.classes != classCount) {
                        FabAdmin.adjustClassCountFab(competition.id, classCode, date, item.classes, classCount)
                    }
                }
            }
        }

        profile.map.forEach { pair, item ->
            val classCode = pair.first
            val day = pair.second
            val date = competition.dateStart.addDays(day)
            FabAdmin.adjustClassCountFab(competition.id, classCode, date, item.classes, 0)
        }
    }

    private fun importShowTemplateClassesIndependent(workbook: Workbook, competition: Competition) {
        val map = HashMap<String, Int>()

        fun getString(sheet: Sheet, row: Int, heading: String): String {
            val column = map[heading]
            return if (column != null) sheet.getCell(column, row).asString else ""
        }

        fun getInt(sheet: Sheet, row: Int, heading: String): Int {
            val column = map[heading]
            return if (column != null) sheet.getCell(column, row).asInt else -1
        }

        fun getBoolean(sheet: Sheet, row: Int, heading: String): Boolean {
            val item = getString(sheet, row, heading)
            if (item.isEmpty()) {
                return false
            }
            return item[0].toString().oneOf(heading[0].toString(), "x", "X")
        }

        dbExecute("UPDATE agilityClass SET flag=TRUE WHERE idCompetition=${competition.id}")

        var day = 0
        for (sheet in workbook.sheets) {
            if (sheet.identifier == SHEET_INDEPENDENT_CLASS_TEMPLATE) {

                map.clear()
                for (column in 0..sheet.columns - 1) {
                    var heading = sheet.getCell(column, 2).asString
                    if (heading == "L" && map.containsKey("L")) heading = "Lh"
                    map[heading] = column
                }

                val sheetDate = competition.dateStart.addDays(day)
                day++
                for (row in 3..sheet.rows - 1) {
                    if (sheet.getCell(0, row).asString.isNotEmpty()) {
                        val idAgilityClass = getInt(sheet, row, "Id")

                        var classNumber = 0
                        var classNumberSuffix = ""
                        getString(sheet, row, "Class").forEach {
                            when (it) {
                                in '0'..'9' -> classNumber = classNumber * 10 + it.toString().toInt()
                                else -> classNumberSuffix += it.toUpperCase()
                            }
                        }


                        val gradeCodesRaw = getString(sheet, row, "Grades")
                        val gradeDelimiter = if (getBoolean(sheet, row, "C")) "," else ";"
                        val heightCodesRaw = getString(sheet, row, "Heights")
                        val heightDelimiter = if (getBoolean(sheet, row, "+")) "," else ";"
                        val name = getString(sheet, row, "Name")
                        val rules = getString(sheet, row, "Rules").noSpaces

                        val template = IND_MISC

                        var gradeCodes = ""
                        val gradeMap = Grade.indGradesReverseMap(competition.independentType)
                        gradeCodesRaw.split(",")
                            .forEach {
                                gradeCodes = gradeCodes.append(gradeMap.getOrDefault(it.trim(), ""), ",")
                            }

                        var heightCodes = ""
                        val heightMap = Height.indHeightsReverseMap(competition.independentType)
                        heightCodesRaw.split(",")
                            .forEach {
                                heightCodes = heightCodes.append(heightMap.getOrDefault(it.trim(), ""), ",")
                            }

                        IndUtils.updateClass(
                            idAgilityClass = idAgilityClass,
                            competition = competition,
                            classDate = sheetDate,
                            classNumber = classNumber,
                            classNumberSuffix = classNumberSuffix,
                            template = template,
                            heightCodes = heightCodes.replace(",", heightDelimiter),
                            gradeCodes = gradeCodes.replace(",", gradeDelimiter),
                            name = "$classNumber$classNumberSuffix $name",
                            rules = rules
                        )
                    }
                }
            }
        }
        if (!competition.hasEntries) {
            dbExecute("DELETE FROM agilityClass WHERE idCompetition=${competition.id} AND flag=TRUE")
        }


    }

    private fun importShowTemplateClassesKc(workbook: Workbook, competition: Competition) {

        val newHeights = competition.kc2020Rules

        val map = HashMap<String, Int>()

        fun getString(sheet: Sheet, row: Int, heading: String): String {
            val column = map[heading]
            return if (column != null) sheet.getCell(column, row).asString else ""
        }

        fun getInt(sheet: Sheet, row: Int, heading: String): Int {
            val column = map[heading]
            return if (column != null) sheet.getCell(column, row).asInt else -1
        }

        fun getDouble(sheet: Sheet, row: Int, heading: String): Double {
            val column = map[heading]
            return if (column != null) sheet.getCell(column, row).asDouble else -1.0
        }

        fun getBoolean(sheet: Sheet, row: Int, heading: String): Boolean {
            val item = getString(sheet, row, heading)
            if (item.isEmpty()) {
                return false
            }
            return item[0].toString().oneOf(heading[0].toString(), "x", "X")
        }

        data class Split(val number: Int, val text: String)

        fun getSplit(sheet: Sheet, row: Int, heading: String): Split {
            val column = map[heading]
            if (column != null) {
                val cell = sheet.getCell(column, row)
                if (cell.type == CellType.NUMBER) {
                    return Split(cell.asInt, "")
                } else {
                    var digits = ""
                    var letters = ""
                    for (c in cell.asString) {
                        if (c in '0'..'9') {
                            digits += c
                        } else {
                            letters += c
                        }
                    }
                    return Split(digits.toIntDef(-1), letters)
                }
            } else {
                return Split(-1, "")
            }
        }

        dbExecute("UPDATE agilityClass SET flag=TRUE WHERE idCompetition=${competition.id}")

        var day = 0
        for (sheet in workbook.sheets) {
            if (sheet.identifier == SHEET_KC_CLASS_TEMPLATE) {
                val sheetDate = competition.dateStart.addDays(day)
                day++
                map.clear()
                for (column in 0..sheet.columns - 1) {
                    var heading = sheet.getCell(column, 2).asString
                    if (heading == "L" && map.containsKey("L")) heading = "Lh"
                    map[heading] = column
                }
                for (row in 3..sheet.rows - 1) {
                    if (sheet.getCell(0, row).asString.isNotEmpty()) {
                        val idAgilityClass = getInt(sheet, row, "Id")
                        val classNumber = getInt(sheet, row, "Class")
                        val classNumberSuffix = getString(sheet, row, "Sub")
                        val classCode = getInt(sheet, row, "Code")
                        val block = getInt(sheet, row, "Bk")
                        var heightCodes = ""
                        if (getBoolean(sheet, row, "S")) heightCodes = heightCodes.commaAppend("KC350")
                        if (getBoolean(sheet, row, "M")) heightCodes = heightCodes.commaAppend("KC450")
                        if (newHeights) {
                            if (getBoolean(sheet, row, "I")) heightCodes = heightCodes.commaAppend("KC500")
                        }
                        if (getBoolean(sheet, row, "L")) heightCodes = heightCodes.commaAppend("KC650")
                        val delimiter = if (getBoolean(sheet, row, "C")) "," else ";"
                        var gradeCodes = ""
                        if (getBoolean(sheet, row, "1")) gradeCodes = gradeCodes.append("KC01", delimiter)
                        if (getBoolean(sheet, row, "2")) gradeCodes = gradeCodes.append("KC02", delimiter)
                        if (getBoolean(sheet, row, "3")) gradeCodes = gradeCodes.append("KC03", delimiter)
                        if (getBoolean(sheet, row, "4")) gradeCodes = gradeCodes.append("KC04", delimiter)
                        if (getBoolean(sheet, row, "5")) gradeCodes = gradeCodes.append("KC05", delimiter)
                        if (getBoolean(sheet, row, "6")) gradeCodes = gradeCodes.append("KC06", delimiter)
                        if (getBoolean(sheet, row, "7")) gradeCodes = gradeCodes.append("KC07", delimiter)
                        val fullHeight = if (newHeights) true else getBoolean(sheet, row, "F")
                        val lowHeight = if (newHeights) false else getBoolean(sheet, row, "Lh")
                        var jumpDelimiter = if (getBoolean(sheet, row, "+")) "," else ";"
                        val runCount = getInt(sheet, row, "R")
                        val qCode = getString(sheet, row, "Q")
                        val entryFee = (getDouble(sheet, row, "Fee") * 100.0).toInt()
                        val entryFeeMembers = (getDouble(sheet, row, "Members") * 100.0).toInt()
                        val prefix = getString(sheet, row, "Prefix")
                        val suffix = getString(sheet, row, "Suffix")
                        val sponsor = getString(sheet, row, "Sponsor")
                        val judge = getString(sheet, row, "Judge").replace("  ", " ").trim()
                        val rules = getString(sheet, row, "Rules").noSpaces

                        val template = ClassTemplate.select(classCode)

                        var heightOptions = ""


                        if (template.isAnySize || template.isVeteran) {
                            heightOptions =
                                if (competition.anySizeRule.isNotEmpty()) competition.anySizeRule else "KC350:X999,KC450:X999,KC500:X999,KC650:X999"
                            jumpDelimiter = ","
                        } else {
                            for (heightCode in heightCodes.split(",")) {
                                var options = ""
                                if (fullHeight || (!fullHeight && !lowHeight)) options = heightCode + "_B"
                                if (lowHeight) {
                                    options = options.append(heightCode + "_A", "|")
                                }
                                heightOptions = heightOptions.commaAppend("$heightCode:$options")
                            }
                        }

                        if (rules.split(",").contains("H1")) {
                            heightOptions = "KC350:KC350_B,KC450:KC450_B,KC650:KC650_B|KC650_A"
                        }

                        var jumpHeightCodes = ""
                        var heightRunningOrder = ""
                        val jumpHeights = ArrayList<String>()
                        for (height in heightOptions.split(",")) {
                            val options = height.substringAfter(":").split("|")
                            for (option in options) {
                                if (!jumpHeights.contains(option)) {
                                    jumpHeights.add(option)
                                }
                            }
                        }

                        var lowerFirst = ""
                        var lowerLast = ""

                        Collections.sort(jumpHeights) { a, b -> b.compareTo(a) }
                        jumpHeights.forEach { jumpHeightCodes = jumpHeightCodes.append(it, jumpDelimiter) }
                        jumpHeights.forEach { lowerLast = lowerLast.commaAppend(it) }

                        Collections.sort(jumpHeights) { a, b -> a.compareTo(b) }
                        jumpHeights.forEach { lowerFirst = lowerFirst.commaAppend(it) }

                        when (competition.lhoOrder) {
                            "first" -> heightRunningOrder = lowerFirst
                            "last" -> heightRunningOrder = lowerLast
                            else -> {
                                if (heightRunningOrder != lowerFirst && heightRunningOrder != lowerLast) {
                                    heightRunningOrder = lowerLast
                                }
                            }
                        }


                        KcUtils.updateClass(
                            idAgilityClass = idAgilityClass,
                            competition = competition,
                            classDate = sheetDate,
                            classNumber = classNumber,
                            classNumberSuffix = classNumberSuffix,
                            template = template,
                            block = block,
                            heightCodes = heightCodes,
                            gradeCodes = gradeCodes,
                            entryFee = entryFee,
                            entryFeeMembers = entryFeeMembers,
                            prefix = prefix,
                            suffix = suffix,
                            sponsor = sponsor,
                            judge = judge,
                            heightOptions = heightOptions.replace("_A", "L").replace("_B", ""),
                            jumpHeightCodes = jumpHeightCodes.replace("_A", "L").replace("_B", ""),
                            heightRunningOrder = heightRunningOrder.replace("_A", "L").replace("_B", ""),
                            runCount = runCount,
                            qCode = qCode,
                            rules = rules,
                            forceSponsor = competition.forceSponsor
                        )
                    }
                }
            }
        }
        if (!competition.hasEntries) {
            dbExecute("DELETE FROM agilityClass WHERE idCompetition=${competition.id} AND flag=TRUE")
        }

    }

    fun importShowEntries(workbook: Workbook) {
        val agilityClass = AgilityClass()
        val entry = Entry()
        entry.agilityClass.joinToParent()
        entry.team.joinToParent()
        entry.team.dog.joinToParent()
        entry.team.competitor.joinToParent()

        var idCompetition = 0

        for (sheet in workbook.sheets) {
            val idAgilityClass = if (sheet.columns < 3) 0 else sheet.getCell(2, 0).asInt
            if (sheet.identifier == SHEET_ENTRIES_TEMPLATE && idAgilityClass > 0) {
                agilityClass.find(idAgilityClass)
                if (idCompetition == 0) idCompetition = agilityClass.idCompetition
                for (row in 3..sheet.rows - 1) {
                    if (sheet.getCell(1, row).asString.isNotEmpty()) {
                        when (agilityClass.template) {
                            ClassTemplate.SPLIT_PAIRS -> {
                                val id = sheet.getCell(0, row).asInt
                                val jumpHeightText = sheet.getCell(1, row).asString
                                val idUka = sheet.getCell(2, row).asInt
                                val competitorName = sheet.getCell(5, row).asString
                                val jumpHeightText2 = sheet.getCell(6, row).asString
                                val idUka2 = sheet.getCell(7, row).asInt
                                val competitorName2 = sheet.getCell(10, row).asString
                                val scratched = sheet.getCell(11, row).asString.isNotBlank()
                                if (id > 0) {
                                    entry.find(id)
                                    val team = Team(entry.idTeam)
                                    val idDog1 = Dog.getIdFromUka(idUka)
                                    val idDog2 = Dog.getIdFromUka(idUka2)
                                    val idCompetitor1 = Dog.getIdCompetitor(idDog1)
                                    val idCompetitor2 = Dog.getIdCompetitor(idDog2)
                                    val idAccount = Competitor(idCompetitor1).getAccountID()
                                    team.selectUkaPair(
                                        idAccount,
                                        agilityClass.id,
                                        idDog1,
                                        idDog1,
                                        idDog2,
                                        idCompetitor1,
                                        idCompetitor2,
                                        Height.getHeightCode(jumpHeightText, "UKA"),
                                        Height.getHeightCode(jumpHeightText2, "UKA")
                                    )
                                    if (team.id != entry.idTeam) {
                                        println("${agilityClass.name}: ${entry.teamDescription} composition changed")
                                        entry.idTeam = team.id
                                    }
                                    if (entry.jumpHeightCode != team.relayHeightCode) {
                                        println(
                                            "${agilityClass.name}: ${entry.teamDescription} height to ${Height.getHeightName(
                                                team.relayHeightCode
                                            )}"
                                        )
                                        entry.jumpHeightCode = team.relayHeightCode
                                        entry.heightCode = team.relayHeightCode
                                    }
                                    if (competitorName != team.getCompetitorName(1)) {
                                        println("${agilityClass.name}: ${entry.teamDescription} handler 1 to $competitorName")
                                        team.member(1)["competitorName"] = competitorName
                                    }
                                    if (competitorName2 != team.getCompetitorName(2)) {
                                        println("${agilityClass.name}: ${entry.teamDescription} handler 2 to $competitorName2")
                                        team.member(2)["competitorName"] = competitorName2
                                    }
                                    if (scratched && entry.progress != PROGRESS_REMOVED) {
                                        println("${agilityClass.name}: ${entry.teamDescription} SCRATCHED")
                                        entry.progress = PROGRESS_REMOVED
                                    }
                                    team.post()
                                    entry.post()
                                } else {
                                    val team = Team()
                                    val idDog1 = Dog.getIdFromUka(idUka)
                                    val idDog2 = Dog.getIdFromUka(idUka2)
                                    val idCompetitor1 = Dog.getIdCompetitor(idDog1)
                                    val idCompetitor2 = Dog.getIdCompetitor(idDog2)
                                    val idAccount = Competitor(idCompetitor1).getAccountID()
                                    team.selectUkaPair(
                                        idAccount,
                                        agilityClass.id,
                                        idDog1,
                                        idDog1,
                                        idDog2,
                                        idCompetitor1,
                                        idCompetitor2,
                                        Height.getHeightCode(jumpHeightText, "UKA"),
                                        Height.getHeightCode(jumpHeightText2, "UKA")
                                    )
                                    agilityClass.enter(
                                        idTeam = team.id,
                                        idAccount = idAccount,
                                        entryType = ENTRY_MANUAL,
                                        fee = 0,
                                        timeEntered = now,
                                        progress = PROGRESS_ENTERED,
                                        heightCode = team.relayHeightCode
                                    )

                                }

                            }
                            ClassTemplate.TEAM -> {
                                val id = sheet.getCell(0, row).asInt
                                val teamName = sheet.getCell(1, row).asString
                                val jumpHeightText = sheet.getCell(2, row).asString
                                val idUka = sheet.getCell(3, row).asInt
                                val competitorName = sheet.getCell(6, row).asString
                                val jumpHeightText2 = sheet.getCell(7, row).asString
                                val idUka2 = sheet.getCell(8, row).asInt
                                val competitorName2 = sheet.getCell(11, row).asString
                                val jumpHeightText3 = sheet.getCell(12, row).asString
                                val idUka3 = sheet.getCell(13, row).asInt
                                val competitorName3 = sheet.getCell(16, row).asString
                                val scratched = sheet.getCell(17, row).asString.isNotBlank()
                                if (id > 0) {
                                    entry.find(id)
                                    val team = Team(entry.idTeam)
                                    val idDog1 = Dog.getIdFromUka(idUka)
                                    val idDog2 = Dog.getIdFromUka(idUka2)
                                    val idDog3 = Dog.getIdFromUka(idUka3)
                                    val idCompetitor1 = Dog.getIdCompetitor(idDog1)
                                    val idCompetitor2 = Dog.getIdCompetitor(idDog2)
                                    val idCompetitor3 = Dog.getIdCompetitor(idDog3)
                                    val idAccount = Competitor(idCompetitor1).getAccountID()
                                    team.selectUkaTeam(
                                        idAccount,
                                        agilityClass.id,
                                        idDog1,
                                        idDog1,
                                        idDog2,
                                        idDog3,
                                        idCompetitor1,
                                        idCompetitor2,
                                        idCompetitor3,
                                        Height.getHeightCode(jumpHeightText, "UKA"),
                                        Height.getHeightCode(jumpHeightText2, "UKA"),
                                        Height.getHeightCode(jumpHeightText3, "UKA"),
                                        teamName
                                    )
                                    if (team.id != entry.idTeam) {
                                        println("${agilityClass.name}: ${entry.teamDescription} composition changed")
                                        entry.idTeam = team.id
                                    }
                                    if (entry.jumpHeightCode != team.relayHeightCode) {
                                        println(
                                            "${agilityClass.name}: ${entry.teamDescription} height to ${Height.getHeightName(
                                                team.relayHeightCode
                                            )}"
                                        )
                                        entry.jumpHeightCode = team.relayHeightCode
                                        entry.heightCode = team.relayHeightCode
                                    }

                                    if (competitorName != team.getCompetitorName(1)) {
                                        println("${agilityClass.name}: ${entry.teamDescription} handler 1 to $competitorName")
                                        team.member(1)["competitorName"] = competitorName
                                    }
                                    if (competitorName2 != team.getCompetitorName(2)) {
                                        println("${agilityClass.name}: ${entry.teamDescription} handler 2 to $competitorName2")
                                        team.member(2)["competitorName"] = competitorName2
                                    }
                                    if (competitorName3 != team.getCompetitorName(3)) {
                                        println("${agilityClass.name}: ${entry.teamDescription} handler 3 to $competitorName3")
                                        team.member(3)["competitorName"] = competitorName3
                                    }
                                    if (scratched && entry.progress != PROGRESS_REMOVED) {
                                        println("${agilityClass.name}: ${entry.teamDescription} SCRATCHED")
                                        entry.progress = PROGRESS_REMOVED
                                    }
                                    team.post()
                                    entry.post()
                                } else {
                                    val team = Team()
                                    val idDog1 = Dog.getIdFromUka(idUka)
                                    val idDog2 = Dog.getIdFromUka(idUka2)
                                    val idDog3 = Dog.getIdFromUka(idUka3)
                                    val idCompetitor1 = Dog.getIdCompetitor(idDog1)
                                    val idCompetitor2 = Dog.getIdCompetitor(idDog2)
                                    val idCompetitor3 = Dog.getIdCompetitor(idDog3)
                                    val idAccount = Competitor(idCompetitor1).getAccountID()
                                    team.selectUkaTeam(
                                        idAccount,
                                        agilityClass.id,
                                        idDog1,
                                        idDog1,
                                        idDog2,
                                        idDog3,
                                        idCompetitor1,
                                        idCompetitor2,
                                        idCompetitor3,
                                        Height.getHeightCode(jumpHeightText, "UKA"),
                                        Height.getHeightCode(jumpHeightText2, "UKA"),
                                        Height.getHeightCode(jumpHeightText3, "UKA"),
                                        teamName
                                    )
                                    agilityClass.enter(
                                        idTeam = team.id,
                                        idAccount = idAccount,
                                        entryType = ENTRY_MANUAL,
                                        fee = 0,
                                        timeEntered = now,
                                        progress = PROGRESS_ENTERED,
                                        heightCode = team.relayHeightCode
                                    )

                                }


                            }
                            else -> {
                                val id = sheet.getCell(0, row).asInt
                                val jumpHeightText = sheet.getCell(1, row).asString
                                val idUka = sheet.getCell(2, row).asInt
                                val competitorName = sheet.getCell(5, row).asString
                                val scratched = sheet.getCell(6, row).asString.isNotBlank()
                                if (id > 0) {
                                    entry.find(id)
                                    if (jumpHeightText != entry.jumpHeightText) {
                                        println("${agilityClass.name}: ${entry.teamDescription} height to $jumpHeightText")
                                        entry.jumpHeightCode = Height.getHeightCode(jumpHeightText, "UKA")
                                        entry.heightCode = Height.getHeightCode(jumpHeightText, "UKA")
                                    }
                                    if (idUka != entry.team.dog.idUka) {
                                        val dog = Dog.select("idUka=$idUka")
                                        dog.first()
                                        val idTeam =
                                            if (competitorName.isEmpty())
                                                Team.getIndividualId(dog.idCompetitor, dog.id)
                                            else
                                                Team.getIndividualNamedId(dog.id, competitorName)
                                        val team = Team(idTeam)
                                        println(
                                            "${agilityClass.name}: ${entry.teamDescription} entry changed to ${team.getTeamDescription(
                                                0
                                            )}"
                                        )
                                        entry.idTeam = idTeam
                                    } else if (competitorName != entry.team.competitorName) {
                                        println("${agilityClass.name}: ${entry.teamDescription} handler to $competitorName")
                                        entry.idTeam = Team.getIndividualNamedId(entry.team.idDog, competitorName)
                                    }
                                    if (scratched && entry.progress != PROGRESS_REMOVED) {
                                        println("${agilityClass.name}: ${entry.teamDescription} SCRATCHED")
                                        entry.progress = PROGRESS_REMOVED
                                    }
                                    entry.post()
                                } else {
                                    var idDog = 0
                                    Dog().where("idUka=$idUka") {
                                        idDog = resolveAlias()
                                    }
                                    val dog = Dog()
                                    dog.join(dog.owner)
                                    dog.seek(idDog)
                                    dog.first()
                                    val idTeam =
                                        if (competitorName.isEmpty())
                                            Team.getIndividualId(dog.idCompetitor, dog.id)
                                        else
                                            Team.getIndividualNamedId(dog.id, competitorName)
                                    val team = Team(idTeam)


                                    if (!Entry.select("idAgilityClass=$idAgilityClass && idTeam=$idTeam").found()) {
                                        println("${agilityClass.name}: ${team.getTeamDescription(0)} ADDED $jumpHeightText")
                                        agilityClass.enter(
                                            idTeam = idTeam,
                                            idAccount = dog.owner.getAccountID(),
                                            heightCode = Height.getHeightCode(jumpHeightText, "UKA"),
                                            entryType = ENTRY_MANUAL,
                                            timeEntered = now,
                                            fee = 0
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (agilityClass.template == ClassTemplate.TEAM) {
                    AgilityClass().where("idCompetition=$idCompetition AND classCode=${ClassTemplate.TEAM_INDIVIDUAL.code}") {
                        prepareEntriesFromFeeder(AgilityClass(idAgilityClassParent))
                    }
                }
            }
        }
        Competition().seek(idCompetition) {
            if (grandFinals) {
                processShowGrandFinals(this, markProcessed = false)
            }
        }
    }


    data class ClassProfileItem(val classes: Int, val heightCodes: String)

    class ClassProfile(var competition: Competition) {

        val map = HashMap<Pair<Int, Int>, ClassProfileItem>()

        init {
            loadFromDatabase()
            if (competition.dateStart.format("YY") == "17") {
                loadBeginners2017()
            }
        }

        fun getClasses(classCode: Int, day: Int): ClassProfileItem {
            val pair = Pair<Int, Int>(classCode, day)
            return map.getOrDefault(pair, ClassProfileItem(0, ""))
        }

        fun setClasses(classCode: Int, day: Int, classes: Int, heightCodes: String) {
            val pair = Pair<Int, Int>(classCode, day)
            map.put(pair, ClassProfileItem(classes, heightCodes))
        }

        fun dropClasses(classCode: Int, day: Int) {
            val pair = Pair<Int, Int>(classCode, day)
            map.remove(pair)
        }

        fun loadFromDatabase() {
            var query = DbQuery(
                """
            SELECT
                classCode, heightCodes, DATEDIFF(classDate, competition.dateStart) AS day, COUNT(*) AS classes
            FROM
                agilityClass JOIN competition USING (idCompetition)
            WHERE
                idCompetition = ${competition.id}
            GROUP BY classCode , classDate
        """
            )
            while (query.next()) {
                val classCode = query.getInt("classCode")
                val day = query.getInt("day")
                val classes = query.getInt("classes")
                val heightCodes = query.getString("heightCodes")
                val template = ClassTemplate.select(query.getInt("classCode"))
                val graded = template.nameTemplate.contains("<grade>")
                val grades = if (competition.isFab) 3 else 4
                setClasses(classCode, day, if (graded) classes / grades else classes, heightCodes)
            }
        }

        // remove after 2018 templates generated ---------------------------------------------------------------
        fun loadBeginners2017() {
            val date = when (competition.id) {
                580 -> "2017-04-02".toDate()
                585 -> "2017-04-22".toDate()
                583 -> "2017-04-30".toDate()
                586 -> "2017-05-27".toDate()
                595 -> "2017-06-24".toDate()
                590 -> "2017-07-08".toDate()
                599 -> "2017-07-23".toDate()
                602 -> "2017-08-13".toDate()
                606 -> "2017-08-20".toDate()
                605 -> "2017-09-02".toDate()
                else -> nullDate
            }
            if (!date.isEmpty()) {
                var day = 0
                while (date > competition.dateStart.addDays(day)) {
                    day++
                }
                setClasses(ClassTemplate.BEGINNERS_STEEPLECHASE_HEAT.code, day, 1, "")
            }
        }
    }


    fun generateUKAHeights(idCompetition: Int) {
        dbTransaction {
            var heightOrderPrevious: List<String> = ArrayList()
            var jumpsNext: List<String> = ArrayList()
            val dateMonitor = ChangeMonitor(nullDate)
            val ringMonitor = ChangeMonitor(0)

            AgilityClass().where("idCompetition=$idCompetition AND ringNumber>0", "classDate, ringNumber, ringOrder") {

                if (dateMonitor.hasChanged(date) or ringMonitor.hasChanged(ringNumber)) {
                    heightOrderPrevious = ArrayList()
                    Ring().where("idCompetition=$idCompetition AND date=${date.sqlDate} AND ringNumber=${ringNumber}") {
                        if (heightBase.isNotEmpty()) {
                            heightOrderPrevious = heightBase.split(",").reverse()
                        }
                    }
                }

                if (template.isRelay) {
                    if (!_heightRunningOrder.oneOf(
                            "UKA901,UKA902,UKA904,UKA903", "UKA901,UKA902,UKA904,UKA903",
                            "UKA903,UKA901,UKA902,UKA904", "UKA904,UKA903,UKA901,UKA902"
                        )) {
                        when (random(4, 1)) {
                            1 -> heightRunningOrder = "UKA901,UKA902,UKA904,UKA903"
                            2 -> heightRunningOrder = "UKA902,UKA904,UKA903,UKA901"
                            3 -> heightRunningOrder = "UKA903,UKA901,UKA902,UKA904"
                            4 -> heightRunningOrder = "UKA904,UKA903,UKA901,UKA902"
                        }
                    }
                } else {
                    jumpsNext = ArrayList()
                    withPeekNext {
                        if (dateMonitor.isSame(date) && ringMonitor.isSame(ringNumber)) {
                            jumpsNext = jumpHeightCodes.split(",")
                        }
                    }

                    val jumpsThis = jumpHeightCodes.split(",")
                    val intersectPrevious = jumpsThis.intersect(heightOrderPrevious)

                    val proposed: ArrayList<String> = ArrayList()

                    if (heightOrderPrevious.size == jumpsThis.size) {
                        proposed.addAll(heightOrderPrevious.reverse())
                        // now substitute any different jumps eg if switching between regular and casual
                        val from = ArrayList(heightOrderPrevious.subtract(intersectPrevious))
                        val to = ArrayList(jumpsThis.subtract(intersectPrevious))
                        for (i in 0 until from.size) {
                            proposed[proposed.indexOf(from[i])] = to[i]
                        }
                    } else {
                        proposed.addAll(jumpsThis.shuffled())
                    }

                    // if last height is not in next class then swap with one that is
                    val intersectNext = jumpsThis.intersect(jumpsNext)
                    if (intersectNext.isNotEmpty() && !intersectNext.contains(proposed.last())) {
                        var i = proposed.size - 2
                        while (i >= 0) {
                            if (intersectNext.contains(proposed[i])) {
                                proposed.swap(i, proposed.size - 1)
                                i = -1
                            } else {
                                i++
                            }
                        }
                    }

                    heightRunningOrder = proposed.asCommaList()
                    heightOrderPrevious = heightRunningOrder.split(",")
                }
                post()
            }
        }
    }

    fun generateFabHeights(idCompetition: Int) {
        dbTransaction {
            var heightOrderPrevious: List<String> = ArrayList()
            val monitor = ChangeMonitor("0")
            val ruleBookMonitor = ChangeMonitor("")

            AgilityClass().where("idCompetition=$idCompetition AND ringNumber>0", "classDate, ringNumber, ringOrder") {
                val jumpsThis = jumpHeightCodes.replace(";", ",").split(",")
                val proposed: ArrayList<String> = ArrayList()
                if (monitor.hasChanged("$ringNumber:${date.sqlDate}")) {
                    ruleBookMonitor.value = template.ruleBook
                    var base = ""
                    Ring().where("idCompetition=$idCompetition AND date=${date.sqlDate} AND ringNumber=${ringNumber}") {
                        if (heightBase.isNotEmpty()) {
                            base = heightBase
                            //heightOrderPrevious = heightBase.split(",").reverse()
                        }
                    }
                    if (base.isNotEmpty()) {
                        proposed.addAll(base.split(","))
                    } else {
                        proposed.addAll(if (ringNumber.rem(2) == 1) jumpsThis else jumpsThis.reverse())
                    }
                } else if (ruleBookMonitor.hasChanged(template.ruleBook)) {
                    proposed.addAll(if (ringNumber.rem(2) == 1) jumpsThis else jumpsThis.reverse())
                } else {
                    if (heightOrderPrevious.size == jumpsThis.size) {
                        proposed.addAll(heightOrderPrevious.reverse())
                    } else {
                        proposed.addAll(if (ringNumber.rem(2) == 1) jumpsThis else jumpsThis.reverse())
                    }
                }
                heightRunningOrder = proposed.asCommaList()
                heightOrderPrevious = heightRunningOrder.split(",")
                post()
            }
        }
    }

    fun generateIndHeights(idCompetition: Int) {
        dbTransaction {
            var heightOrderPrevious: List<String> = ArrayList()
            val monitor = ChangeMonitor("0")

            AgilityClass().where("idCompetition=$idCompetition AND ringNumber>0", "classDate, ringNumber, ringOrder") {
                val jumpsThis = jumpHeightCodes.replace(";", ",").split(",")
                val proposed: ArrayList<String> = ArrayList()
                if (monitor.hasChanged("$ringNumber:${date.sqlDate}")) {
                    var base = ""
                    Ring().where("idCompetition=$idCompetition AND date=${date.sqlDate} AND ringNumber=${ringNumber}") {
                        if (heightBase.isNotEmpty()) {
                            base = heightBase
                            heightOrderPrevious = heightBase.split(",").reverse()
                        }
                    }
                    if (base.isNotEmpty()) {
                        proposed.addAll(base.split(","))
                    } else {
                        proposed.addAll(if (ringNumber.rem(2) == 1) jumpsThis else jumpsThis.reverse())
                    }
                } else {
                    if (heightOrderPrevious.size == jumpsThis.size) {
                        proposed.addAll(heightOrderPrevious.reverse())
                    } else {
                        proposed.addAll(if (ringNumber.rem(2) == 1) jumpsThis else jumpsThis.reverse())
                    }
                }
                heightRunningOrder = proposed.asCommaList()
                heightOrderPrevious = heightRunningOrder.split(",")
                post()
            }
        }
    }

    fun historicNameFix(uniqueName: String, thisYear: String, nextYear: String): String {
        return uniqueName
            .replace("Jan$thisYear", "_${nextYear}_01")
            .replace("Feb$thisYear", "_${nextYear}_02")
            .replace("Mar$thisYear", "_${nextYear}_03")
            .replace("Apr$thisYear", "_${nextYear}_04")
            .replace("May$thisYear", "_${nextYear}_05")
            .replace("Jun$thisYear", "_${nextYear}_06")
            .replace("June$thisYear", "_${nextYear}_06")
            .replace("Jul$thisYear", "_${nextYear}_07")
            .replace("July$thisYear", "_${nextYear}_07")
            .replace("Aug$thisYear", "_${nextYear}_08")
            .replace("Sep$thisYear", "_${nextYear}_09")
            .replace("Sept$thisYear", "_${nextYear}_09")
            .replace("Oct$thisYear", "_${nextYear}_10")
            .replace("Nov$thisYear", "_${nextYear}_11")
            .replace("Dec$thisYear", "_${nextYear}_12")
            .replace("Early_18_02", "_18_02_A")
            .replace("AgilityAntics_18_09", "AgilityAntics_18_09_A")
            .replace("Late_18_02", "_18_02_B")
            .replace("Late_18_09", "_18_09_B")
            .replace("BASE_18_09", "BASE_18")
            .replace("Aldon_18_06", "Aldon_18")
            .replace("JustSox_18_07", "JustSox_18")
            .replace("LADS_18_05", "LADS_18")
            .replace("LAPS_18_07", "LAPS_18")
            .replace("LechladeK9_18_09", "LechladeK9_18")
            .replace("Lydiard_18_04", "Lydiard_18")
            .replace("MADS_18_06", "MADS_18")
            .replace("Paws", "PAwS")
            .replace("QuadPAwS", "QuadPaws")
            .replace("Phoenix_18_05", "Phoenix")
            .replace("RedRun_18_05", "RedRun_18")
            .replace("RIOT_18_09", "RIOT_18")
            .replace("RUFFS_18_07", "RUFFS_18")
            .replace("SuttonWeavers_18_06", "SuttonWeavers_18")
            .replace("Teejay_18_08", "Teejay_18")
            .replace("Valentines_18_02", "Valentines_18")
            .replace("CaptainJack_18_07", "CaptainJack_18")
            .replace("20$thisYear", "_$nextYear")
    }


    fun exportUkaHeights(): String {
        val path = Global.documentPath("uka_measurements", "xls")
        val workbook = createWorkbook(path)

        val sheet = workbook.createSheet("dogs", 0)

        var row = 0
        var column = 0

        with(sheet) {

            addHeading(column++, row, "member", width = 0.7)
            addHeading(column++, row, "firstname", width = 1.5)
            addHeading(column++, row, "lastname", width = 1.5)
            addHeading(column++, row, "dog", width = 0.7)
            addHeading(column++, row, "registered_name", width = 2.0)
            addHeading(column++, row, "pet_name", width = 1.5)
            addHeading(column++, row, "height", width = 0.7)
            addHeading(column++, row, "2nd", width = 0.7)
            addHeading(column++, row, "3rd", width = 0.7)
            addHeading(column++, row, "Permanent", width = 0.9)
            addHeading(column++, row, "Note", width = 1.5)

        }

        row = 2
        Dog().join { owner }.where("dog.idUka between 10000 AND 50000", "dog.idUka") {
            val idDog = id
            column = 0
            with(sheet) {
                var _dog = this
                var _owner = owner
                var note = ""
                if (aliasFor > 0) {
                    val alias = Dog(resolveAlias())
                    if (alias.found()) {
                        note = "Replaced by ${alias.idUka}"
                        _owner = alias.owner
                    } else {
                        debug("TTT", "TTT $idDog")
                    }
                }
                if (_owner.aliasFor > 0) {
                    _owner = Competitor(_owner.resolveAlias())
                }
                addCell(column++, row, _owner.idUka)
                addCell(column++, row, _owner.givenName.naturalCase)
                addCell(column++, row, _owner.familyName.naturalCase)
                addCell(column++, row, idUka)
                addCell(column++, row, registeredName.naturalCase)
                addCell(column++, row, petName.naturalCase)
                addCell(column++, row, ukaMeasure1)
                addCell(column++, row, ukaMeasure2)
                addCell(column++, row, ukaMeasure3)
                addCell(
                    column++,
                    row,
                    if (ukaMeasureProvisional || (ukaMeasure1 + ukaMeasure2 + ukaMeasure3 == 0)) 0 else 1
                )
                addCell(column++, row, note)
                row++
            }


        }
        workbook.write()
        workbook.close()
        return path
    }

    fun importUkaHeights(path: String) {
        val workbook = Workbook.getWorkbook(File(path))
        val sheet = workbook.getSheet(0)

        for (row in 2..sheet.rows - 1) {
            val dogId = sheet.getCell(3, row).contents
            val petName = sheet.getCell(5, row).contents
            val measure1 = sheet.getCell(6, row).contents.toIntDef(0)
            val measure2 = sheet.getCell(7, row).contents.toIntDef(0)
            val measure3 = sheet.getCell(8, row).contents.toIntDef(0)
            val permanent = sheet.getCell(9, row).contents.toIntDef(0)

            if (measure1 > 0) {
                Dog().where("dog.idUka=$dogId") {
                    if (measure1 > 0) ukaMeasure1 = measure1
                    if (measure2 > 0) ukaMeasure2 = measure2
                    if (measure3 > 0) ukaMeasure3 = measure3
                    ukaMeasureProvisional = permanent == 0
                    post()
                }
            }
        }
    }

    fun sortOutIds() {
        Entry.fixIds()
        TabletLog.fixIds()
        MobileSignal.fixIds()
        Radio.fixIds()
        SignOn.fixIds()
        Panic.fixIds()
        CompetitionLedger.fixIds()
        Measurement.fixIds()
    }

    fun checkShowActions() {
        Competition.select("NOT processed", "dateOpens").forEach { competition ->
            if (!competition.processed && !competition.closed && !competition.preCloseEmail && competition.dateCloses != today
                && competition.dateCloses.addDays(-2) <= today
            ) {
                showCloseWarning(competition)
            }
            if (!competition.processed && !competition.closed && !competition.provisional && competition.dateCloses < today) {
                closeShow(competition)
            }
            if (!competition.postCloseEmail && !competition.processed && competition.closed && competition.dateCloses.addDays(
                    3
                ) <= today
            ) {
                showPostCloseWarning(competition)
            }
            if (!competition.postCloseFinalEmail && !competition.processed && competition.closed && competition.dateCloses.addDays(
                    5
                ) <= today
            ) {
                postCloseFinalWarning(competition)
            }
            if (!competition.processed && competition.dateProcessing <= today) {
                processShow(competition)
            }
        }
    }


    fun showCloseWarning(competition: Competition) {
        dbTransaction {
            Competition.balanceQuery(competition.id).forEach { q ->
                val email = q.getString("email").toLowerCase()
                val balance = q.getInt("balance")
                val charge = q.getInt("charge")
                val amount = q.getInt("amount")
                val amountOwing = maxOf(charge - amount, 0)
                val idAccount = q.getInt("idAccount")
                PlazaMessage.closeWarning(competition, idAccount, amountOwing, balance)
            }
            competition.preCloseEmail = true
            competition.post()
        }
    }

    fun showPostCloseWarning(competition: Competition) {
        dbTransaction {
            Competition.balanceQuery(competition.id).forEach { q ->
                val email = q.getString("email").toLowerCase()
                val balance = q.getInt("balance")
                val charge = q.getInt("charge")
                val amount = q.getInt("amount")
                val amountOwing = maxOf(charge - amount, 0)
                val idAccount = q.getInt("idAccount")
                if (balance < amountOwing) {
                    PlazaMessage.entryOnHold2(idAccount, email, competition.name, amountOwing, balance)
                }
            }
            competition.postCloseEmail = true
            competition.post()
        }
    }

    fun postCloseFinalWarning(competition: Competition) {
        dbTransaction {
            Competition.balanceQuery(competition.id).forEach { q ->
                val email = q.getString("email").toLowerCase()
                val balance = q.getInt("balance")
                val charge = q.getInt("charge")
                val amount = q.getInt("amount")
                val amountOwing = maxOf(charge - amount, 0)
                val idAccount = q.getInt("idAccount")
                if (balance < amountOwing) {
                    PlazaMessage.entryOnHold3(idAccount, email, competition.name, amountOwing, balance)
                }
            }
            competition.postCloseFinalEmail = true
            competition.post()
        }
    }

    fun closeShowNow(competition: Competition) {
        dbTransaction {
            competition.changeClosingDate(today.addDays(-1))
            closeShow(competition)
        }
    }


    fun closeShow(competition: Competition) {
        val ledger = Ledger()
        val showName = competition.name
        if (!competition.closed) {
            dbTransaction {
                competition.checkSubClasses()
                competition.closed = true
                competition.post()
                if (competition.hasManagedCamping) finalizeCamping(competition)
                Competition.balanceQuery(competition.id).forEach { q ->
                    val idAccount = q.getInt("idAccount")
                    val email = q.getString("email").toLowerCase()
                    val balance = q.getInt("balance")
                    val charge = q.getInt("charge")
                    val amount = q.getInt("amount")
                    val amountOwing = maxOf(charge - amount, 0)
                    if (amountOwing > 0 && balance > 0) {
                        ledger.seekEntry(idAccount, competition.id)
                        ledger.first()
                        val revisedBalance = ledger.payFromFunds(balance)
                        if (ledger.amountOwing > 0) {
                            PlazaMessage.entryOnHold(idAccount, email, showName, ledger.amountOwing, revisedBalance)
                        } else {
                            PlazaMessage.entryConfirmed(competition, idAccount, ledger.amount)
                        }
                    } else {
                        PlazaMessage.entryOnHold(idAccount, email, showName, amountOwing, balance)
                    }
                }
                releaseFunds(competition)

            }
        }
    }

    fun finalizeCamping(competition: Competition) {
        Camping().where("idCompetition=${competition.id} AND NOT confirmed") {
            rejectApplication()
        }
    }


    fun processShowGrandFinals(competition: Competition, markProcessed: Boolean = true) {
        dbTransaction {
            competition.buildCompetitionDog()
            val idCompetition = competition.id
            AgilityClass().where("idCompetition=$idCompetition") {
                if (template == ClassTemplate.TEAM_INDIVIDUAL) {
                    prepareEntriesFromFeeder(AgilityClass(idAgilityClassParent))
                } else if (template.canEnterDirectly && template != ClassTemplate.TEAM) {
                    prepareEntries()
                }
            }
            if (markProcessed) competition.processed = true
            competition.post()
        }
    }

    fun cancelUnpaidShowEntries(competition: Competition) {
        if (competition.closed) {
            dbTransaction {
                Ledger().join { account }
                    .where("idCompetition = ${competition.id} AND type=$LEDGER_ENTRY_FEES AND ledger.amount<ledger.charge") {
                        payFromFunds(Ledger.balance(idAccount))
                        if (amountOwing > 0) {
                            PlazaMessage.entryDeleted(competition, idAccount)
                            competition.cancelEntry(idAccount, ENTRY_DELETED_NO_FUNDS)
                        }
                    }
            }
        }
    }


    fun processShow(competition: Competition) {
        if (!competition.processed) {
            if (competition.isUkOpen) {
                processShowUkOpen(competition)
            } else if (competition.grandFinals) {
                processShowGrandFinals(competition)
            } else {
                dbTransaction {
                    competition.checkSubClasses()
                    fixCRO(competition.id)
                    cancelUnpaidShowEntries(competition)
                    competition.buildCompetitionDog()
                    if (competition.isKc) {
                        AgilityClass().where("idCompetition=${competition.id}") {
                            if (template.hasChildren && !template.isChild) {
                                populateChildren()
                            }
                        }
                    } else if (competition.isUka) {
                        AgilityClass().where("idCompetition=${competition.id} AND classCode IN (${ClassTemplate.TRY_OUT.code})") {
                            populateChildren(recurse = false)
                        }
                    }


                    AgilityClass().where("idCompetition=${competition.id}") {
                        prepareRunningOrders()
                    }

                    Ledger().join { account }
                        .where("idCompetition = ${competition.id} AND type IN ($LEDGER_ENTRY_FEES, $LEDGER_ENTRY_FEES_PAPER)") {
                            PlazaMessage.runningOrders(competition, idAccount)
                        }

                    competition.processed = true
                    competition.post()
                    releaseFunds(competition)
                }
            }
            if (!competition.isUka && !competition.isUkOpen) {
                uploadRunningOrders(competition.id)
                uploadRingPlan(competition.id)
            }
        }
    }


    fun processShowTest(competition: Competition) {
        if (!competition.processed) {
            if (competition.isUkOpen) {
                processShowUkOpen(competition)
            } else if (competition.grandFinals) {
                processShowGrandFinals(competition)
            } else {
                dbTransaction {
                    competition.checkSubClasses()
                    fixCRO(competition.id)
                    cancelUnpaidShowEntries(competition)
                    competition.buildCompetitionDog()
                    if (competition.isKc) {
                        AgilityClass().where("idCompetition=${competition.id}") {
                            if (template.hasChildren && !template.isChild) {
                                populateChildren()
                            }
                        }
                    } else if (competition.isUka) {
                        AgilityClass().where("idCompetition=${competition.id} AND classCode IN (${ClassTemplate.TRY_OUT.code})") {
                            populateChildren(recurse = false)
                        }
                    }


                    competition.processed = true
                    competition.post()
                }
            }
        }
    }

    fun processShowUkOpen(competition: Competition) {
        dbTransaction {
            Ledger().join { account }
                .where("idCompetition = ${competition.id} AND type=$LEDGER_ENTRY_FEES AND ledger.amount<ledger.charge") {
                    PlazaMessage.entryDeleted(competition, idAccount)
                    competition.cancelEntry(idAccount, ENTRY_DELETED_NO_FUNDS)
                }
        }
    }

    fun fixTeams() {
        AgilityClass.select("classDate>'2018-01-01' AND classCode=${ClassTemplate.TEAM.code}").forEach {
            val entry = Entry()
            entry.join(entry.team).select("entry.idAgilityClass=${it.id}").forEach {
                entry.team.refreshMembers()
                entry.heightCode = entry.team.relayHeightCode
                entry.jumpHeightCode = entry.team.relayHeightCode
                entry.heightCodeEntered = entry.team.relayHeightCode
                entry.post()
            }
        }
    }

    fun fixPairs() {
        AgilityClass.select("classDate>'2018-01-01' AND classCode=${ClassTemplate.SPLIT_PAIRS.code}").forEach {
            val entry = Entry()
            entry.join(entry.team).select("entry.idAgilityClass=${it.id}").forEach {
                entry.team.refreshMembers()
                entry.heightCode = entry.team.relayHeightCode
                entry.jumpHeightCode = entry.team.relayHeightCode
                entry.heightCodeEntered = entry.team.relayHeightCode
                entry.post()
            }
        }
    }

    fun fixCRO(idCompetition: Int) {
        val entry = Entry()
        entry.agilityClass.joinToParent()
        entry.team.joinToParent()
        entry.team.dog.joinToParent()
        entry.select(
            "agilityClass.idCompetition = $idCompetition AND agilityClass.classCode IN " +
                    "(${ClassTemplate.CASUAL_AGILITY.code}, ${ClassTemplate.CASUAL_JUMPING.code}, ${ClassTemplate.CASUAL_STEEPLECHASE.code})"
        )
        entry.forEach {
            val dog = entry.team.dog
            entry.clearRoundOnly = dog.isClearRoundOnly(entry.jumpHeightCode)
            entry.post()
        }

    }


    fun adjustUkaLevels() {
        val baseDate = today
        var dogList = ""

        DbQuery(
            """
            SELECT
                group_concat(distinct team.idDog) as dogList
            FROM
                entry
                    JOIN
                agilityClass USING (idAgilityClass)
                    JOIN
                team USING (idTeam)
            WHERE
                classCode IN (${ClassTemplate.ukaProgressionList})
                    AND classDate < ${baseDate.sqlDate}
                    AND NOT agilityClass.resultsProcessed
                    AND progressionPoints > 0
        """
        ).forEach { it -> dogList = it.getString("dogList") }


        val dog = Dog()

        if (dogList.isNotEmpty()) {
            dog.select("idDog IN ($dogList)")
            while (dog.next()) {
                dog.ukaCalculateGrades()
            }
        }

        dbExecute(
            """
            UPDATE
                agilityClass
            SET
                resultsProcessed = TRUE
            WHERE
                classCode IN (${ClassTemplate.ukaProgressionList})
                    AND classDate < ${baseDate.sqlDate}
                    AND NOT resultsProcessed
        """
        )
    }

    fun checkUkaRegistrations() {
        var owing = 0
        dbQuery(
            """
            SELECT SUM(IF(credit=$ACCOUNT_UKA_HOLDING, amount, -amount)) AS balance
            FROM ledger WHERE NOT ledger.amount<ledger.charge AND credit=$ACCOUNT_UKA_HOLDING OR debit=$ACCOUNT_UKA_HOLDING
        """
        ) { owing = getInt("balance") }
        if (owing > 0) {
            BankPaymentRequest.ukaRegistrationFees(owing)
        }
    }

    fun checkSWAPFees() {
        var owed = 0
        var paid = 0
        Competition().where("NOT idOrganization IN ($ORGANIZATION_UKA, $ORGANIZATION_UK_OPEN) AND processed", "dateStart") {
            val data = CompetitionLedgerData(this)
            owed += data.swapFees
        }
        Ledger().where("debit=$ACCOUNT_SWAP OR credit=$ACCOUNT_SWAP", "ledger.dateEffective") {
            if (debit == ACCOUNT_SWAP) {
                paid += amount
            }
        }
        if (owed - paid > 0) {
            BankPaymentRequest.SwapFees(owed - paid)
        }
    }

    fun releaseFunds(competition: Competition) {
        val idCompetition = competition.id
        dbTransaction {
            if (competition.isUka || competition.isUkOpen) {
                val balance =
                    Ledger.balanceAccount(ACCOUNT_SHOW_HOLDING, "idCompetition=$idCompetition AND NOT ledger.amount<ledger.charge")
                val amount = balance.credit - balance.debit
                if (amount > 0) {
                    BankPaymentRequest.showFees(competition.id, amount)
                }
            } else {
                val amount = competition.getBalanceOwing()
                if (amount > 0) {
                    BankPaymentRequest.showFees(competition.id, amount)
                }
            }
        }
    }

    fun rejectCamping(competition: Competition) {
        // sort out deposits
        LedgerItem().join { ledger }
            .where("ledgerItem.idCompetition=${competition.id} AND ledger.type=$LEDGER_ITEM_CAMPING")
    }


    fun importUKABalances(path: String) {
        val balanceTransfer = BalanceTransfer()
        val competitor = Competitor()
        val workbook = openWorkbook(path)
        dbTransaction {
            val sheet = workbook.getSheet(0)

            for (row in 1..sheet.rows - 1) {
                if (sheet.getCell(0, row).asString.isNotEmpty()) {
                    balanceTransfer.append()
                    balanceTransfer.source = "UKA"
                    balanceTransfer.idUka = sheet.getCell(0, row).asInt
                    balanceTransfer.memberName = sheet.getCell(1, row).asString
                    balanceTransfer.amount = (sheet.getCell(2, row).asDouble * 100.0).toInt()
                    competitor.find("idUka=${balanceTransfer.idUka}")
                    if (competitor.found()) {
                        balanceTransfer.idCompetitor = competitor.id
                        balanceTransfer.idAccount = competitor.idAccount
                    }
                    balanceTransfer.post()
                }
            }
            BalanceTransfer.process("UKA", today)
        }
    }

    fun importSwapBalances(path: String) {
        val balanceTransfer = BalanceTransfer()
        val competitor = Competitor()
        val workbook = openWorkbook(path)
        dbTransaction {
            val sheet = workbook.getSheet(0)

            for (row in 1..sheet.rows - 1) {
                if (sheet.getCell(0, row).asString.isNotEmpty()) {
                    val accountCode = sheet.getCell(0, row).asString
                    if (accountCode.contains("-")) {
                        val idAccount = Account.codeToId(accountCode)
                        balanceTransfer.append()
                        balanceTransfer.source = "SWAP"
                        balanceTransfer.idUka = 0
                        balanceTransfer.memberName = "${sheet.getCell(4, row).asString} (${accountCode})"
                        balanceTransfer.amount = (sheet.getCell(2, row).asDouble * 100.0).toInt()
                        balanceTransfer.idAccount = idAccount
                        balanceTransfer.post()
                    }
                }
            }
            BalanceTransfer.process("SWAP", today)
        }
    }


    fun exportPlaceSheet(idCompetition: Int, maxPlace: Int = 10, maxTrophy: Int = 3, reset: Boolean = false): String {

        fun setColumnWidth(sheet: WritableSheet, columnNumber: Int, width: Int) {
            val column = sheet.getColumnView(columnNumber)
            column.size = width
            sheet.setColumnView(columnNumber, column)
        }

        val inch = 3333
        val competition = Competition()
        val agilityClass = AgilityClass()
        competition.find(idCompetition)
        agilityClass.select("idCompetition=$idCompetition", "ClassNumber, classNumberSuffix")

        val rosetteRule = AwardRule(competition.rosetteRule)
        val trophyRule = AwardRule(competition.trophyRule)

        val fileName = Global.showDocumentPath(competition.uniqueName, "places", "xls")
        val file = File(fileName)
        val workbook = Workbook.createWorkbook(file)

        val overview = workbook.headedSheet("Overview", 0, SHEET_AWARD_TEMPLATE, 1, "Awards Template")
        overview.setWidths(0.5, 1.5, 3.0)
        var row = 2
        overview.addHeading(1, row++, "Ring Plan Details")
        overview.addLabeledCell(0, row++, SHOW_ID, "Identifier", competition.id)
        overview.addLabeledCell(0, row++, UNIQUE_NAME, "Unique Name", competition.uniqueName)
        overview.addLabeledCell(0, row++, DATE_GENERATED, "Date Generated", today)

        val regular = WritableCellFormat(WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD, false))
        val highlight =
            WritableCellFormat(WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.RED))

        regular.setBackground(Colour.YELLOW)
        highlight.setBackground(Colour.YELLOW)
        regular.setBorder(Border.ALL, BorderLineStyle.THIN)
        highlight.setBorder(Border.ALL, BorderLineStyle.THIN)


        val sheet = workbook.createSheet("Places", 1)
        setColumnWidth(sheet, 0, inch * 1)
        setColumnWidth(sheet, 2, inch * 3)
        var column = 0
        sheet.addHeading(column++, 0, "Id", 1.0)
        sheet.addHeading(column++, 0, "Sub")
        sheet.addHeading(column++, 0, "Class", 3.0)
        sheet.addHeading(column++, 0, "Split")
        if (competition.clearRoundOnly) {
            sheet.addHeading(column++, 0, "CRO", right = true)
        }
        sheet.addHeading(column++, 0, "Entries", right = true)
        val placesColumn = ('A'.toByte() + column.toByte()).toChar().toString()
        sheet.addHeading(column++, 0, "Places", right = true)
        val trophiesColumn = ('A'.toByte() + column.toByte()).toChar().toString()
        sheet.addHeading(column++, 0, "Trophies", right = true)

        for (place in 1..maxPlace) {
            sheet.addHeading(7 + place, 0, "P$place", right = true)
        }
        for (trophy in 1..maxTrophy) {
            sheet.addHeading(8 + maxPlace + trophy, 0, "T$trophy", right = true)
        }

        row = 0
        while (agilityClass.next()) {
            for (subClass in 0..agilityClass.subClassCount - 1) {
                val query =
                    DbQuery("SELECT SUM(if(clearRoundOnly, 0, 1)) AS entries, SUM(if(clearRoundOnly, 1, 0)) AS clearRoundOnly FROM entry WHERE idAgilityClass = ${agilityClass.id} AND entry.progress<$PROGRESS_REMOVED AND subClass = ${subClass}")
                query.first()
                val entries = query.getInt("entries")
                val clearRoundOnly = query.getInt("clearRoundOnly")
                val places =
                    if (!agilityClass.haveAwards) rosetteRule.getUnits(entries) else agilityClass.getSubClassRosettes(subClass)
                val trophies =
                    if (!agilityClass.haveAwards) trophyRule.getUnits(entries) else agilityClass.getSubClassTrophies(subClass)
                val proposedPlaces = rosetteRule.getUnits(entries)
                val proposedTrophies = trophyRule.getUnits(entries)

                row++
                column = 0

                sheet.addCell(Label(column++, row, "${agilityClass.id}"))
                sheet.addCell(Label(column++, row, "$subClass"))
                if (subClass == 0) {
                    sheet.addCell(Label(column++, row, agilityClass.nameLong))
                } else {
                    column++
                }
                if (agilityClass.subClassCount > 1) {
                    sheet.addCell(Label(column++, row, agilityClass.subClassDescription(subClass, shortGrade = true)))
                } else {
                    column++
                }

                if (competition.clearRoundOnly) {
                    sheet.addCell(Number(column++, row, clearRoundOnly.toDouble()))
                }
                sheet.addCell(Number(column++, row, entries.toDouble()))
                if (places == proposedPlaces) {
                    sheet.addCell(Number(column++, row, places.toDouble(), regular))
                } else {
                    sheet.addCell(Number(column++, row, places.toDouble(), highlight))
                }
                if (trophies == proposedTrophies) {
                    sheet.addCell(Number(column++, row, trophies.toDouble(), regular))
                } else {
                    sheet.addCell(Number(column++, row, trophies.toDouble(), highlight))
                }

                for (place in 1..maxPlace) {
                    sheet.addCell(Formula(7 + place, row, "IF($placesColumn${row + 1}>=$place,1 ,0)"))
                }
                for (trophy in 1..maxTrophy) {
                    sheet.addCell(Formula(8 + maxPlace + trophy, row, "IF($trophiesColumn${row + 1}>=$trophy,1 ,0)"))
                }

            }


        }


        for (place in 1..maxPlace) {
            sheet.addHeading(7 + place, row + 2, "P$place", right = true)
        }
        for (trophy in 1..maxTrophy) {
            sheet.addHeading(8 + maxPlace + trophy, row + 2, "T$trophy", right = true)
        }

        for (place in 1..maxPlace) {
            val letter = ('A'.toInt() + (7 + place)).toChar()
            sheet.addCell(Formula(7 + place, row + 3, "SUM(${letter}2:${letter}${row + 1})"))
        }
        for (trophy in 1..maxTrophy) {
            val letter = ('A'.toInt() + (8 + maxPlace + trophy)).toChar()
            sheet.addCell(Formula(8 + maxPlace + trophy, row + 3, "SUM(${letter}2:${letter}${row + 1})"))
        }


        workbook.write()
        workbook.close()
        return fileName
    }


    fun importPlacesSheet(workbook: Workbook) {
        val sheet = workbook.sheets[1]
        val agilityClass = AgilityClass()
        for (row in 1..sheet.rows - 1) {
            val idAgilityClass = sheet.getCell(0, row).contents.toIntDef()
            if (idAgilityClass > 0 && agilityClass.find(idAgilityClass)) {
                val subClass = sheet.getCell(1, row).contents.toIntDef()
                val places = sheet.getCell(5, row).contents.toIntDef()
                val trophies = sheet.getCell(6, row).contents.toIntDef()
                if (places >= 0) {
                    agilityClass.setSubClassRosettes(subClass, places)
                    agilityClass.haveAwards = true
                }
                if (trophies >= 0) {
                    agilityClass.setSubClassTrophies(subClass, trophies)
                    agilityClass.haveAwards = true
                }
                agilityClass.post()
            }
        }
    }

    fun importPlacesSheet(path: String) {
        val workbook = Workbook.getWorkbook(File(path))
        val sheet = workbook.sheets[0]
        val agilityClass = AgilityClass()
        for (row in 1..sheet.rows - 1) {
            val idAgilityClass = sheet.getCell(0, row).contents.toIntDef()
            if (idAgilityClass > 0 && agilityClass.find(idAgilityClass)) {
                val subClass = sheet.getCell(1, row).contents.toIntDef()
                val places = sheet.getCell(5, row).contents.toIntDef()
                val trophies = sheet.getCell(6, row).contents.toIntDef()
                if (places >= 0) {
                    agilityClass.setSubClassRosettes(subClass, places)
                }
                if (trophies >= 0) {
                    agilityClass.setSubClassTrophies(subClass, trophies)
                }
                agilityClass.post()
            }
        }
    }

    fun dequeue(emailAccount: String, items: Int = -1) {
        Mutex.ifAquired("dequeue: $emailAccount") {
            val account = EmailQueue.accounts.get(emailAccount)
            var itemsSent = 0
            if (account != null) {
                val emailQueue = EmailQueue()
                emailQueue.select("emailAccount=${emailAccount.quoted} AND NOT status IN (1, 99)", "idEmailQueue")
                if (emailQueue.rowCount > 0) {
                    GoogleMail.withAccount(account.email, account.password) { connection ->
                        var ok = true
                        while (ok && emailQueue.next()) {
                            try {
                                GoogleMail.send(
                                    connection,
                                    emailQueue.emailFrom,
                                    emailQueue.emailTo,
                                    emailQueue.emailCC,
                                    emailQueue.subject,
                                    emailQueue.message
                                )
                                emailQueue.sent = now
                                emailQueue.status = 1
                                emailQueue.post()
                                itemsSent++
                                if (items > 0 && itemsSent >= items) ok = false
                            } catch (e: Throwable) {
                                if (e.message.toString().oneOf(
                                        "Invalid Addresses",
                                        "Domain contains illegal character"
                                    )
                                ) {
                                    emailQueue.status = 99
                                } else {
                                    emailQueue.status = 9
                                    ok = false
                                }
                                val className = e.javaClass.name ?: "unknown"
                                emailQueue.error = "$className: ${e.message.toString()}"
                                emailQueue.post()
                            }
                        }
                    }
                }
            }
        }
    }

    fun dequeuePlaza(items: Int = -1) {
        dequeue("Plaza", items)
    }

    fun swapAccounts(): String {
        val path = Global.documentPath("swap_accounts", "xls")
        val workbook = createWorkbook(path)
        var sheetIndex = 0

        var owed = 0
        var paid = 0

        with(workbook.createSheet("Shows", sheetIndex++)) {
            var row = 2
            var column = 0
            addHeading(column++, row, "Name", 1.5)
            addHeading(column++, row, "Run Units", 1.0, right = true)
            addHeading(column++, row, "Unit Rate", 1.0, right = true)
            addHeading(column++, row, "Run Fees", 1.0, right = true)
            addHeading(column++, row, "Paper Fees", 1.0, right = true)
            addHeading(column++, row, "Print Fees", 1.0, right = true)
            addHeading(column++, row, "Net Paper", 1.0, right = true)
            val totalColumn = column
            addHeading(column++, row, "Total Fees", 1.0, right = true)
            column++
            addHeading(column++, row, "Paper Entries", 1.0, right = true)
            addHeading(column++, row, "Print Cost", 1.0, right = true)

            Competition().where("NOT idOrganization IN ($ORGANIZATION_UKA, $ORGANIZATION_UK_OPEN) AND processed", "dateStart") {
                column = 0; row++
                val data = CompetitionLedgerData(this)
                addCell(column++, row, briefName)
                addCell(column++, row, data.totalRunUnits, WorkbookFormats.default.intFormatRight)
                addCell(column++, row, Money(if (processingFeeSwap == 0) 8 else processingFeeSwap))
                addCell(column++, row, Money(data.swapRunFees))
                addCell(column++, row, Money(data.paperAdmin))
                addCell(column++, row, Money(-data.paperPrintCost))
                addCell(column++, row, Money(data.swapPaperFees))
                addCell(column++, row, Money(data.swapFees))
                column++
                addCell(column++, row, data.paperEntryCount, WorkbookFormats.default.intFormatRight)
                addCell(column++, row, Money(data.unitPrintCost))
                owed += data.swapFees
            }

            row += 2
            addLabel(totalColumn - 1, row, "Total")
            addCell(totalColumn, row, Money(owed))
            row++

            Ledger().where("debit=$ACCOUNT_SWAP OR credit=$ACCOUNT_SWAP", "ledger.dateEffective") {
                row++
                if (debit == ACCOUNT_SWAP) {
                    addLabel(totalColumn - 1, row, "Paid ${dateEffective.dateTextShort}")
                    addCell(totalColumn, row, Money(-amount))
                    paid += amount
                }
            }

            row += 2
            addLabel(totalColumn - 1, row, "Owed")
            addCell(totalColumn, row, Money(owed - paid))


        }
        workbook.quit()
        return path
    }

    fun exportUkaFinalsQualified(): String {
        val GRAND_FINALS = 1749893004


        val path = Global.documentPath("finals_invites", "xls")
        val workbook = createWorkbook(path)
        var sheetIndex = 0

        var sheet = workbook.headedSheet("Overview", sheetIndex++, SHEET_UKA_FINALS_QUALIFIED, 1, "Finals Qualified")
        sheet.setWidths(0.5, 1.5, 3.0)
        var row = 2
        var column = 0
        sheet.addHeading(1, row++, "Show Qualified")
        sheet.addLabeledCell(0, row++, DATE_GENERATED, "Date Generated", today)

        val highlightYellow = WritableCellFormat(WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD, false))
        highlightYellow.setBackground(Colour.YELLOW)
        highlightYellow.setBorder(Border.ALL, BorderLineStyle.THIN)

        val struckout = WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD, false)
        struckout.isStruckout = true
        val struckoutFont = WritableCellFormat(struckout)

        val regularFont = WritableCellFormat(WritableFont(WritableFont.ARIAL, 10, WritableFont.NO_BOLD, false))

        val owingMap = HashMap<Int, Int>()
        Ledger().where("idCompetition=$GRAND_FINALS AND type IN ($LEDGER_ENTRY_FEES, $LEDGER_ENTRY_FEES_PAPER)") {
            owingMap.put(idAccount, amountOwing)
        }

        val clothingMap = HashMap<Int, Int>()
        LedgerItem().where("idCompetition=$GRAND_FINALS AND type IN ($LEDGER_ITEM_CLOTHING)") {
            clothingMap.put(idAccount, amount)
        }

        val showMonitor = ChangeMonitor(-1)
        val agilityClassMonitor = ChangeMonitor(-1)
        val heightMonitor = ChangeMonitor("")

        Entry().join { agilityClass }.join { team }.join { account }.join { agilityClass.competition }
            .join { account.competitor }
            .join { team.dog }.join { team.competitor }
            .join { linkedEntry }
            .where(
                "classCode IN (${ClassTemplate.ukaFinalsQualifierList}) AND classDate > '2019-01-01' AND classDate < CurDate() AND (agilityClass.finalized AND entry.place BETWEEN 1 AND 99 OR classCode=${ClassTemplate.CIRCULAR_KNOCKOUT.code})",
                "competition.dateStart, competition.uniqueName, classCode, " +
                        "classCode=${ClassTemplate.CIRCULAR_KNOCKOUT.code}, if (classCode IN (${ClassTemplate.combineHeightsList}), '', entry.jumpHeightCode), entry.place, competitor_1.givenName, competitor_1.familyName"
            ) {
                if (showMonitor.hasChanged(agilityClass.idCompetition)) {
                    sheet =
                        workbook.headedSheet(agilityClass.competition.uniqueName, sheetIndex++, SHEET_UKA_FINALS_QUALIFIED_SHOW, 1, "Show Invites")
                    row = 2
                    with(sheet) {
                        column = 0
                        addHeading(column++, row, "id1", 0.9)
                        addHeading(column++, row, "id2", 0.9)
                        addHeading(column++, row, "Flags", 0.5)
                        addHeading(column++, row, "Height", 0.6)
                        addHeading(column++, row, "Qualified", 0.7)
                        addHeading(column++, row, "Name", 2.5)
                        addHeading(column++, row, "Invite", 0.5)

                        addHeading(column++, row, "Days", 0.5)
                        addHeading(column++, row, "Entered", 0.7)
                        addHeading(column++, row, "Clothing", 0.7)
                        addHeading(column++, row, "Paid", 0.5)

                        addHeading(column++, row, "Account", 1.2)
                        addHeading(column++, row, "A/c Holder", 1.5)
                        addHeading(column++, row, "Email", 2.5)
                    }
                    row++

                }
                if (agilityClassMonitor.hasChanged(agilityClass.id)) {
                    row++
                    sheet.addHeading(0, row++, agilityClass.name)
                    heightMonitor.value = ""
                    column = 0
                }
                if ((hasRun || agilityClass.template == ClassTemplate.CIRCULAR_KNOCKOUT) && place <= if (agilityClass.template == ClassTemplate.GAMES_CHALLENGE) 25 else agilityClass.template.ukaFinalsPlaces + 3) {
                    with(sheet) {
                        val qualifiedCode = if (agilityClass.template == ClassTemplate.CIRCULAR_KNOCKOUT)
                            "?"
                        else if (place <= agilityClass.template.ukaFinalsPlaces)
                            "q$place"
                        else
                            "r${place - agilityClass.template.ukaFinalsPlaces}"
                        val heightText =
                            if (agilityClass.template.combineHeights) "" else Height.getHeightName(jumpHeightCode)

                        val isMastersPart =
                            agilityClass.template.oneOf(ClassTemplate.MASTERS_AGILITY, ClassTemplate.MASTERS_JUMPING)
                        val autoQualified = qualifiedCode.startsWith("q") && !isMastersPart

                        val proposeInvite = invite || autoQualified && !dontInvite

                        if (heightMonitor.value.isNotEmpty() and heightMonitor.hasChanged(heightText)) row++
                        column = 0
                        val dateInvited =
                            if (linkedEntry.dateCreated < "2019-07-16".toDate()) "2019-07-16".toDate() else linkedEntry.dateCreated
                        val entered = linkedEntry.type.oneOf(ENTRY_AGILITY_PLAZA, ENTRY_PAPER)
                        val amountOwing = owingMap.getOrDefault(idAccount, 0)
                        val amountClothing = clothingMap.getOrDefault(idAccount, 0)
                        val paid = entered && amountOwing == 0
                        val clothing = entered && amountClothing > 0
                        val font = if (linkedEntry.uninvited) struckoutFont else regularFont
                        val spotLight =
                            linkedEntry.invited && !(entered && clothing && paid) && -dateInvited.daysSince(today) > 7

                        addCell(column++, row, id, font)
                        addCell(column++, row, idEntryLinked, font)
                        addCell(column++, row, qualifierFlags or linkedEntry.qualifierFlags, font)
                        addCell(column++, row, heightText, font)
                        addCell(column++, row, qualifiedCode, font)
                        addCell(column++, row, teamDescription, font)
                        if (linkedEntry.uninvited) {
                            column += 5
                        } else {
                            addCell(column++, row, proposeInvite, if (spotLight) highlightYellow else font)
                            addCell(column++, row, if (linkedEntry.id == 0) "" else -dateInvited.daysSince(today), if (spotLight) highlightYellow else font)
                            addCell(column++, row, entered, if (spotLight) highlightYellow else font)
                            addCell(column++, row, clothing, if (spotLight) highlightYellow else font)
                            addCell(column++, row, paid, if (spotLight) highlightYellow else font)
                        }
                        addCell(column++, row, account.code, font)
                        addCell(column++, row, account.competitor.fullName, font)
                        addCell(column++, row, account.competitor.email, font)
                        row++
                    }
                }
            }

        workbook.quit()

        return path
    }

    fun importUkaFinalsInvites(workbook: Workbook) {
        dbTransaction {
            for (sheet in workbook.sheets) {
                if (sheet.identifier == SHEET_UKA_FINALS_QUALIFIED_SHOW) {
                    for (row in 2..sheet.rows - 1) {
                        val idEntryQualifer = sheet.getCell(0, row).contents.toIntDef(-1)
                        val idEntryFinals = sheet.getCell(1, row).contents.toIntDef(-1)
                        val qualifiedCode = sheet.getCell(4, row).contents.toString()
                        val flags = sheet.getCell(2, row).contents.toIntDef(0)
                        val invite = flags.isBitSet(inviteBit)
                        val cancelled = flags.isBitSet(cancelledBit)
                        val uninvited = flags.isBitSet(uninvitedBit)

                        val hasQualified = qualifiedCode.startsWith("q")

                        if (idEntryQualifer > 1) {
                            val ticked = sheet.getCell(6, row).asBoolean
                            if (ticked && (!invite || uninvited)) {
                                Entry.inviteUkaFinals(idEntryQualifer, uninvited, cancelled)
                            } else if (!ticked && (hasQualified || invite)) {
                                Entry.dontInviteUkaFinals(idEntryQualifer, idEntryFinals)
                            }
                        }
                    }
                }
            }
        }
    }

    fun exportUkaFinalsInvited(): String {

        val path = Global.documentPath("finals_invited", "xls")
        val workbook = createWorkbook(path)
        var sheetIndex = 0

        var sheet = workbook.headedSheet("Overview", sheetIndex++, SHEET_UKA_FINAL_INVITED, 1, "Finals Invited")
        sheet.setWidths(0.5, 1.5, 3.0)
        var row = 2
        var column = 0
        sheet.addHeading(1, row++, "Finals Invites")
        sheet.addLabeledCell(0, row++, DATE_GENERATED, "Date Generated", today)


        val showMonitor = ChangeMonitor(-1)
        val agilityClassMonitor = ChangeMonitor(-1)
        val heightMonitor = ChangeMonitor("")

        Entry().join { agilityClass }.join { team }.join { account }.join { agilityClass.competition }
            .join { account.competitor }
            .join { team.dog }.join { team.competitor }
            .where(
                "classCode IN (${ClassTemplate.ukaFinalsQualifierList}) AND " +
                        "classDate > '2019-01-01' AND agilityClass.finalized AND qualifierFlags>0",
                "json_extract(entry.extra, '$.uka.finalsCode'), " +
                        "if (classCode IN (${ClassTemplate.combineHeightsList}), '', entry.jumpHeightCode), competition.dateStart, competition.uniqueName, place"
            ) {
                if (agilityClassMonitor.hasChanged(ukaFinalsCode)) {
                    val template = ClassTemplate.select(ukaFinalsCode)
                    sheet =
                        workbook.headedSheet(template.nameTemplate, sheetIndex++, SHEET_UKA_FINAL_INVITED_CLASS, 1, "Class Invites")
                    row = 2
                    with(sheet) {
                        column = 0
                        addHeading(column++, row, "id", 0.9)
                        addHeading(column++, row, "Height", 0.6)
                        addHeading(column++, row, "Show", 1.4)
                        addHeading(column++, row, "Qualified", 0.6)
                        addHeading(column++, row, "Name", 2.5)
                        addHeading(column++, row, "Status", 1.0)
                        addHeading(column++, row, "Account", 1.2)
                        addHeading(column++, row, "A/c Holder", 1.5)
                        addHeading(column++, row, "Email", 2.5)
                    }
                    row++

                }
                with(sheet) {
                    val qualifiedCode = if (place <= agilityClass.template.ukaFinalsPlaces)
                        "q$place" else "r${place - agilityClass.template.ukaFinalsPlaces}"
                    val heightText =
                        if (agilityClass.template.combineHeights) "" else Height.getHeightName(jumpHeightCode)
                    val status = when {
                        invite -> "Invite"
                        dontInvite -> "Don't Invite"
                        invited -> "Invited"
                        else -> ""
                    }
                    if (heightMonitor.value.isNotEmpty() and heightMonitor.hasChanged(heightText)) row++
                    column = 0
                    addCell(column++, row, id)
                    addCell(column++, row, heightText)
                    addCell(column++, row, agilityClass.competition.uniqueName)
                    addCell(column++, row, qualifiedCode)
                    addCell(column++, row, teamDescription)
                    addCell(column++, row, status)
                    addCell(column++, row, account.code)
                    addCell(column++, row, account.competitor.fullName)
                    addCell(column++, row, account.competitor.email)
                    row++
                }
            }

        workbook.quit()

        return path
    }


    fun cancellationEntryRefund(idCompetition: Int, totalRefund: Int) {
        var totalFees = 0
        var totalRefunded = 0
        dbQuery(
            """
            SELECT 
                SUM(ledgerItem.amount) AS totalFees
            FROM
                ledgerItem
                    JOIN
                ledger USING (idLedger)
            WHERE
                ledgerItem.idCompetition = $idCompetition
                    AND ledgerItem.type IN ($LEDGER_ITEM_ENTRY, $LEDGER_ITEM_ENTRY_SURCHARGE, $LEDGER_ITEM_ENTRY_DISCOUNT, $LEDGER_ITEM_ENTRY_CREDIT)
                    AND ledger.type = $LEDGER_ENTRY_FEES
        """.trimIndent()
        )
        { totalFees = getInt("totalFees") }

        dbQuery(
            """
            SELECT 
                ledgerItem.idAccount, SUM(ledgerItem.amount) AS totalEntryFees
            FROM
                ledgerItem
                    JOIN
                ledger USING (idLedger)
            WHERE
                ledgerItem.idCompetition = $idCompetition
                    AND ledgerItem.type IN ($LEDGER_ITEM_ENTRY, $LEDGER_ITEM_ENTRY_SURCHARGE, $LEDGER_ITEM_ENTRY_DISCOUNT, $LEDGER_ITEM_ENTRY_CREDIT)
                    AND ledger.type = $LEDGER_ENTRY_FEES
            GROUP BY ledgerItem.idAccount
                    """.trimIndent()
        )
        {
            val idAccount = getInt("idAccount")
            val totalEntryFees = getInt("totalEntryFees")
            val refund = totalEntryFees * totalRefund / totalFees
            totalRefunded += refund
            Ledger.addCompetitionCancellationRefund(idCompetition, idAccount, refund, totalEntryFees)
        }

        debug("cancellationEntryRefund", "Unused: ${totalRefund - totalRefunded}")
    }


    fun exportUkOpenGroups(idCompetition: Int): String {
        val path = Global.documentPath("uk_open_groups", "xls")
        val workbook = createWorkbook(path)
        var sheetIndex = 0

        var sheet = workbook.headedSheet("Overview", sheetIndex++, SHEET_UK_OPEN_GROUPS, 1, "UK Open Groups")
        sheet.setWidths(0.5, 1.5, 3.0)
        var row = 2
        var column = 0
        sheet.addHeading(1, row++, "Parameters")
        sheet.addLabeledCell(0, row++, SHOW_ID, "Identifier", idCompetition)
        sheet.addLabeledCell(0, row++, DATE_GENERATED, "Date Generated", today)
        sheet.addLabeledCell(0, row++, NOTES, "Notes", "Update the Height and Group values as required. When you import, running orders will be generated/adjusted. Do not use once data cards have been burnt.", wrap = true)


        with(workbook.headedSheet("Dogs", sheetIndex++, PlazaAdmin.SHEET_UK_OPEN_GROUPS_DOG, 1, "Dogs")) {
            var row = 2
            var column = 0
            addHeading(column++, row, "Id", width = 1.0)
            addHeading(column++, row, "Account", width = 1.1)
            addHeading(column++, row, "Given Name", width = 1.0)
            addHeading(column++, row, "Family Name", width = 1.0)
            addHeading(column++, row, "Dog", width = 0.7)
            addHeading(column++, row, "Pet Name", width = 1.0)
            addHeading(column++, row, "Height", width = 0.6)
            addHeading(column++, row, "Group", width = 0.6)

            CompetitionDog().join { dog }.join { account }
                .where("competitionDog.idCompetition=$idCompetition", "account.accountCode") {
                    column = 0; row++
                    addCell(column++, row, idDog)
                    addCell(column++, row, account.code)
                    addCell(column++, row, ukOpenHandler.trim().substringBeforeLast(" "))
                    addCell(column++, row, ukOpenHandler.trim().substringAfterLast(" "))
                    addCell(column++, row, dog.code)
                    addCell(column++, row, dog.cleanedPetName)
                    addCell(column++, row, Height.getHeightName(ukOpenHeightCode))
                    addCell(column++, row, ukOpenGroup)
                }
        }
        workbook.quit()
        return path
    }

    fun importUkOpenGroups(workbook: Workbook) {
        val overview = workbook.getSheet(0)
        val idCompetition = overview.getLabeledCell(0, SHOW_ID)?.asInt ?: -1
        mandate(idCompetition > 0, "Spreadsheet does not have a valid competition identifier")
        for (sheet in workbook.sheets) {
            if (sheet.identifier == SHEET_UK_OPEN_GROUPS_DOG) {
                for (row in 2..sheet.rows - 1) {
                    val idDog = sheet.getCell(0, row).contents.toIntDef(-1)
                    val heightCode = "OP${sheet.getCell(6, row).asInt}"
                    val group = sheet.getCell(7, row).asString
                    if (idDog > 0 && group.isNotEmpty()) {
                        CompetitionDog().where("idCompetition=$idCompetition AND idDog=$idDog") {
                            ukOpenGroup = group.toUpperCase()
                            ukOpenHeightCode = heightCode
                            post()
                        }
                    }
                }
            }
        }
        UkOpenUtils.generateRunningOrders(idCompetition)
    }


    fun chaseEntries(thisYearId: Int, lastYearId: Int) {
        val competition = Competition(thisYearId)
        dbQuery(
            """
            SELECT 
                l1.idAccount
            FROM
                ledger AS l1
                    LEFT JOIN
                ledger AS l2 ON l2.idCompetition = $thisYearId
                    AND l2.idAccount = l1.idAccount
                    AND l2.type IN (200 , 201)
            WHERE
                l1.idCompetition = $lastYearId
                    AND l1.type = 200
                    AND l2.idLedger IS NULL;            
        """.trimIndent()
        ) {
            val idAccount = getInt("idAccount")
            PlazaMessage.chaseEntries(competition, idAccount)
        }
    }

    fun exportCruftsTeams(idCompetition: Int): String {
        val competition = Competition(idCompetition)

        val path = Global.showDocumentPath(competition.uniqueName, "crufts_teams", "xls")
        val workbook = createWorkbook(path)

        var sheetIndex = 0
        var sheet = workbook.headedSheet("Overview", sheetIndex++, SHEET_CRUFTS_TEAMS_TEMPLATE, 1, "Crufts Teams")
        sheet.setWidths(0.5, 1.5, 3.0)
        var row = 2
        var column = 0
        sheet.addHeading(1, row++, "Parameters")
        sheet.addLabeledCell(0, row++, SHOW_ID, "Identifier", idCompetition)
        sheet.addLabeledCell(0, row++, DATE_GENERATED, "Date Generated", today)

        sheet = workbook.headedSheet("Teams", sheetIndex++, SHEET_CRUFTS_TEAMS_DETAIL, 1, "Team Details")

        row = 0
        column = 0
        sheet.addHeading(column++, row, "id", 1.0)
        sheet.addHeading(column++, row, "Account", 1.0)
        sheet.addHeading(column++, row, "Entered By", 2.0)
        sheet.addHeading(column++, row, "Team Name", 2.0)
        sheet.addHeading(column++, row, "Club Name", 2.0)
        sheet.addHeading(column++, row, "Height", 1.0)
        for (i in 1..6) {
            sheet.addHeading(column++, row, "Code $i", 1.0)
            sheet.addHeading(column++, row, "Dog $i", 2.0)
            sheet.addHeading(column++, row, "Handler $i", 2.0)
        }

        Entry().join { account }.join { account.competitor }.join { team }.join { agilityClass }
            .where(
                "agilityClass.idCompetition=$idCompetition AND agilityClass.classCode=${ClassTemplate.KC_CRUFTS_TEAM.code}",
                "json_extract(team.extra, '\$.teamName')"
            ) {
                row++
                column = 0
                sheet.addCell(column++, row, team.id)
                sheet.addCell(column++, row, account.code)
                sheet.addCell(column++, row, account.competitor.fullName)
                sheet.addCell(column++, row, team.teamName)
                sheet.addCell(column++, row, team.clubName)
                sheet.addCell(column++, row, jumpHeightText)
                for (member in team.members) {
                    sheet.addCell(column++, row, member["dogCode"].asString)
                    sheet.addCell(column++, row, member["petName"].asString)
                    sheet.addCell(column++, row, member["competitorName"].asString)
                }
            }

        workbook.quit()

        return path
    }

    fun importCruftsTeams(workbook: Workbook) {
        val sheet = workbook.getSheet(1)

        for (row in 2..sheet.rows - 1) {
            var column = 0
            val idTeam = sheet.getCell(column++, row).contents.toIntDef(0)
            if (idTeam > 0) {
                val accountCode = sheet.getCell(column++, row).contents.toString()
                val enteredBy = sheet.getCell(column++, row).contents.toString()
                val teamName = sheet.getCell(column++, row).contents.toString()
                val clubName = sheet.getCell(column++, row).contents.toString()
                val height = sheet.getCell(column++, row).contents.toString()

                val team = Team(idTeam)
                team.teamName = teamName
                team.clubName = clubName
                if (team.found()) {
                    for (i in 1..6) {
                        val dogCode = sheet.getCell(column++, row).contents.toIntDef(0)
                        val petName = sheet.getCell(column++, row).contents.toString()
                        val handlerName = sheet.getCell(column++, row).contents.toString()
                    }
                }
                team.post()
            }
        }
    }

    fun uploadRingPlan(idCompetition: Int) {
        val ringPlan = KcAccountPaperwork(idCompetition).ringPlan()
        putCompetitionDocument(idCompetition, "Ring Plan", ringPlan)
    }

    fun uploadRunningOrders(idCompetition: Int) {
        val list = ArrayList<String>()
        AgilityClass().where("idCompetition=$idCompetition", "classCode, jumpHeightCodes DESC") {
            if (strictRunningOrder) {
                var hasEntries = false
                dbQuery("SELECT true FROM entry WHERE idAgilityClass=$id LIMIT 1") {
                    hasEntries = true
                }
                if (hasEntries) {
                    list.add(Reports.printRunningOrders(id, pdf = true))
                }
            }
        }
        if (list.isNotEmpty()) {
            val target = Report.nameOutfile("running_orders", true, idCompetition)
            mergePdfs(target, list)
            putCompetitionDocument(idCompetition, "Running Orders", target)

        }
    }

    fun mergePdfs(target: String, list: List<String>) {
        if (list.isNotEmpty()) {
            val targetStream = FileOutputStream(File(target))
            val pdfDocument = Document()
            val pdfSmartCopy = PdfSmartCopy(pdfDocument, targetStream)
            pdfDocument.open()
            for (source in list) {
                val pdfReader = PdfReader(source)
                for (i in 1..pdfReader.numberOfPages) {
                    pdfSmartCopy.addPage(pdfSmartCopy.getImportedPage(pdfReader, i))
                }
                pdfReader.close()
            }
            pdfDocument.close()
            targetStream.close()
        }
    }

    fun putCompetitionDocument(idCompetition: Int, documentName: String, source: String) {
        val competition = Competition(idCompetition)
        val competitionDocument = CompetitionDocument()
        val documentPath = "/published/${competition.uniqueName}/${documentName}.pdf"
        val target = Global.showDocumentPath(idCompetition, documentName, "pdf", canRegenerate = false)
        Files.move(File(source).toPath(), prepareFile(target).toPath(), StandardCopyOption.REPLACE_EXISTING)
        competitionDocument.select("idCompetition=$idCompetition AND documentName=${documentName.quoted}")
        if (!competitionDocument.first()) {
            competitionDocument.append()
            competitionDocument.idCompetition = idCompetition
            competitionDocument.documentName = documentName
        }
        competitionDocument.documentPath = documentPath
        competitionDocument.post()
    }


}





