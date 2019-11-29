/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

public enum class VarType {
    VT_ANY, VT_INTEGER, VT_REAL, VT_BOOLEAN, VT_STRING, VT_DATE, VT_BLOB, VT_NULL, VT_JSON
}

private enum class Content {
    CT_INTEGER, CT_REAL, CT_BOOLEAN, CT_STRING, CT_DATE, CT_BLOB, CT_NULL, CT_EMPTY, CT_JSON
}

class Variant {

    private var data: Any? = null
    private var dataSaved: Any? = null
    private var constant = false
    private var nullAllowed = true
    private var checkPointed = false
    private var type = VarType.VT_ANY

    var isModified: Boolean = false
        get() {
            if (isJson) {
                return json.isModified || field
            } else {
                return field
            }
        }
        private set(value) {
            if (isJson) {
                json.isModified=value
            }
            field = value
        }


    /**
     * Create a general variant that can change type
     */
    constructor() : super() {
        onCreate()
        type = VarType.VT_ANY
        constant = false
        nullAllowed = true
        data = Empty()
    }

    /**
     * Create a variant of a fixed type
     */
    constructor(type: VarType) : super() {
        onCreate()
        this.type = type
        if (type == VarType.VT_NULL) {
            data = null
            constant = true
        } else {
            data = Empty()
            constant = false
        }
        nullAllowed = true
    }

    constructor(value: Int) : this(value.toLong()) {
    }

    constructor(value: Long) : super() {
        onCreate()
        type = VarType.VT_INTEGER
        set(value)
        constant = true
        nullAllowed = false
    }

    constructor(value: Float) : this(value.toDouble()) {
    }

    constructor(value: Double) : super() {
        onCreate()
        type = VarType.VT_REAL
        set(value)
        constant = true
        nullAllowed = false
    }

    constructor(value: Boolean) : super() {
        onCreate()
        type = VarType.VT_BOOLEAN
        set(value)
        constant = true
        nullAllowed = false
    }

    constructor(value: String) : super() {
        onCreate()
        type = VarType.VT_STRING
        set(value)
        constant = true
        nullAllowed = false
    }

    constructor(value: Json) : super() {
        onCreate()
        type = VarType.VT_JSON
        set(value)
        constant = true
        nullAllowed = false
    }

    constructor(value: Date) : super() {
        onCreate()
        type = VarType.VT_DATE
        set(value)
        constant = true
        nullAllowed = false
    }

    constructor(value: ByteArray) : super() {
        onCreate()
        type = VarType.VT_BLOB
        set(value)
        constant = true
        nullAllowed = false
    }

    protected fun finalize() {
        data = null
        dataSaved = null
        varcount--
    }

    private fun onCreate() {
        varcount++
        allvarcount++
    }


    fun setEmpty(): Variant {
        if (data !is Empty) {
            if (checkPointed && !isModified) {
                dataSaved = data
            }
            data = Empty()
            isModified = true
        }
        return this
    }

    fun setNull(): Variant {
        mandate(nullAllowed || type == VarType.VT_NULL, "null value not allowed")
        if (data != null) {
            if (checkPointed && !isModified) {
                dataSaved = data
            }
            data = null
            isModified = true
        }
        return this
    }

    fun set(value: Long): Variant {
        if (type == VarType.VT_ANY || type == VarType.VT_INTEGER) {
            if (data !is Long || (data as Long).compareTo(value) != 0) {
                if (checkPointed && !isModified) {
                    dataSaved = data
                }
                data = value
                isModified = true
            }
        } else {
            when (type) {
                VarType.VT_REAL -> set(value.toDouble())
                VarType.VT_BOOLEAN -> set(value != 0L)
                VarType.VT_STRING -> set(java.lang.Long.toString(value))
                VarType.VT_JSON -> throw Wobbly("can't set JSON variant")
                VarType.VT_DATE -> set(Date(value))
                VarType.VT_BLOB -> set(value.toLong())
                else -> throw Wobbly("can't set Long variant")
            }/* ignore */
        }
        return this
    }

