/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import org.egility.library.database.DbConnection
import org.egility.library.database.DbQuery
import java.io.*
import java.net.NetworkInterface
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Files.newBufferedReader
import java.nio.file.Paths
import java.util.*

fun panic(throwable: Throwable) {
    Global.services.panic(throwable)
}

val nullDate = Date(0)

private var hostname = ""
private var ipAddress = HashMap<String, String>()
private val random = Random()

val messages = ArrayList<String>()
val debugClasses = ArrayList<String>()
val debugExcludeClasses = ArrayList<String>()

val SINGLE_QUOTE = 39.toChar()
val DOUBLE_QUOTE = 34.toChar()
val TAB = 9.toChar()


var effectiveDate = nullDate


val cpuTime: Long
    get() = System.nanoTime() / 1000000

fun random(max: Int, min: Int = 0): Int {
    return random.nextInt(max - min + 1) + min
}

fun doNothing(vararg zilch: Any) {
    for (a in zilch) {
        if (a is Date) {
            // do nothing
        }
    }
}

fun either(a: String, b: String): String {
    return if (a.isNotEmpty()) a else b
}


val now: Date
    get() {
        if (effectiveDate == nullDate) {
            return networkDate
        } else {
            val instant = Calendar.getInstance()
            instant.time = networkDate
            val result = GregorianCalendar()
            result.time = effectiveDate
            result.set(Calendar.HOUR_OF_DAY, instant.get(Calendar.HOUR_OF_DAY))
            result.set(Calendar.MINUTE, instant.get(Calendar.MINUTE))
            result.set(Calendar.SECOND, instant.get(Calendar.SECOND))
            result.set(Calendar.MILLISECOND, instant.get(Calendar.MILLISECOND))
            return result.time
        }
    }


val machineDate: Date
    get() = Date()

private var _networkAdjustment = 0

var networkDate: Date
    get() = machineDate.addMilliseconds(_networkAdjustment)
    set(value) {
        val holdDate = machineDate
        _networkAdjustment = (value.time - holdDate.time).toInt()
        debug("utils", "_networkAdjustment=$_networkAdjustment (${value.time} - ${holdDate.time})")
    }

val realToday: Date
    get() = machineDate.dateOnly()

val realNow: Date
    get() = machineDate

val today: Date
    get() = getDay(0)

fun getDay(addDays: Int): Date {
    return now.dateOnly().addDays(addDays)
}


fun copyFile(sourceFile: File, destFile: File) {
    if (!destFile.exists()) {
        destFile.createNewFile()
    }

    val source = FileInputStream(sourceFile).channel
    val destination = FileOutputStream(destFile).channel
    try {
        destination.transferFrom(source, 0, source.size())
    } finally {
        source.close()
        destination.close()
    }
}

fun intOneOf(value: Int, vararg list: Int): Boolean {
    for (item in list) {
        if (item == value) {
            return true
        }
    }
    return false
}

fun msgYesNo(title: String, message: String, body: (Boolean) -> Unit) {
    Global.services.msgYesNo(title, message, body)
}

fun whenYes(title: String, message: String, body: () -> Unit) {
    msgYesNo(title, message) { if (it) body() }
}

fun setDebugClasses(vararg list: String) {
    debugClasses.clear()
    for (item in list) {
        debugClasses.add(item.toLowerCase())
    }
}

fun setDebugExcludeClasses(vararg list: String) {
    debugExcludeClasses.clear()
    for (item in list) {
        debugExcludeClasses.add(item.toLowerCase())
    }
}


fun debug(debugClass: String, event: String) {
    var display = true
    if (debugExcludeClasses.contains("*") || debugExcludeClasses.contains(debugClass.toLowerCase())) {
        display = false
    }
    if (debugClasses.contains("*") || debugClasses.contains(debugClass.toLowerCase())) {
        display = true
    }
    if (display) {
        val lines = event.split("\n")
        var pointer = "===>"
        for (line in lines) {
            log("${Thread.currentThread().id}: $debugClass $pointer $line")
            pointer = "......"
        }
    }
}

