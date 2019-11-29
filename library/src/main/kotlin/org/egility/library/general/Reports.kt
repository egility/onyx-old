/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import java.util.*

/**
 * Created by mbrickman on 09/03/16.
 */

object Reports {

    val RESULTS = "results"
    val AWARDS = "awards"
    val ACCOUNT_PAYMENTS = "account_payments"
    val ACCOUNT_OWING = "account_owing"
    val ACCOUNT_PROJECTED_OWING = "account_projected_owing"
    val LATE_ENTRY_CREDITS = "late_entry_credits"
    val LATE_ENTRY_FREE = "late_entry_free"
    val LATE_ENTRY_SPECIAL = "late_entry_special"
    val COMPLIMENTARY_CREDITS_REPORT = "complimentary_credits_used"
    var CHEQUE_LIST = "cheque_list"
    val RUNNING_ORDERS = "running_orders"
    val ENTRIES = "entries"
    val SHOW_OWING = "show_owing"
    val KNOCKOUT_RESULTS = "knockout_results"
    val KNOCKOUT_ENTRIES = "knockout_entries"
    val END_OF_DAY = "end_of_day"
    var RING_CARDS = "ring_cards"
    var CALLING_SHEET = "calling_sheets"
    var EMERGENCY_SCRIME = "emergency_scrime"
    var PAPER_SCORE = "paper_score"
    var PAPER_PLACE = "paper_place"
    var CAMPING_LIST = "camping_list"
    var ADDRESS_LABELS = "address_labels"
    var AWARDS_LABELS = "awards_labels"
    var SCRIME_SHEETS = "scrime_sheets"
    var PERSONAL_RUNNING_ORDERS = "personal_running_orders"
    var MEASUREMENT = "measurement"
    var REGISTRATION = "registration"
    val MARKETING_REPORT = "marketing_report"
    var ENTRY_FORM = "entry_form"

    fun printResults(idAgilityClass: Int, pdf: Boolean = false, copies: Int = 1, subResultsFlag: Int = 0, finalize: Boolean = false, tournament: Boolean = false): String {
        try {
            val pdfFile = Param<String>()
            val request = Json()
            request["report"] = RESULTS
            request["idAgilityClass"] = idAgilityClass
            request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
            request["copies"] = copies
            request["subResultsFlag"] = subResultsFlag
            request["finalize"] = finalize
            request["tournament"] = tournament
            return Global.services.generateReport(request)
        } catch (e: Throwable) {
            debug("Report Panic", e.stack)
            return ""
        }
    }

    fun printAwards(idAgilityClass: Int, pdf: Boolean = false, copies: Int = 1): String {
        try {
            val pdfFile = Param<String>()
            val request = Json()
            request["report"] = AWARDS
            request["idAgilityClass"] = idAgilityClass
            request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
            request["copies"] = copies

            return Global.services.generateReport(request)

        } catch (e: Throwable) {
            debug("Report Panic", e.stack)
            return ""
        }
    }

    fun printRunningOrders(idAgilityClass: Int, pdf: Boolean = false, copies: Int = 1, tournament: Boolean = false): String {
        try {
            val pdfFile = Param<String>()
            val request = Json()
            request["report"] = RUNNING_ORDERS
            request["idAgilityClass"] = idAgilityClass
            request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
            request["copies"] = copies
            request["tournament"] = tournament

            return Global.services.generateReport(request)

        } catch (e: Throwable) {
            debug("Report Panic", e.stack)
            return ""
        }
    }

    fun printEntries(idAgilityClass: Int, pdf: Boolean = false, copies: Int = 1): String {
        try {
            val pdfFile = Param<String>()
            val request = Json()
            request["report"] = ENTRIES
            request["idAgilityClass"] = idAgilityClass
            request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
            request["copies"] = copies

            return Global.services.generateReport(request)

        } catch (e: Throwable) {
            debug("Report Panic", e.stack)
            return ""
        }
    }

    fun printAccountPayments(idCompetition: Int, pdf: Boolean = false, copies: Int = 1, accountOrder: Boolean = false): String {
        val pdfFile = Param<String>()
        val request = Json()
        request["report"] = ACCOUNT_PAYMENTS
        request["idCompetition"] = idCompetition
        request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
        request["copies"] = copies
        request["accountOrder"] = accountOrder

        return Global.services.generateReport(request)
    }

    fun printAccountsOwing(idCompetition: Int, pdf: Boolean = false, copies: Int = 1): String {
        val pdfFile = Param<String>()
        val request = Json()
        request["report"] = ACCOUNT_OWING
        request["idCompetition"] = idCompetition
        request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
        request["copies"] = copies

        return Global.services.generateReport(request)
    }

    fun printAccountsProjectedOwing(idCompetition: Int, pdf: Boolean = false, copies: Int = 1): String {
        val pdfFile = Param<String>()
        val request = Json()
        request["report"] = ACCOUNT_PROJECTED_OWING
        request["idCompetition"] = idCompetition
        request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
        request["copies"] = copies

        return Global.services.generateReport(request)
    }

    fun printLateEntryCredits(idCompetition: Int, date: Date= nullDate, pdf: Boolean = false, copies: Int = 1): String {
        val pdfFile = Param<String>()
        val request = Json()
        request["report"] = LATE_ENTRY_CREDITS
        request["idCompetition"] = idCompetition
        request["date"] = date
        request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
        request["copies"] = copies

        return Global.services.generateReport(request)
    }

