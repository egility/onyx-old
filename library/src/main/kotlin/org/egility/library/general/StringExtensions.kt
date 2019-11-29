/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

private fun escape(string: String): String {
    val builder = StringBuilder()
    for (char in string) {
        when (char.toInt()) {
            8 -> builder.append("\\b")
            9 -> builder.append("\\t")
            10 -> builder.append("\\n")
            12 -> builder.append("\\f")
            13 -> builder.append("\\r")
            34 -> builder.append("\\\"")
//            47 -> builder.append("\\/")
            92 -> builder.append("\\\\")
            in 32..256 -> builder.append(char)
            else -> {
                var hex = Integer.toHexString(char.toInt()).toUpperCase()
                hex = "0000".substring(0, 4 - hex.length) + hex
                builder.append("\\u" + hex)
            }
        }
    }
    return builder.toString()
}

fun String.unescape(): String {
    val builder = StringBuilder()
    var inEscape = false
    var escape = ""
    for (char in this) {
        when (char) {
            '\\' -> {
                if (inEscape) {
                    if (escape.isEmpty()) {
                        builder.append(92.toChar())
                    } else {
                        // invalid char
                    }
                    inEscape = false
                } else {
                    escape=""
                    inEscape = true
                }
            }
            else -> {
                if (inEscape) {
                    if (escape.isEmpty()) {
                        when (char) {
                            'b' -> builder.append(8.toChar())
                            't' -> builder.append(9.toChar())
                            'n' -> builder.append(10.toChar())
                            'f' -> builder.append(12.toChar())
                            'r' -> builder.append(13.toChar())
                            '\"' -> builder.append(34.toChar())
                            '\\' -> builder.append(92.toChar())
                            'u', '0', '1', '2' -> escape = char.toString()
                            else -> builder.append(char)
                        }
                        inEscape = escape.isNotEmpty()
                    } else {
                        escape += char
                        if (escape.startsWith("u") && escape.length==5) {
                            val hex=escape.dropLeft(1)
                            builder.append(Integer.parseInt(hex,16).toChar())
                            inEscape = false
                        } else if (!escape.startsWith("u") && escape.length==3) {
                            builder.append(escape.toIntDef(32).toChar())
                            inEscape = false
                        }
                    }
                } else {
                    builder.append(char)
                }

            }
        }
    }
    return builder.toString()
}

val String.sqlQuoted: String
    get() {
        return "`$this`"
    }

val String.escapeQuoted: String
    get() {
        return "\"" + escape(this) + "\""
    }

val String.quoted: String
    get() {
//        return "\"" + escape(this) + "\""
        return "\"${this.replace("\"", "\\\"")}\""
    }

val String.quotedSingle: String
    get() {
//        return "'" + escape(this) + "'"
        return "'" + this + "'"
    }

val String.asBarQuoted: String
    get()=this.replace('|', '"')


val String.possessive: String
    get() {
        return this + "'s"
    }

fun String.plural(): String {
    when (this) {
        "competitionDay" -> return "competitionDays"
    }
    when (this[this.length - 1]) {
        's' -> {
            return this + "es"
        }
        'y' -> {
            return this.substring(0, this.length - 1) + "ies"
        }
        else -> {
            return this + "s"
        }
    }
}

fun String.enclose(open: String="\"", close: String=""): String {
    if (close.isEmpty()) {
        return open + this + open

    } else {
        return open + this + close
    }
}

fun String.poundsToPence(): Int {
    return this.replace("£", "").replace(",", "").toDoubleDef(0.0).pence
}

fun String.append(addendum: String, delimiter: String=", ", quote: String=""): String {
    val base = this.trim()
    val trimmed = addendum.trim().enclose(quote)
    if (base == "" && trimmed != "") {
        return trimmed
    } else if (base != "" && trimmed != "") {
        return base + delimiter + trimmed
    } else if (base != "") {
        return base
    } else {
        return ""
    }
}

fun String.spaceAppend(addendum: String): String {
    return append(addendum, " ")
}

fun String.commaAppend(addendum: String): String {
    return append(addendum, ",")
}

fun String.semiColonAppend(addendum: String): String {
    return append(addendum, ";")
}