fun debug(debugClass: String, format: String, vararg args: Any) {
    val message = format.format(*args)
    debug(debugClass, message)
}

private fun log(event: String) {
    Global.services.log(event)
}

fun bytesToLong(bytes: ByteArray): Long {
    var result: Long = 0
    for (i in bytes.indices) {
        result = result * 256 + bytes[i]
    }
    return result
}

fun fileToString(path: String): String {
    val file = File(path)
    var result = ""
    if (file.exists()) {
        newBufferedReader(file.toPath(), StandardCharsets.UTF_8).use { reader ->
            while (true) {
                val charAsInt = reader.read()
                if (charAsInt <= 0) {
                    break
                }
                result += charAsInt.toChar()
            }
        }
        if (result.isNotEmpty() && result[result.length - 1] == '\n') {
            result = result.dropLast(1)
        }
        return result
    }
    return ""
}

fun stringToFile(path: String, string: String) {
    Files.write(Paths.get(path), string.toByteArray());
}

fun streamToString(stream: InputStream): String {
    val reader = InputStreamReader(stream)
    var charAsInt = reader.read()
    var result = ""
    while (charAsInt != -1) {
        val thisChar = charAsInt
        val next = reader.read()
        // don't terminate on a new line
        if (next != -1 || charAsInt != 10) {
            result += charAsInt.toChar()
        }
        charAsInt = next
    }
    return result
}


fun doExec(command: Array<String>, wait: Boolean = false, dir: String = ""): Process {
    val process = Runtime.getRuntime().exec(command, null, if (dir.isNotEmpty()) File(dir) else null)
    if (wait) {
        process.waitFor()
    }
    return process
}

fun exec(command: String, wait: Boolean = true, silent: Boolean = false, dir: String = ""): Int {
    val process = doExec(commandToArray(command), wait, dir)
    if (!silent) {
        debug("exec", "$command - > ${process.exitValue()}")
    }
    return process.exitValue()
}

fun execStr(command: String, wait: Boolean = true, silent: Boolean = true): String {
    var result = ""
    val process = doExec(commandToArray(command), wait)
    if (process.exitValue() != 0) {
        result = "Error ${process.exitValue()}"
    } else {
        result = streamToString(process.inputStream)
    }
    if (!silent) {
        debug("execStr", "$command - > $result")
    }
    return result
}

enum class CommandState { IN_SPACE, IN_UNQUOTED, IN_SINGLE, IN_DOUBLE }

fun commandToArray(command: String): Array<String> {
    val result = ArrayList<String>()
    var state = CommandState.IN_SPACE
    var phrase = ""
    for (char in command) {
        when (char) {
            ' ' -> {
                when (state) {
                    CommandState.IN_SINGLE, CommandState.IN_DOUBLE -> {
                        phrase += char
                    }
                    CommandState.IN_UNQUOTED -> {
                        result.add(phrase)
                        phrase = ""
                        state = CommandState.IN_SPACE
                    }
                }
            }
            SINGLE_QUOTE -> {
                when (state) {
                    CommandState.IN_SINGLE -> {
                        if (phrase.isNotEmpty()) {
                            result.add(phrase)
                        }
                        phrase = ""
                        state = CommandState.IN_SPACE
                    }
                    CommandState.IN_UNQUOTED, CommandState.IN_DOUBLE -> {
                        phrase += char
                    }
                    CommandState.IN_SPACE -> {
                        state = CommandState.IN_SINGLE
                    }
                }
            }
            DOUBLE_QUOTE -> {
                when (state) {
                    CommandState.IN_DOUBLE -> {
                        if (phrase.isNotEmpty()) {
                            result.add(phrase)
                        }
                        phrase = ""
                        state = CommandState.IN_SPACE
                    }
                    CommandState.IN_UNQUOTED, CommandState.IN_SINGLE -> {
                        phrase += char
                    }
                    CommandState.IN_SPACE -> {
                        state = CommandState.IN_DOUBLE
                    }
                }
            }
            else -> {
                when (state) {
                    CommandState.IN_UNQUOTED, CommandState.IN_SINGLE, CommandState.IN_DOUBLE -> {
                        phrase += char
                    }
                    CommandState.IN_SPACE -> {
                        phrase = char.toString()
                        state = CommandState.IN_UNQUOTED
                    }
                }
            }
        }
    }
    if (phrase.isNotEmpty()) {
        result.add(phrase)
    }
    return result.toTypedArray()
}