    fun set(value: Double): Variant {
        if (type == VarType.VT_ANY || type == VarType.VT_REAL) {
            if (data !is Double || (data as Double).compareTo(value) != 0) {
                if (checkPointed && !isModified) {
                    dataSaved = data
                }
                data = value
                isModified = true
            }
        } else {
            when (type) {
                VarType.VT_INTEGER -> set(value.toLong())
                VarType.VT_BOOLEAN -> set(value != 0.0)
                VarType.VT_STRING -> set(java.lang.Double.toString(value))
                VarType.VT_JSON -> throw Wobbly("can't set JSON variant")
                VarType.VT_DATE -> set(Date(value.toLong()))
                VarType.VT_BLOB -> set(value.toDouble())
                else -> throw Wobbly("can't set Double variant")
            }/* ignore */
        }
        return this
    }

    fun set(value: Boolean): Variant {
        if (type == VarType.VT_ANY || type == VarType.VT_BOOLEAN) {
            if (data !is Boolean || (data as Boolean).compareTo(value) != 0) {
                if (checkPointed && !isModified) {
                    dataSaved = data
                }
                data = value
                isModified = true
            }
        } else {
            when (type) {
                VarType.VT_INTEGER -> set(if (value) 1 else 0)
                VarType.VT_REAL -> set(if (value) 1.0 else 0.0)
                VarType.VT_STRING -> set(if (value) "true" else "false")
                VarType.VT_JSON -> throw Wobbly("can't set JSON variant")
                VarType.VT_DATE -> set(if (value) now else nullDate)
                VarType.VT_BLOB -> set(value)
                else -> throw Wobbly("can't set Boolean variant")
            }/* ignore */
        }
        return this
    }

    fun set(value: String): Variant {
        if (type == VarType.VT_ANY || type == VarType.VT_STRING) {
            if (data !is String || (data as String).compareTo(value) != 0) {
                if (checkPointed && !isModified) {
                    dataSaved = data
                }
                data = value
                isModified = true
            }
        } else {
            when (type) {
                VarType.VT_INTEGER ->
                    try {
                        set(Integer.parseInt(value))
                    } catch (e: Throwable) {
                        panic(e)
                    }
                VarType.VT_REAL ->
                    try {
                        set(java.lang.Double.parseDouble(value))
                    } catch (e: Throwable) {
                        panic(e)
                    }
                VarType.VT_BOOLEAN ->
                    set(java.lang.Boolean.parseBoolean(value))
                VarType.VT_JSON ->
                    set(Json(value))
                VarType.VT_DATE ->
                    try {
                        set(variantDateTime.parse(value))
                    } catch (e: Throwable) {
                        panic(e)
                    }
                VarType.VT_BLOB ->
                    set(value)
                else ->
                    throw Wobbly("can't set String variant")
            }
        }
        return this
    }

    fun setAsJson(jsonText: String) {
        type = VarType.VT_JSON
        if (checkPointed && !isModified) {
            dataSaved = data
        }
        if (!(data is Json)) {
            data = Json()
        }
        (data as Json).load(jsonText)
        isModified = true
    }
    
    fun set(value: Json): Variant {
        if (type == VarType.VT_ANY || type == VarType.VT_JSON) {
            if (data !is Json || (data as Json).toJson(compact = true).compareTo(value.toJson(compact = true)) != 0) {
                if (checkPointed && !isModified) {
                    dataSaved = data
                }
                if (!(data is Json)) {
                    data = Json()
                }
                (data as Json).assign(value)
                isModified = true
            }
        } else {
            when (type) {
                VarType.VT_STRING ->
                    try {
                        set(value.toJson(compact = true))
                    } catch (e: Throwable) {
                        panic(e)
                    }
                VarType.VT_INTEGER ->
                    try {
                        set(0)
                    } catch (e: Throwable) {
                        panic(e)
                    }
                VarType.VT_REAL ->
                    try {
                        set(0.0)
                    } catch (e: Throwable) {
                        panic(e)
                    }
                VarType.VT_BOOLEAN ->
                    set(false)
                VarType.VT_DATE ->
                    try {
                        set(nullDate)
                    } catch (e: Throwable) {
                        panic(e)
                    }
                VarType.VT_BLOB ->
                    set(value)
                else ->
                    throw Wobbly("can't set String variant")
            }
        }
        return this
    }