fun String.newlineAppend(addendum: String): String {
    return append(addendum, "\n")
}

fun String.delimiterInc(delimiter: String, addendum: String): String {
    val trimmed = addendum.trim()
    if (this != "" && trimmed != "") {
        return (delimiter + trimmed)
    } else {
        return trimmed
    }
}

fun String.default(default: String): String {
    return if (this.isNotEmpty()) this else default
}

val String.initials: String
get() {
    var result=""
    var last = ' '
    for (c in this) {
        if (c!=' ' && last== ' ') result += c.toUpperCase()
        last=c
    }
    return result
}

val String.naturalCase: String
    get() {
        val result = StringBuilder(this.length)

        if (this.isNotEmpty()) {
            var previous = ' '
            var previous2 = ' '
            var previous3 = ' '
            var current = ' '
            var next = Character.toUpperCase(this[0])

            for (i in 0..this.length - 1) {
                previous3 = previous2
                previous2 = previous
                previous = current
                current = next
                if (i + 1 < this.length) {
                    next = Character.toUpperCase(this[i + 1])
                } else {
                    next = ' '
                }

                if (Character.isLetter(current)) {
                    when (previous) {
                        ' ', '.', '-', '_', '\n' -> result.append(Character.toTitleCase(current))
                        '\'' -> if ((previous2 == 'D' || previous2 == 'O') && previous3 == ' ') {
                            result.append(Character.toTitleCase(current))
                        } else {
                            result.append(Character.toLowerCase(current))
                        }
                        else -> result.append(Character.toLowerCase(current))
                    }
                } else {
                    if (current != ' ' || previous != ' ') {
                        result.append(current)
                    }

                }
            }
        }

        return result.toString().trim()

    }


infix fun String.spaceAdd(addendum: String): String {
    return spaceAppend(addendum)
}

infix fun String.commaAdd(addendum: String): String {
    return append(addendum)
}

val String.initialUpper: String
    get() {
        val first = this.substring(0, 1)
        val rest = this.substring(1)
        return first.toUpperCase() + rest
    }

val String.initialLower: String
    get() {
        if (this.isEmpty()) return this
        val first = this.substring(0, 1)
        val rest = this.substring(1)
        return first.toLowerCase() + rest
    }

infix fun String.eq(str: String): Boolean {
    return this.compareTo(str, ignoreCase = true) == 0
}

infix fun String.neq(str: String): Boolean {
    return this.compareTo(str, ignoreCase = true) != 0
}

infix fun String.notSimilar(str: String): Boolean {
    return this.replace(" ", "").compareTo(str.replace(" ", ""), ignoreCase = true) != 0
}

fun String.oneOf(vararg list: String): Boolean {
    return list.contains(this)
}

fun String.indexIn(vararg list: String): Int {
    return list.indexOf(this)
}

val String.unQualify: String
    get() {
        val vElements = this.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (vElements.size > 1) {
            return vElements[1]
        } else if (vElements.size > 0) {
            return vElements[0]
        } else {
            return ""
        }
    }

fun String.dropLeft(length: Int): String {
    if (this.length< length) {
        return this
    } else {
        return this.substring(length)
    }
}

fun String.leftOf(str: String): String {
    val pos = this.indexOf(str, ignoreCase = true)
    return if (pos >= 0) this.substring(0, pos) else this
}

fun String.rightOf(str: String): String {
    val pos = this.indexOf(str, ignoreCase = true)
    return if (pos >= 0) this.substring(pos + str.length) else this
}

private enum class PositionState {IN_WHITE_SPACE, IN_STRING, IN_SINGLE, IN_DOUBLE }