fun getHostname(): String {
    if (hostname.isEmpty()) {
        val OS = System.getProperty("os.name").toLowerCase()
        if (OS.contains("win")) {
            hostname = System.getenv("COMPUTERNAME")
        } else {
            if (OS.contains("linux")) {
                try {
                    hostname = execStr("hostname")
                } catch (e: IOException) {
                    panic(e)
                }

            }
        }
    }
    return hostname
}

fun getUptime(): Int {
    try {
        val uptime = fileToString("/proc/uptime")
        val seconds = uptime.leftOf(".").toIntDef(-1)
        return seconds
    } catch (e: IOException) {
        return -2
    }
}

var _bootCount = -1
fun getBootCount(): Int {
    if (_bootCount == -1) {
        try {
            val bootCount = fileToString("/data/bootcount")
            _bootCount = bootCount.toIntDef(-2)
        } catch (e: IOException) {
            return -3
        }
    }
    return _bootCount
}

fun getIpAddress2(interfaceName: String): String {
    if (ipAddress[interfaceName]?.isEmpty() ?: true || ipAddress[interfaceName] == interfaceName + "???") {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in Collections.list(interfaces)) {
                if (networkInterface.displayName == interfaceName) {
                    val addresses = networkInterface.inetAddresses
                    for (address in Collections.list(addresses)) {
                        if (address.address.size == 4) {
                            ipAddress[interfaceName] = address.hostAddress
                            return ipAddress[interfaceName] ?: ""
                        }
                    }
                }
            }
            ipAddress[interfaceName] = interfaceName + "???"
        } catch (e: Throwable) {
            panic(e)
            ipAddress[interfaceName] = ""
        }

    }
    return ipAddress[interfaceName] ?: interfaceName+"???"
}

fun getIpAddress(interfaceName: String): String {
    val interfaces = NetworkInterface.getNetworkInterfaces()
    for (networkInterface in Collections.list(interfaces)) {
        debug("getIpAddress", "${networkInterface.displayName}")
        if (networkInterface.displayName == interfaceName) {
            val addresses = networkInterface.inetAddresses
            for (address in Collections.list(addresses)) {
                if (address.address.size == 4) {
                    return address.hostAddress
                }
            }
        }
    }
    return interfaceName+"???"
}

fun getMacAddress(interfaceName: String): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (networkInterface in Collections.list(interfaces)) {
            debug("getMacAddress", "${networkInterface.displayName}")
            if (networkInterface.displayName == interfaceName) {

                val mac = networkInterface.hardwareAddress

                val builder = StringBuilder()
                for (i in mac.indices) {
                    builder.append("%02x%s".format(mac[i], if ((i < mac.size - 1)) ":" else ""))
                }
                debug("getMacAddress", "${networkInterface.displayName} Mac Address: ${builder.toString()}")
                return builder.toString()
            }
        }
    } catch (e: SocketException) {
    }

    debug("getMacAddress", "Interface $interfaceName not found")
    return ""
}

fun setBit(set: Int, bit: Int): Int {
    return set or (1 shl bit)
}

fun resetBit(set: Int, bit: Int): Int {
    return set and (1 shl bit).inv()
}

fun isBitSet(set: Int, bit: Int): Boolean {
    return (set and (1 shl bit)) != 0
}

fun crash1(): Int {
    return 10 / 0
}

fun crash2(): Int {
    try {
        return 10 / 0
    } catch (e: Throwable) {
        panic(e)
        return 0
    }

}