    fun printMeasurements(idCompetition: Int, pdf: Boolean = false, copies: Int = 1): String {
        val pdfFile = Param<String>()
        val request = Json()
        request["report"] = MEASUREMENT
        request["idCompetition"] = idCompetition
        request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
        request["copies"] = copies

        return Global.services.generateReport(request)
    }

    fun printRegistrations(idCompetition: Int, pdf: Boolean = false, copies: Int = 1): String {
        val pdfFile = Param<String>()
        val request = Json()
        request["report"] = REGISTRATION
        request["idCompetition"] = idCompetition
        request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
        request["copies"] = copies

        return Global.services.generateReport(request)
    }

    fun printLateEntryFree(idCompetition: Int, date: Date= nullDate, pdf: Boolean = false, copies: Int = 1): String {
        val request = Json()
        request["report"] = LATE_ENTRY_FREE
        request["idCompetition"] = idCompetition
        request["date"] = date
        request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
        request["copies"] = copies

        return Global.services.generateReport(request)
    }

    fun printLateEntrySpecial(idCompetition: Int, date: Date= nullDate, pdf: Boolean = false, copies: Int = 1): String {
        val request = Json()
        request["report"] = LATE_ENTRY_SPECIAL
        request["idCompetition"] = idCompetition
        request["date"] = date
        request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
        request["copies"] = copies

        return Global.services.generateReport(request)
    }

    fun printComplimentaryCreditsUsed(idCompetition: Int, pdf: Boolean = false, copies: Int = 1): String {
        val request = Json()
        request["report"] = COMPLIMENTARY_CREDITS_REPORT
        request["idCompetition"] = idCompetition
        request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
        request["copies"] = copies

        return Global.services.generateReport(request)
    }

    fun printChequeList(idCompetition: Int, pdf: Boolean = false, copies: Int = 1): String {
        val request = Json()
        request["report"] = CHEQUE_LIST
        request["idCompetition"] = idCompetition
        request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
        request["copies"] = copies

        return Global.services.generateReport(request)
    }

    fun printRingCards(idCompetition: Int, pdf: Boolean = false, copies: Int = 1): String {
        val request = Json()
        request["report"] = RING_CARDS
        request["idCompetition"] = idCompetition
        request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
        request["copies"] = copies

        return Global.services.generateReport(request)
    }

    fun printCallingSheets(idCompetition: Int=0, idAgilityClass: Int=0, pdf: Boolean = false, copies: Int = 1, formal: Boolean=false): String {
        try {
            val pdfFile = Param<String>()
            val request = Json()
            request["report"] = CALLING_SHEET
            request["idCompetition"] = idCompetition
            request["idAgilityClass"] = idAgilityClass
            request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
            request["copies"] = copies
            request["formal"] = formal

            return Global.services.generateReport(request)

        } catch (e: Throwable) {
            debug("Report Panic", e.stack)
            return ""
        }
    }

    fun printScrimeSheets(idCompetition: Int=0, idAgilityClass: Int=0, pdf: Boolean = false, copies: Int = 1, formal: Boolean=false): String {
        try {
            val pdfFile = Param<String>()
            val request = Json()
            request["report"] = EMERGENCY_SCRIME
            request["idCompetition"] = idCompetition
            request["idAgilityClass"] = idAgilityClass
            request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
            request["copies"] = copies
            request["formal"] = formal

            return Global.services.generateReport(request)

        } catch (e: Throwable) {
            debug("Report Panic", e.stack)
            return ""
        }
    }

    fun printPersonalRunningOrders(idAccount: Int=0, idCompetition: Int=0,  pdf: Boolean = false, copies: Int = 1): String {
        try {
            val pdfFile = Param<String>()
            val request = Json()
            request["report"] = PERSONAL_RUNNING_ORDERS
            request["idAccount"] = idAccount
            request["idCompetition"] = idCompetition
            request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
            request["copies"] = copies

            return Global.services.generateReport(request)

        } catch (e: Throwable) {
            debug("Report Panic", e.stack)
            return ""
        }
    }

    fun printAddressLabels(idCompetition: Int, pdf: Boolean = false, copies: Int = 1): String {
        val request = Json()
        request["report"] = ADDRESS_LABELS
        request["idCompetition"] = idCompetition
        request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
        request["copies"] = copies

        return Global.services.generateReport(request)
    }


    fun printKnockoutResults(idAgilityClass: Int, pdf: Boolean = false, copies: Int = 1): String {
        val request = Json()
        request["report"] = KNOCKOUT_RESULTS
        request["idAgilityClass"] = idAgilityClass
        request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
        request["copies"] = copies

        return Global.services.generateReport(request)
    }

    fun printKnockoutEntries(idAgilityClass: Int, pdf: Boolean = false, copies: Int = 1): String {
        val request = Json()
        request["report"] = KNOCKOUT_ENTRIES
        request["idAgilityClass"] = idAgilityClass
        request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
        request["copies"] = copies

        return Global.services.generateReport(request)
    }

    fun printEndOfDay(idCompetition: Int, date: Date, pdf: Boolean = false, copies: Int = 1): String {
        val request = Json()
        request["report"] = END_OF_DAY
        request["idCompetition"] = idCompetition
        request["date"] = date.softwareDate
        request["pdf"] = pdf || (Global.testMode && Global.alwaysToPdf)
        request["copies"] = copies

        return Global.services.generateReport(request)
    }

}

