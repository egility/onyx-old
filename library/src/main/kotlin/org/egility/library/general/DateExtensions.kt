/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


val jsonFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
val jsonFormatNoTime = SimpleDateFormat("yyyy-MM-dd")

val SYSTEM_DATE_FORMAT = "yyyy-MM-dd"
val EXCEL_DATE_FORMAT = "dd/MM/yyyy"

val Date.dateInt: Int
    get() {
        var text = "0"
        if (this.time != 0L) {
            text = SimpleDateFormat("yyMMdd").format(this)
        }
        return Integer.parseInt(text)
    }


fun Date.format(formatString: String): String {
    return SimpleDateFormat(formatString).format(this)
}

val Date.shortText: String
    get() {
        if (this.time == 0L) {
            return "00/00/00"
        } else {
            return SimpleDateFormat("dd/MM/yy").format(this)
        }
    }

val Date.sqlDate: String
    get() = softwareDate.quotedSingle

val Date.softwareDate: String
    get() {
        if (this.time == 0L) {
            return "0000-00-00"
        } else {
            return SimpleDateFormat(SYSTEM_DATE_FORMAT).format(this)
        }
    }

val Date.fileNameDate: String
    get() {
        if (this.time == 0L) {
            return "00000000"
        } else {
            return SimpleDateFormat("yyyyMMdd").format(this)
        }
    }

val Date.fileNameDateTime: String
    get() {
        if (this.time == 0L) {
            return "00000000000000"
        } else {
            return SimpleDateFormat("yyyyMMddHHmmss").format(this)
        }
    }

val Date.usDate: String
    get() {
        if (this.time == 0L) {
            return "0000-00-00"
        } else {
            return SimpleDateFormat("yyyy-dd-MM").format(this)
        }
    }

val Date.timeSeconds: String
    get() {
        return SimpleDateFormat("HH:mm:ss").format(this)
    }

val Date.zone: String
    get() {
        return SimpleDateFormat("z").format(this)
    }

val Date.timeMinutes: String
    get() {
        return SimpleDateFormat("HH:mm").format(this)
    }

val Date.sqlDateTime: String
    get() {
        if (this.time == 0L) {
            return "0000-00-00 00:00:00".quotedSingle
        } else {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(this).quotedSingle
        }
    }

val Date.sqlDateTime2: String
    get() {
        if (this.time == 0L) {
            return "0000-00-00 00:00:00".quoted
        } else {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(this).quoted
        }
    }

val Date.quotedSqlTime: String
    get() {
        if (this.time == 0L) {
            return "00:00:00".quotedSingle
        } else {
            return SimpleDateFormat("HH:mm:ss").format(this).quotedSingle
        }
    }

fun Date.dayName(): String {
    return SimpleDateFormat("EEEE").format(this)
}

val Date.dayNameShort: String
    get() {
        return SimpleDateFormat("EEE").format(this)
    }

val Date.dateDate: String
    get() {
        return SimpleDateFormat("EEE d").format(this)
    }

val Date.dayDate: String
    get() {
        return SimpleDateFormat("EEE").format(this)
    }


fun Date.fullDate(): String {
    return SimpleDateFormat("EEEE, d MMMM").format(this)
}

fun Date.fullishDate(): String {
    return SimpleDateFormat("EEE, d MMM").format(this)
}

val Date.timeText: String
    get() {
        return SimpleDateFormat("HH:mm").format(this)
    }

fun Date.longTime(): String = SimpleDateFormat("HH:mm:ss").format(this)

val Date.fullTimeText: String
    get() = SimpleDateFormat("HH:mm:ss").format(this)

val Date.extendedTimeText: String
    get() = SimpleDateFormat("HH:mm:ss.S").format(this).substring(1, 10)

val Date.dateText: String
    get() = SimpleDateFormat("dd/MM/yyyy").format(this)

val Date.dateTextShort: String
    get() = SimpleDateFormat("dd/MM/yy").format(this)

val Date.fullDateTimeText: String
    get() = SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(this)

val Date.fullDateMinutesText: String
    get() = SimpleDateFormat("dd/MM/yyyy HH:mm").format(this)

val Date.shortDateMinutesText: String
    get() = SimpleDateFormat("dd/MM/yy HH:mm").format(this)

val Date.fullDayMinutesText: String
    get() = SimpleDateFormat("EEE HH:mm").format(this)