    fun set(value: Date): Variant {
        if (type == VarType.VT_ANY || type == VarType.VT_DATE) {
            if (data !is Date || (data as Date).compareTo(value) != 0 || removeMilliseconds(data as Date).compareTo(value) != 0) {
                if (checkPointed && !isModified) {
                    dataSaved = data
                }
                if (value == null) {
                    data = null
                } else {
                    data = removeMilliseconds(value)
                }
                isModified = true
            }
        } else {
            when (type) {
                VarType.VT_INTEGER -> try {
                    set(value.time)
                } catch (e: Throwable) {
                    panic(e)
                }

                VarType.VT_REAL -> try {
                    set(value.time / (24.0 * 60.0 * 60.0))
                } catch (e: Throwable) {
                    panic(e)
                }

                VarType.VT_STRING -> try {
                    set(variantDateTime.format(value))
                } catch (e: Throwable) {
                    panic(e)
                }

                VarType.VT_JSON -> throw Wobbly("Attempt to assign date to JSON")
                VarType.VT_BLOB -> throw Wobbly("Attempt to assign date to blob")
                else ->
                    throw Wobbly("can't set Date variant")
            }/* ignore */
        }
        return this
    }

    fun set(value: ByteArray): Variant {
        if (type == VarType.VT_ANY || type == VarType.VT_BLOB) {
            val blobValue = ByteArray(value.size)
            System.arraycopy(value, 0, blobValue, 0, value.size)
            if (checkPointed && !isModified) {
                dataSaved = data
            }
            data = blobValue
            isModified = true /* should test really */
        } else {
            throw Wobbly("Attempt to assign blob value to non-blob variant")
        }
        return this
    }

    fun set(value: Int): Variant {
        return set(value.toLong())
    }

    fun set(value: Float): Variant {
        return set(value.toDouble())
    }

    fun set(value: Variant) {
        if (value.isEmpty) {
            setEmpty()
        } else if (value.isNull) {
            setNull()
        } else if (value.isInteger) {
            set(value.long)
        } else if (value.isReal) {
            set(value.double)
        } else if (value.isString) {
            set(value.string)
        } else if (value.isBoolean) {
            set(value.boolean)
        } else if (value.isDate) {
            set(value.date)
        } else if (value.isBlob) {
            set(value.blob)
        } else if (value.isJson) {
            set(value.json)
        }
    }

    fun set(value: Any?): Variant {
        if (value == null) {
            setNull()
        } else if (value is Byte) {
            set(value.toLong())
        } else if (value is Short) {
            set(value.toLong())
        } else if (value is Int) {
            set(value.toLong())
        } else if (value is Long) {
            set(value)
        } else if (value is Number) {
            set(value.toDouble())
        } else if (value is Boolean) {
            set(value)
        } else if (value is String) {
            set(value)
        } else if (value is Date) {
            set(value)
        } else if (value is Json) {
            set(value)
        } else {
            setEmpty()
        }
        return this
    }

    fun isEqual(value: Variant, loose: Boolean = false): Boolean {
        if (isEmpty) {
            return value.isEmpty
        } else if (isNull) {
            return value.isNull
        } else if (isInteger) {
            return value.long == this.long
        } else if (isReal) {
            return value.double == this.double
        } else if (isBoolean) {
            return value.boolean == this.boolean
        } else if (isString) {
            return if (loose) value.string.trim().equals(this.string.trim(), ignoreCase = true) else value.string.equals(this.string, ignoreCase = false)
        } else if (isDate) {
            return value.date.compareTo(this.date) == 0
        } else if (isBlob) {
            return blob.hashCode() == this.blob.hashCode()
        } else if (isJson) {
            return value.json.toJson() == this.json.toJson()
        }
        return false
    }

    val isEmpty: Boolean
        get() = contentType == Content.CT_EMPTY

    val isNull: Boolean
        get() = contentType == Content.CT_NULL

    val isInteger: Boolean
        get() = contentType == Content.CT_INTEGER

    val isReal: Boolean
        get() = contentType == Content.CT_REAL

    val isBoolean: Boolean
        get() = contentType == Content.CT_BOOLEAN