val String.removeWhiteSpace: String
    get() {
        val SPACE = 32
        val DOUBLE_QUOTE = 34
        val SINGLE_QUOTE = 39

        var state = PositionState.IN_WHITE_SPACE
        var result = StringBuilder(this.length)
        for (c in this) {
            val charValue = c.toInt()
            when (charValue) {
                DOUBLE_QUOTE -> {
                    when (state) {
                        PositionState.IN_STRING -> {
                            result.append(c)
                            state = PositionState.IN_DOUBLE
                        }
                        PositionState.IN_WHITE_SPACE -> {
                            result.append(c)
                            state = PositionState.IN_DOUBLE
                        }
                        PositionState.IN_SINGLE -> {
                            result.append(c)
                        }
                        PositionState.IN_DOUBLE -> {
                            result.append(c)
                            state = PositionState.IN_STRING
                        }
                    }
                }
                SINGLE_QUOTE -> {
                    when (state) {
                        PositionState.IN_STRING -> {
                            result.append(c)
                            state = PositionState.IN_SINGLE
                        }
                        PositionState.IN_WHITE_SPACE -> {
                            result.append(c)
                            state = PositionState.IN_SINGLE
                        }
                        PositionState.IN_SINGLE -> {
                            result.append(c)
                            state = PositionState.IN_STRING
                        }
                        PositionState.IN_DOUBLE -> {
                            result.append(c)
                        }
                    }
                }
                in 0..SPACE -> {
                    when (state) {
                        PositionState.IN_STRING -> {
                            result.append(' ')
                            state = PositionState.IN_WHITE_SPACE
                        }
                        PositionState.IN_WHITE_SPACE -> {
                            /* ignore */
                        }
                        PositionState.IN_SINGLE -> {
                            result.append(c)
                        }
                        PositionState.IN_DOUBLE -> {
                            result.append(c)
                        }
                    }
                }
                else -> {
                    when (state) {
                        PositionState.IN_STRING -> {
                            result.append(c)
                        }
                        PositionState.IN_WHITE_SPACE -> {
                            result.append(c)
                            state = PositionState.IN_STRING
                        }
                        PositionState.IN_SINGLE -> {
                            result.append(c)
                        }
                        PositionState.IN_DOUBLE -> {
                            result.append(c)
                        }
                    }
                }
            }
        }
        return result.toString().trim()
    }

val dateRegex = Regex("[12][0-9]{3}-[01][0-9]-[0-3][0-9].*")

val String.isPossibleJsonDate: Boolean
    get() {
        return this.matches(dateRegex)
    }

val String.asJsonDate: Date?
    get() {
        if (!this.isPossibleJsonDate) return null
        try {
            return jsonFormat.parse(this)
        } catch (e: Throwable) {
            try {
                return jsonFormatNoTime.parse(this)
            } catch (e: Throwable) {
                return null
            }
        }
    }

val String.unQuoted: String
    get() {
        if (this.length > 2 && this[0] == '"' && this[length - 1] == '"') {
            return this.substring(1, length - 1)
        }
        return this
    }

fun String.toIntDef(default: Int = 0): Int {
    return if (this.matches(Regex("-?[0-9]+"))) Integer.parseInt(this) else default
}

fun String.toLongDef(default: Long = 0L): Long {
    try {
        return this.toLong()
    } catch (e: Throwable) {
        return default
    }
}

fun String.toDoubleDef(default: Double = 0.0): Double {
    try {
        return this.toDouble()
    } catch (e: NumberFormatException) {
        return default
    }
}


fun String.toMoneyDef(default: Int = 0): Int {
    try {
        val money = this.toDouble()
        return (money * 100).toInt()
    } catch (e: NumberFormatException) {
        return default
    }
}

fun String.countOf(match: Char): Int {
    var count = 0
    for (char in this) {
        if (char == match) {
            count++
        }
    }
    return count
}

fun String.
        toDate(format: String=SYSTEM_DATE_FORMAT): Date {
    try {
        return SimpleDateFormat(format).parse(this)
    } catch (e: Throwable) {
        return nullDate
    }
}

fun String.toDateOrNull(format: String=SYSTEM_DATE_FORMAT): Date? {
    try {
        return SimpleDateFormat(format).parse(this)
    } catch (e: Throwable) {
        return null
    }
}

val String.asCommaLine: String
    get() {
        return this.replace("\n", ", ").replace("\r", ", ").replace("  ", " ").replace(", ,", ",")
    }

val String.asMultiLine: String
    get() {
        return this.replace(", ", "\n")
    }

fun String.encrypt(keyPhrase: String): String {
    return Cryptography.encrypt(this, keyPhrase)
}

