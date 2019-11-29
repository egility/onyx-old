/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import java.io.BufferedReader
import java.io.FileReader

/**
 * Created by mbrickman on 13/12/17.
 */
class CommaFile(var csvFile: String) {

    enum class State{INITIAL, INQUOTE, INFIELD}

    fun forEachLine(callback: (Int, ArrayList<String>)->Unit) {
        val array=ArrayList<String>()
        val reader = BufferedReader(FileReader(csvFile))
        var lineNo=0
        var state=State.INITIAL
        var line = reader.readLine()
        while (line != null) {
            var field=""
            array.clear()
            for (c in line) {
                when (c) {
                    '"' -> {
                        when (state) {
                            State.INITIAL -> {
                                state=State.INQUOTE
                                field=""
                            }
                            State.INQUOTE -> {
                                state=State.INFIELD
                            }
                            State.INFIELD -> {
                                state=State.INQUOTE
                                field=""
                            }
                        }
                    }
                    ',' -> {
                        when (state) {
                            State.INITIAL, State.INFIELD -> {
                                array.add(field)
                                field = ""
                                state = State.INFIELD
                            }
                            State.INQUOTE -> {
                                field += c
                            }
                        }
                    }
                    else -> {
                        when (state) {
                            State.INITIAL, State.INQUOTE -> {
                                doNothing()
                            }
                            State.INQUOTE -> {
                                state = State.INQUOTE
                            }
                        }
                        field += c
                    }
                }
            }
            array.add(field)
            callback(lineNo++, array)
            line = reader.readLine()
        }
    }

}