    val isString: Boolean
        get() = contentType == Content.CT_STRING

    val isJson: Boolean
        get() = contentType == Content.CT_JSON

    val isDate: Boolean
        get() = contentType == Content.CT_DATE

    val isBlob: Boolean
        get() = contentType == Content.CT_BLOB

    val int: Int
        get() = long.toInt()

    val long: Long
        get() = getLong(data)
    
    val savedLong: Long
        get() = getLong(dataSaved)

    private fun getLong(data: Any?): Long {
        when (getContentType(data)) {
            Content.CT_EMPTY -> return 0
            Content.CT_NULL -> return 0
            Content.CT_INTEGER -> return (data as Long).toLong()
            Content.CT_REAL -> return Math.round(getDouble(data))
            Content.CT_BOOLEAN -> return (if (getBoolean(data)) 1 else 0).toLong()
            Content.CT_STRING -> {
                try {
                    return Integer.parseInt(getString(data)).toLong()
                } catch (e: NumberFormatException) {
                    return 0
                } catch (e: Throwable) {
                    panic(e)
                }

                return (getDate(data).time.toInt() / 1000).toLong()
            }
            Content.CT_DATE -> return (getDate(data).time.toInt() / 1000).toLong()
            Content.CT_BLOB -> return 0
            Content.CT_JSON -> return 0
        }
        return 0
    }

    val float: Float
        get() = getDouble(data).toFloat()
    val savedFloat: Float
        get() = getDouble(dataSaved).toFloat()

    val double: Double
        get() = getDouble(data)
    val savedDouble: Double
        get() = getDouble(dataSaved)

    private fun getDouble(data: Any?): Double {
        when (getContentType(data)) {
            Content.CT_EMPTY -> return 0.0
            Content.CT_NULL -> return 0.0
            Content.CT_INTEGER -> return getLong(data).toDouble()
            Content.CT_REAL -> return (data as Double).toDouble()
            Content.CT_BOOLEAN -> return (if (getBoolean(data)) 1 else 0).toDouble()
            Content.CT_STRING -> {
                try {
                    return Integer.parseInt(getString(data)).toDouble()
                } catch (e: NumberFormatException) {
                    return 0.0
                }
            }
            Content.CT_DATE -> return getDate(data).time.toDouble() / 1000
            Content.CT_BLOB -> return 0.0
            Content.CT_JSON -> return 0.0
        }
        return 0.0
    }

    val boolean: Boolean
        get() = getBoolean(data)
    val savedBoolean: Boolean
        get() = getBoolean(dataSaved)

    private fun getBoolean(data: Any?): Boolean {
        when (getContentType(data)) {
            Content.CT_EMPTY -> return false
            Content.CT_NULL -> return false
            Content.CT_INTEGER -> return getLong(data) != 0L
            Content.CT_REAL -> return getDouble(data) != 0.0
            Content.CT_BOOLEAN -> return (data as Boolean)
            Content.CT_STRING -> return getString(data) !== ""
            Content.CT_DATE -> return getDate(data).time != 0L
            Content.CT_BLOB -> return true
            Content.CT_JSON -> return true
        }
        return false
    }

    val string: String
        get() = getString(data)

    val json: Json
        get() {
            if (!(data is Json)) {
                data = Json()
            }
            return data as Json
        }



    val savedString: String
        get() = getString(dataSaved)

    private fun getString(data: Any?): String {
        when (getContentType(data)) {
            Content.CT_EMPTY -> return ""
            Content.CT_NULL -> return ""
            Content.CT_INTEGER, Content.CT_REAL, Content.CT_BOOLEAN -> return data.toString()
            Content.CT_STRING -> return (data as String).toString()
            Content.CT_DATE -> return DateFormat.getDateInstance().format(getDate(data))
            Content.CT_BLOB -> return ""
            Content.CT_JSON -> return (data as Json).toJson(compact = true)
        }
        return ""
    }

    val sql: String
        get() = getSql(data)
    val savedSql: String
        get() {
            if (isModified) {
                return getSql(dataSaved)
            } else {
                return getSql(data)
            }
        }