fun Date.isEmpty(): Boolean {
    return this.time==0L || this.time==-3600000L
}

fun Date.isNotEmpty(): Boolean {
    return !isEmpty()
}

fun Date.toJson(): String {
    if (this.dateOnly() == this) {
        return jsonFormatNoTime.format(this)
    } else {
        return jsonFormat.format(this)
    }
}

fun Date.dateOnly(): Date {
    val result = GregorianCalendar()
    result.time = this
    result.set(Calendar.HOUR_OF_DAY, 0)
    result.set(Calendar.MINUTE, 0)
    result.set(Calendar.SECOND, 0)
    result.set(Calendar.MILLISECOND, 0)
    return result.time
}

fun Date.daysSince(baseDate: Date): Int {
    val diff = this.dateOnly().time - baseDate.dateOnly().time
    return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
}

fun Date.secondsUntil(baseDate: Date): Long {
    return (baseDate.time - this.time) / 1000
}

fun Date.timeOnly(): Date {
    val calendar = GregorianCalendar()
    val result = GregorianCalendar()
    calendar.time = this
    result.set(0, 0, 0, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND))
    return result.time
}

fun Date.hourOfDay(): Int {
    val calendar = GregorianCalendar()
    calendar.time = this
    return (calendar.get(Calendar.HOUR_OF_DAY))
}

fun Date.minutesOfHour(): Int {
    val calendar = GregorianCalendar()
    calendar.time = this
    return (calendar.get(Calendar.MINUTE))
}

fun Date.addDays(days: Int): Date {
    val calendar = GregorianCalendar()
    calendar.time = this
    calendar.add(Calendar.DAY_OF_MONTH, days)
    return calendar.time
}

fun Date.addMonths(months: Int): Date {
    val calendar = GregorianCalendar()
    calendar.time = this
    calendar.add(Calendar.MONTH, months)
    return calendar.time
}

fun Date.addYears(years: Int): Date {
    val calendar = GregorianCalendar()
    calendar.time = this
    calendar.add(Calendar.YEAR, years)
    return calendar.time
}

fun Date.addHours(hours: Int): Date {
    val calendar = GregorianCalendar()
    calendar.time = this
    calendar.add(Calendar.HOUR_OF_DAY, hours)
    return calendar.time
}

fun Date.addMinutes(minutes: Int): Date {
    val calendar = GregorianCalendar()
    calendar.time = this
    calendar.add(Calendar.MINUTE, minutes)
    return calendar.time
}

fun Date.diffMinutes(otherTime: Date): Int {
    return (this.time - otherTime.time).toInt() / 1000 / 60
}

fun Date.addSeconds(seconds: Int): Date {
    val calendar = GregorianCalendar()
    calendar.time = this
    calendar.add(Calendar.SECOND, seconds)
    return calendar.time
}

fun Date.addMilliseconds(milliseconds: Int): Date {
    val calendar = GregorianCalendar()
    calendar.time = this
    calendar.add(Calendar.MILLISECOND, milliseconds)
    return calendar.time
}

fun Date.at(hour: Int, minutes: Int, seconds: Int = 0, milliseconds: Int = 0): Date {
    val calendar = GregorianCalendar()
    calendar.time = this
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, minutes)
    calendar.set(Calendar.SECOND, seconds)
    calendar.set(Calendar.MILLISECOND, milliseconds)
    return calendar.time
}

fun makeDate(year: Int, month: Int, day: Int): Date {
    val calendar = GregorianCalendar()
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month - 1)
    calendar.set(Calendar.DAY_OF_MONTH, day)

    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
}

fun Date.isSameMonth(date: Date): Boolean {
    val calendar = GregorianCalendar()
    val compare = GregorianCalendar()
    calendar.time = this
    compare.time = date
    return calendar.get(Calendar.YEAR) == compare.get(Calendar.YEAR) &&
            calendar.get(Calendar.MONTH) == compare.get(Calendar.MONTH)
}

fun Date.isSameYear(date: Date): Boolean {
    val calendar = GregorianCalendar()
    val compare = GregorianCalendar()
    calendar.time = this
    compare.time = date
    return calendar.get(Calendar.YEAR) == compare.get(Calendar.YEAR)
}

fun Date.dayOfWeek(): Int {
    val calendar = GregorianCalendar()
    calendar.time = this
    return calendar.get(Calendar.DAY_OF_WEEK)
}