fun removeMilliseconds(date: Date): Date {
    val time = date.time / 1000 * 1000
    return Date(time)
}

fun clearMessages() {
    messages.clear()
}

fun addMessage(message: String) {
    messages.add(message)
}

fun addMessage(message: String, vararg args: Any) {
    messages.add(message.format(*args))
}


fun mandate(condition: Boolean, message: String) {
    if (!condition) {
        val stack = Thread.currentThread().stackTrace
        var className = stack[3].className
        className = className.substring(className.lastIndexOf('.') + 1)
        val calledFrom = className + '.' + stack[3].methodName
        throw Wobbly(message)
    }
}

fun mandate(condition: Boolean, format: String, vararg args: Any) {
    if (!condition) {
        val stack = Thread.currentThread().stackTrace
        var className = stack[3].className
        className = className.substring(className.lastIndexOf('.') + 1)
        val calledFrom = className + '.' + stack[3].methodName
        throw Wobbly(calledFrom + ": " + format.format(*args))
    }
}

fun <T> debugTime(debugClass: String, event: String, body: () -> T): T {
    var timeStamp = System.nanoTime()
    try {
        return body()
    } finally {
        timeStamp = System.nanoTime() - timeStamp
        debug(debugClass + " Completed", "$event, Time taken = %.1f msec", (timeStamp.toDouble()) / 1000000.0)
    }
}

fun <T> timeTaken(event: String, body: () -> T): T {
    var timeStamp = System.nanoTime()
    try {
        return body()
    } finally {
        timeStamp = System.nanoTime() - timeStamp
        println("$event, Time taken = %.1f msec".format(timeStamp.toDouble() / 1000000.0))
    }
}

fun dbTransaction(connection: DbConnection = Global.connection, body: () -> Unit) {
    connection.transaction(body)
}

fun dbExecute(sql: String, connection: DbConnection = Global.connection) {
    connection.execute(sql)
}


fun dbQuery(
    sql: String = "",
    connection: DbConnection? = null,
    silent: Boolean = false,
    whenFetched: (DbQuery.() -> Unit)? = null,
    body: DbQuery.() -> Unit
) {
    val q = DbQuery(sql, connection, silent)
    if (whenFetched != null) {
        whenFetched(q)
    }
    q.withEach(body)
}


fun arrayOfString(vararg strings: String): ArrayList<String> {
    val x = intArrayOf(1, 2)
    val result = ArrayList<String>()
    result.addAll(strings)
    return result
}

open class DelimitedList(val delimiter: String) {

    var list = ArrayList<String>()

    val size: Int
        get() = list.size

    fun add(item: String) {
        val trimmed = item.trim()
        if (trimmed != "") {
            list.add(trimmed)
        }
    }

    override fun toString(): String {
        var result = ""
        for (item in list) {
            if (result == "") {
                result = item
            } else {
                result += delimiter + item
            }
        }
        return result
    }

    fun isEmpty() = list.size == 0

}

fun popUp(title: String, message: String) {
    Global.services.popUp(title, message)
}

fun buildDate(year: Int, month: Int, day: Int): Date {
    return GregorianCalendar(year, month - 1, day).time
}