    private fun getSql(data: Any?): String {
        when (getContentType(data)) {
            Content.CT_EMPTY, //                return quoted("EMPTY");
            Content.CT_NULL -> return "NULL"
            Content.CT_INTEGER, Content.CT_REAL, Content.CT_BOOLEAN -> return data.toString()
            Content.CT_STRING -> return getString(data).quoted
            Content.CT_JSON -> return json.toJson(compact = true).quoted
            Content.CT_DATE -> return getDate(data).sqlDateTime
            Content.CT_BLOB -> throw Wobbly("Variant.getSql does not currently support blobs")
        }
        throw Wobbly("Variant.getSql unsupported variant type: " + type.toString())
    }

    val date: Date
        get() = getDate(data)
    val savedDate: Date
        get() = getDate(dataSaved)

    private fun getDate(data: Any?): Date {
        when (getContentType(data)) {
            Content.CT_EMPTY -> return nullDate
            Content.CT_NULL -> return nullDate
            Content.CT_INTEGER -> return Date(getLong(data) * 1000)
            Content.CT_REAL -> return Date(getDouble(data).toLong() * 1000)
            Content.CT_BOOLEAN -> return nullDate
            Content.CT_STRING -> {
                try {
                    return DateFormat.getDateInstance().parse(getString(data))
                } catch (e: ParseException) {
                    return nullDate
                } catch (e: Throwable) {
                    panic(e)
                }

                return data as Date
            }
            Content.CT_DATE -> return data as Date
            Content.CT_BLOB -> return nullDate
            Content.CT_JSON -> return nullDate
        }
        return nullDate
    }

    val blob: ByteArray
        get() = getBlob(data)
    val savedBlob: ByteArray
        get() = getBlob(dataSaved)

    fun getBlob(data: Any?): ByteArray {
        if (getContentType(data) == Content.CT_BLOB) {
            return data as ByteArray
        } else {
            return ByteArray(0)
        }
    }

    private val contentType: Content
        get() = getContentType(data)

    private val savedContentType: Content
        get() = getContentType(dataSaved)

    private fun getContentType(data: Any?): Content {
        if (data == null) {
            return Content.CT_NULL
        } else if (data is Long) {
            return Content.CT_INTEGER
        } else if (data is Double) {
            return Content.CT_REAL
        } else if (data is Boolean) {
            return Content.CT_BOOLEAN
        } else if (data is String) {
            return Content.CT_STRING
        } else if (data is Json) {
            return Content.CT_JSON
        } else if (data is Date) {
            return Content.CT_DATE
        } else if (data is ByteArray) {
            return Content.CT_BLOB
        } else {
            return Content.CT_EMPTY
        }
    }

    fun checkPoint() {
        checkPointed = true
        isModified = false
    }

    fun rollBack() {
        if (isModified) {
            if (dataSaved == null) {
                data = null
            } else if (dataSaved is Long) {
                data = dataSaved as Long?
            } else if (dataSaved is Double) {
                data = dataSaved as Double?
            } else if (dataSaved is Boolean) {
                data = dataSaved as Boolean?
            } else if (dataSaved is String) {
                data = dataSaved as String?
            } else if (dataSaved is java.sql.Date) {
                data = Date((dataSaved as Date).getTime())
            } else if (dataSaved is ByteArray) {
                val cpValue = dataSaved as ByteArray
                val blobValue = ByteArray(cpValue.size)
                System.arraycopy(cpValue, 0, blobValue, 0, cpValue.size)
                data = blobValue
            } else {
                dataSaved = Empty()
            }
        }
        isModified = false
    }

    fun commit() {
        checkPoint()
    }

    private inner class Empty

    companion object {

        var allvarcount: Long = 0
        var varcount: Long = 0

        private val variantDateTime = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")

        fun nullValue(): Variant {
            return Variant(VarType.VT_NULL)
        }

        fun emptyValue(): Variant {
            return Variant()
        }

        private val check = 0
        private val checkAll = 0

        fun log(test: String) {
            /*
        long var=Variant.varcount;
        long varAll=Variant.allvarcount;
        System.gc();
        logHeap();
        log(String.format("%s: %d/%d (%d, %d)", test, var, varAll, var-check, varAll-checkAll));
        check =var;
        checkAll =varAll;
*/
        }
    }


}