fun String.decrypt(keyPhrase: String): String {
    return Cryptography.decrypt(this, keyPhrase)
}

fun String.pad(length: Int, char: Char=' '): String {
    if (this.length>=length) {
        return substring(0, length)
    } else {
        var result = this
        while (result.length < length) {
            result += char
        }
        return result
    }
}

fun String.hexToInt(default: Int= -1): Int {
    try {
        return Integer.parseInt(this, 16)
    } catch(e: Throwable) {
        return -1
    }
}

fun String.listHas(target: String, delimiter: String=","): Boolean {
    return this.split(delimiter).contains(target)
}

fun String.listToIntArray(delimiter: String=","): ArrayList<Int> {
    val result=ArrayList<Int>()
    for (item in this.split(delimiter)) {
        val value=item.toIntDef(Integer.MIN_VALUE)
        if (value!=Integer.MIN_VALUE) {
            result.add(value)
        }
    }
    return result
}

fun String.listToStringArray(delimiter: String=","): ArrayList<String> {
    return ArrayList<String>(this.replace(" ", "").split(","))
}

fun String.listToQuotedList(delimiter: String=","): String {
    var result=""
    for (item in this.split(delimiter)) {
        result = result.append(item.quoted, delimiter)
    }
    return result
}

val String.noSpaces: String
    get() = this.replace(" ", "").replace("\t", "")

val String.upperCase: String
    get() = this.toUpperCase()


private enum class Camel {INITIAL, IN_WORD, IN_NUMBER}

fun String.camelNameToTitle(): String {
    var result=""
    var state = Camel.INITIAL
    for (char in this) {
        when (char) {
            in 'A'..'Z' -> {
                when (state) {
                    Camel.INITIAL -> {
                        result += char.toUpperCase()
                    }
                    Camel.IN_WORD, Camel.IN_NUMBER -> {
                        result+= " $char"
                    }
                }
                state=Camel.IN_WORD
            }
            in '0'..'9' -> {
                when (state) {
                    Camel.INITIAL, Camel.IN_NUMBER -> {
                        result += char
                    }
                    Camel.IN_WORD -> {
                        result+= " $char"
                    }
                }
                state=Camel.IN_NUMBER
            }
            else -> {
                when (state) {
                    Camel.INITIAL -> {
                        result += char.toUpperCase()
                    }
                    Camel.IN_WORD -> {
                        result += char
                    }
                    Camel.IN_NUMBER -> {
                        result+= " ${char.toUpperCase()}"
                    }
                }
                state=Camel.IN_WORD
            }
        }
    }


    return result

}

fun Char.isAlpha() : Boolean {
    return this.toUpperCase().toInt().between(65, 90)
}

fun String.ifEmpty(default: String): String {
    return if (this.isEmpty()) default else this
}

private enum class PairState{OUTSIDE, IN_KEY, IN_VALUE}

fun String.pairsToMap(quoted: Boolean=true): Map<String, String> {

    val result=HashMap<String, String>()
    var state=PairState.OUTSIDE
    var key=""
    var value=""
    for (c in this) {
        when (c) {
            '"' -> when (state) {
                PairState.OUTSIDE -> {
                    state = PairState.IN_KEY
                    key=""
                    value=""
                }
                PairState.IN_KEY -> {
                    key += c
                }
                PairState.IN_VALUE -> {
                    state = PairState.OUTSIDE
                    if (key.trim().isNotEmpty()) result[key.trim()]=value.trim()
                }
            }
            '=' ->when (state) {
                PairState.OUTSIDE -> {
                    state = PairState.IN_VALUE
                    key = ""
                    value = ""
                }
                PairState.IN_KEY -> {
                    state = PairState.IN_VALUE
                    value = ""
                }
                PairState.IN_VALUE -> {
                    state = PairState.OUTSIDE
                    key += c
                }
            }
            else -> when (state) {
                PairState.OUTSIDE -> {
                    doNothing()
                }
                PairState.IN_KEY -> {
                    key+=c
                }
                PairState.IN_VALUE -> {
                    value+=c
                }
            }
        }
    }
    return result
}
