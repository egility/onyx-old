/*
 * Copyright (c) Mike Brickman 2014-2018
 */

package org.egility.library.general

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Created by mbrickman on 03/07/18.
 */
class TabletLog(var path: String) {


    fun analyze() {
        val file = File(path)
        val reader = BufferedReader(FileReader(file))
        var line = reader.readLine()
        var lastSql = ""
        var event = ""
        var eventSqlTime = 0.0
        var eventStart = nullDate
        var time = nullDate
        var endWhen = ""

        fun endEvent(message: String) {
            val elapsed = ((time.time - eventStart.time) / 1000L).toInt()
            val sqlTimeText = "%.1f".format(eventSqlTime)
            val startText = eventStart.format("HH:mm:ss")
            println("$startText +$elapsed $event -> $message (sql = $sqlTimeText)")
            event = ""

        }

        fun startEvent(name: String) {
            if (event.isNotEmpty()) endEvent("END")
            event = name
            eventSqlTime = 0.0
            eventStart = time
        }

        while (line != null) {
            val timeString = line.substring(0, 8)
            val thread = line.substring(8).substringBefore(":").trim().toIntDef(0)
            val log = line.substring(8).substringAfter(":").trim()
            val type = log.substringBefore(" ===> ")
            val message = log.substringAfter(" ===> ")
            time = timeString.toDate("HH:mm:ss")

            when (type) {
                "SQL" -> {
                    if (message.contains("Time taken")) {
                        val timeTaken = message.substringAfter("=").substringBefore("msec").trim().toDoubleDef(0.0)
                        val timeTakenText = "%6.1f".format(timeTaken)
                        if (event.isEmpty()) {
                            println("$timeString $timeTakenText   $lastSql")
                        } else {
                            // println("   $timeTakenText   $lastSql")
                        }
                        eventSqlTime += timeTaken
                        if (endWhen.isNotEmpty() && lastSql.startsWith(endWhen)) {
                            endWhen = ""
                            endEvent("End when")
                        }
                    } else if (message.startsWith("Query")) {
                        val select = message.substringAfter(":").trim()
                        lastSql = select
                    } else if (message.startsWith("Execute")) {
                        val execute = message.substringAfter(":").trim()
                        lastSql = execute
                    } else if (message.startsWith("Connect")) {
                        val connect = message.substringAfter("//").substringBeforeLast("/").trim()
                        lastSql = "Connect $connect"
                    } else {
                        println("$timeString, $thread, $type, $message")
                    }
                }
                "Activity" -> {
                    when (message) {
                        "Fragment.SelectTeamByRunningOrderFragment: sendSignal (OK, delayed=false)" -> {
                            startEvent("Select runner")
                        }
                        "Fragment.EntryStatusFragment: onResume" -> {
                            endEvent("Runner selected")
                        }
                        "Fragment.EntryStatusFragment: sendSignal (JOIN_QUEUE, delayed=false)" -> {
                            startEvent("Ready to run")
                        }
                        "Fragment.EntryStatusFragment: sendSignal (LEAVE_QUEUE, delayed=false)" -> {
                            startEvent("Not ready to run")
                        }
                        "Fragment.SelectTeamByRunningOrderFragment: onResume" -> {
                            endEvent("Ready for next")
                        }
                        "Fragment.SelectTeamByRunningOrderFragment: sendSignal (BACK, delayed=false)" -> {
                            startEvent("Back (Manage queue)")
                        }
                        "Menu: sendSignal (MENU_QUERY_PROGRESS, delayed=false)" -> {
                            startEvent("Manage queue")
                        }
                        "Fragment.SelectTeamByRunningOrderFragment: sendSignal (LIST_BY_NAME, delayed=false)" -> {
                            startEvent("Search")
                        }
                        "Fragment.CallListFragment: onResume" -> {
                            endEvent("Search results")
                        }
                        "Fragment.CallListFragment: sendSignal (BACK, delayed=false)" -> {
                            startEvent("Back (search)")
                        }
                        "Home: sendSignal (HAVE_DATABASE_HOST, delayed=false)" -> {
                            startEvent("Connect - Load data")
                        }
                        "Menu: onResume" -> {
                            endEvent("Menu")
                            startEvent("Menu")
                        }
                        "Fragment.EntryStatusFragment: sendSignal (CONFIRM_ENTRY, delayed=false)" -> {
                            startEvent("Mark late")
                        }
                        "Fragment.SelectTeamByRunningOrderFragment: sendSignal (HEIGHT_SELECTED, delayed=false)" -> {
                            startEvent("Choose height")
                            endWhen = "SELECT jumpHeightCode, MIN(entry.runningOrder) AS minRunningOrder"
                        }
                        "Fragment.EntryStatusFragment: sendSignal (BACK, delayed=false)" -> {
                            startEvent("Back (runner)")
                        }
                        "Fragment.CallListFragment: sendSignal (ENTRY_SELECTED, delayed=false)" -> {
                            startEvent("Search (runner clicked)")
                        }
                        "Fragment.QueueFragment: sendSignal (ENTRY_SELECTED, delayed=false)" -> {
                            startEvent("Review (runner clicked)")
                        }
                        "Fragment.SelectTeamByRunningOrderFragment: sendSignal (RUNNERS_LIST, delayed=false)" -> {
                            startEvent("Review")
                        }
                        "Fragment.QueueFragment: onResume" -> {
                            endEvent("Review ready")
                        }
                        "Fragment.QueueFragment: sendSignal (BACK, delayed=false)" -> {
                            startEvent("Back (review)")
                        }
                        "Fragment.EntryStatusFragment: sendSignal (CHANGE_HANDLER, delayed=false)" -> {
                            startEvent("Change Handler")
                        }
                        "Fragment.EntryStatusFragment: sendSignal (WITHDRAWN, delayed=false)" -> {
                            startEvent("Withdrawn")
                        }
                        "Fragment.QueueFragment: sendSignal (REFRESH, delayed=false)" -> {
                            if (event.isEmpty()) {
                                startEvent("Refresh (review)")
                                endWhen = "SELECT jumpHeightCode, MIN(entry.runningOrder) AS minRunningOrder"
                            }
                        }
                        "Scrime: loadFragment (WaitFragment, STACK)" -> {
                            startEvent("Eliminated")
                        }
                        "Fragment.WaitFragment: sendSignal (RUN_COMPLETE, delayed=false)" -> {
                            startEvent("Ring clear")
                        }
                        "Fragment.TimeFragment: sendSignal (RUN_COMPLETE, delayed=false)" -> {
                            startEvent("Scrime done")
                        }
                        "Fragment.ScrimeCompetitorFragment: onResume" -> {
                            endEvent("Have competitor")
                        }
                        "Fragment.ScrimeCompetitorFragment: sendSignal (ENTER_SCORE, delayed=false)" -> {
                            startEvent("Start scoring")
                        }
                        "Fragment.ScoreFragment: onResume" -> {
                            endEvent("Score page")
                        }
                        "Fragment.ScoreFragment: sendSignal (ENTER_TIME, delayed=false)" -> {
                            startEvent("Enter time")
                        }
                        "Scrime: sendSignal (ENTER_TIME, delayed=false)" -> {
                            startEvent("Enter time")
                        }
                        "Fragment.TimeFragment: onResume" -> {
                            endEvent("Time page")
                        }
                        "Fragment.ScrimeCompetitorFragment: sendSignal (HEIGHT_SELECTED, delayed=false)" -> {
                            startEvent("Change height")
                            endWhen="SELECT idEntry FROM entry "
                        }
                        "Fragment.ScrimeCompetitorFragment: sendSignal (RESET, delayed=false)" -> {
                            startEvent("Scrime - check again")
                            endWhen="SELECT entry.*, team.*, competitor.*"
                        }


                    }

                }
            }




            line = reader.readLine()
        }
    }
}