fun buildDate(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Date {
    return GregorianCalendar(year, month - 1, day, hour, minute, second).time
}

class CommaList() : DelimitedList(", ")
class NewlineList() : DelimitedList("\n")
class SpaceList() : DelimitedList(" ")

data class ButtonData(var enabled: Boolean, var underlined: Boolean, var caption: String)

data class MenuItem(val selector: String, val title: String = "", val tag: Int = 0)

fun convertStreamToString(inputStream: java.io.InputStream): String {
    val result = StringBuffer()
    val reader = InputStreamReader(inputStream)


    val buffer = BufferedReader(reader)

    var line = buffer.readLine()
    while (line != null) {
        result.append(line + "\n")
        line = buffer.readLine()
    }
    inputStream.close()
    return result.toString()
}


fun stopService(service: String) {
    exec("systemctl stop $service")
}

fun startService(service: String) {
    exec("systemctl start $service")
}

fun restartService(service: String) {
    exec("systemctl restart $service")
}


fun macToLong(mac: String): Long {
    var hex = ""
    for (char in mac) {
        if (char != ':') {
            hex += char
        }
    }
    return java.lang.Long.parseLong(hex, 16)
}

fun longToMac(long: Long): String {
    var mac = ""
    var _long = long
    for (i in 1..6) {
        val byte = _long and 0xff
        mac = "%02x".format(byte) + if (mac.isNotEmpty()) ":" + mac else ""
        _long = _long.shr(8)
    }
    return mac
}

fun argsToMap(args: Array<String>): Map<String, String> {
    val result = HashMap<String, String>()
    for (arg in args) {
        val parts = arg.split("=")
        val switch = parts[0]
        val value = if (parts.size > 1) parts[1] else ""
        result[switch] = value
    }

    val debug = result["--debug"]
    if (debug != null) {
        debugClasses.addAll(debug.toLowerCase().split(","))
    }

    return result
}

fun generateId(max: Int = Int.MAX_VALUE, min: Int = Int.MAX_VALUE / 2): Int {
    return random(max, min)
}

fun prepareFile(path: String): File {
    val folder = File(path).parent
    File(folder).mkdirs()
    return File(path)
}

class ChangeMonitor<T>(var value: T) {
    
    fun hasChanged(newValue: T): Boolean {
        if (newValue != value) {
            value = newValue
            return true
        }
        return false
    }

    fun isSame(value: T): Boolean {
        return value == this.value
    }

    fun whenChange(newValue: T, body: () -> Unit) {
        if (newValue != value) {
            value = newValue
            body()
        }
    }
}

fun calcSurcharge(pence: Int, fixedFee: Int, rate: Double): Int {
    return Math.round((pence + fixedFee).toDouble() / (1.0 - rate) - pence.toDouble()).toInt()
}

fun calcFee(pence: Int, fixedFee: Int, rate: Double): Int {
    return Math.round(pence.toDouble() * rate).toInt() + fixedFee
}

fun calcDDSurcharge(pence: Int, minFee: Int, rate: Double): Int {
    val minFeeLimit = Math.round(minFee.toDouble() * (1.0 - rate) / rate).toInt()
    if (pence > minFeeLimit) {
        return Math.round(pence.toDouble() / (1.0 - rate) - pence.toDouble()).toInt()
    } else {
        return minFee
    }
}

fun calcDDFee(pence: Int, minFee: Int, rate: Double): Int {
    val fee = Math.round(pence.toDouble() * rate).toInt()
    return if (fee < minFee) minFee else fee
}

val base = "WVRYKTJCNBDHFUEPXASMZG"

fun generateBaseCode(letters: Int = 6): String {
    var result = ""
    for (i in 0..letters - 1) {
        result += base[random(base.length - 1, 0)]
    }
    return result
}


fun intToBase(number: Int): String {
    var result = ""
    var remain = number
    while (remain >= base.length) {
        val digit = remain.rem(base.length)
        result = base[digit] + result
        remain = remain / base.length
    }
    result = base[remain] + result
    return result

}

fun baseToInt(code: String): Int {
    var result = 0
    for (c in code) {
        val digit = base.indexOf(c)
        result = result * base.length + digit
    }
    return result
}

fun firstNotEmptyString(vararg strings: String): String {
    for (string in strings) {
        if (string.isNotEmpty()) return string
    }
    return ""
}

fun firstNonZero(vararg ints: Int): Int {
    for (int in ints) {
        if (int != -0) return int
    }
    return 0
}

fun loop(times: Int, body: (Int) -> Unit) {
    for (i in 1..times) {
        body(i)
    }
}

fun minInt(vararg values: Int): Int {
    var lowest = values[0]
    for (i in 1..values.size-1) {
        if (values[i]<lowest) lowest=values[i]
    }
    return lowest
}