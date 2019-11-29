/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import org.egility.library.database.DbDatasetCursor
import org.egility.library.database.DbTable
import java.io.*
import java.lang.StringBuilder
import java.sql.Types
import java.util.*
import kotlin.reflect.KProperty

data class JsonText(val text: String)

data class dumpString(var string: String = "", var index: Int = 0)

interface CharStack {
    fun unPop()
    fun pop(): Char
    val isEmpty: Boolean
    val dump: dumpString

    val surplus: String
        get() {
            var result = ""
            while (!isEmpty) {
                val char = pop()
                when (char) {
                    '\t', '\n', '\r', ' ' -> {
                        // ignore
                    }
                    else -> {
                        result += char
                    }
                }
            }
            return result
        }

}

class ReaderStack(val reader: Reader) : CharStack {
    var buffer = CharArray(2560)
    var bufferLength = 0
    var bufferIndex = 0
    var eos = false
    var cycle = CharArray(2560)
    var cycleLength = 0
    var cycleIndex = 0
    var lastIndex = 0

    override val isEmpty: Boolean
        get() {
            topUpBuffer()
            return cycleIndex >= cycleLength && eos
        }

    override fun unPop() {
        cycleIndex--
    }

    override fun pop(): Char {
        if (!isEmpty) {
            if (cycleIndex >= cycleLength) {
                cycle[cycleLength % 256] = buffer[bufferIndex++]
                cycleLength++
            }
            val char = cycle[cycleIndex % 256]
            lastIndex = cycleIndex
            cycleIndex++
            return char
        } else {
            return 0.toChar()
        }
    }

    fun topUpBuffer() {
        if (!eos && bufferIndex >= bufferLength) {
            bufferLength = reader.read(buffer)
            bufferIndex = 0
            eos = bufferLength == -1
        }
    }

    override val dump: dumpString
        get () {
            val result = dumpString()

            val saveIndex = cycleIndex
            val point = lastIndex
            cycleIndex -= 100
            if (cycleIndex < 0) {
                cycleIndex = 0
            }
            while (cycleIndex < saveIndex + 20 && !isEmpty) {
                if (cycleIndex == point) {
                    result.index = result.string.length - 1
                    result.string += "#" + pop() + "#"
                } else {
                    result.string += pop()
                }
            }
            cycleIndex = saveIndex
            return result
        }

}

class StringStack(val string: String) : CharStack {
    var index = 0

    override val isEmpty: Boolean
        get() = index >= string.length

    override fun unPop() {
        if (index>0) index--
    }

    override fun pop(): Char {
        return if (!isEmpty) {
            string[index++]
        } else {
            0.toChar()
        }
    }

    override val dump: dumpString
        get () {
            return dumpString(string, index)
        }
}

class Unexpected(val stack: CharStack, val _message: String) : Exception() {

    var dump = dumpString()

    init {
        dump = stack.dump
    }


    override val message: String
        get() {
            return _message + " - " + dump.string
        }

    override fun toString(): String {
        return this.message
    }
}

internal enum class JsonType { UNDEFINED, NULL, LONG, DOUBLE, STRING, BOOLEAN, DATE, ARRAY, OBJECT }

open class Json() : JsonNode(null, "", JsonType.OBJECT) {

    var checkPoint = this.toJson(compact = true)

    var isModified: Boolean
        get() {
            val j1 = checkPoint
            val j2 = this.toJson(compact = true)
            val result = j2 != j1
            return result
        }
        set(value) {
            if (value == false) {
                checkPoint = this.toJson(compact = true)
            }
        }

    override var isReadOnly = false

    private constructor(type: JsonType) : this() {
        _type = type
    }

    constructor(json: String) : this() {
        val stack = StringStack(json)
        this.parseStack(stack)
        checkPoint = toJson(compact = true)
    }

    constructor(json: JsonNode) : this() {
        this.setValue(json)
        checkPoint = toJson(compact = true)
    }

    constructor(file: File) : this() {
        this.parseStack(ReaderStack(FileReader(file)))
        checkPoint = toJson(compact = true)
    }

    constructor(stream: InputStream) : this() {
        this.parseStack(ReaderStack(InputStreamReader(stream)))
        checkPoint = toJson(compact = true)
    }

    fun assign(json: Json) {
        this.clear()
        this.parseStack(StringStack(json.toJson()))
    }

    fun load(json: String) {
        this.clear()
        this.parseStack(StringStack(json))
    }


    /*
    fun assign(json: Json) {
        this.clear()
        this.setValue(json)
    }
    */

    fun parseStack(stack: CharStack) {
        while (!stack.isEmpty) {
            val char = stack.pop()
            when (char) {
                '\t', '\n', '\r', ' ' -> {
                    // ignore
                }
                '{' -> {
                    this._type = JsonType.OBJECT
                    this.parseObject(stack)
                    //this.isReadOnly = true
                    val surplus = stack.surplus
                    if (surplus.isNotEmpty()) {
                        throw Unexpected(stack, "Json: Unexpected characters after end of array ($surplus)")
                    }
                }
                '[' -> {
                    this._type = JsonType.ARRAY
                    this.parseArray(stack)
                    //this.isReadOnly = true
                    val surplus = stack.surplus
                    if (surplus.isNotEmpty()) {
                        throw Unexpected(stack, "Json: Unexpected characters after end of object ($surplus)")
                    }
                }
                else -> {
                    throw Unexpected(stack, "Unexpected character ($char) in JSON")
                }
            }
        }
    }

    companion object {

        fun undefined() = Json(JsonType.UNDEFINED)
        fun nullNode() = JsonNode(null, "", JsonType.NULL)
        val ROOT = "*"

    }

}


open class JsonNode internal constructor(val _parent: JsonNode?, var name: String, protected var _type: JsonType) : Collection<JsonNode>, JsonContainer {

    init {
    }

    override fun getJson(): JsonNode {
        return this
    }

    private var _string: String = ""
    private var _long: Long = 0L
    private var _double: Double = 0.0
    private var _boolean: Boolean = false
    private var _date: Date = nullDate
    private var children: ArrayList<JsonNode> = ArrayList()
    private var hasValue = _type != JsonType.UNDEFINED

    val root: JsonNode
        get() = if (_parent == null) this else _parent.root

    var _isReadOnly = false
    open var isReadOnly: Boolean
        get() = root._isReadOnly 
        set(value) {
            root._isReadOnly=value
        }

    fun clear() {
        mandate(!isReadOnly, "Json: Attempt to clear() for read only object")
        _type = JsonType.NULL
        _string = ""
        _long = 0L
        _double = 0.0
        _boolean = false
        _date = nullDate
        synchronized(children) {
            children = ArrayList()
        }
    }

    private var type: JsonType
        get() = _type
        set(value) {
            mandate(!isReadOnly, "Json: Attempt to set type for read only object")
            if (value != _type) {
                clear()
            }
            _type = value
        }

    override val size: Int
        get() {
            synchronized(children) {
                return children.size
            }
        }

    override fun contains(element: JsonNode): Boolean {
        synchronized(children) {
            return children.contains(element)
        }
    }

    override fun containsAll(elements: Collection<JsonNode>): Boolean {
        synchronized(children) {
            return children.containsAll(elements)
        }
    }

    override fun isEmpty(): Boolean {
        synchronized(children) {
            return children.isEmpty()
        }
    }

    override fun iterator(): Iterator<JsonNode> {
        synchronized(children) {
            return children.iterator()
        }
    }

    fun retrieve(name: String): JsonNode? {
        synchronized(children) {
            if (type == JsonType.OBJECT) {
                for (child in children) {
                    if (child.name == name) {
                        return child
                    }
                }
            }
            return null
        }
    }

    fun drop(name: String) {
        synchronized(children) {
            val node = retrieve(name)
            if (node != null) {
                children.remove(node)
            }
        }
    }

    fun sort(comparator: Comparator<JsonNode>) {
        synchronized(children) {
            if (type == JsonType.ARRAY) {
                Collections.sort(children, comparator)
            }
        }
    }

    fun sortBy(vararg names: String, descending: Boolean=false) {
        sort(Comparator { a, b ->
            val node1 = if (!descending) a else b
            val node2 = if (!descending) b else a
            var result = 0
            if (node1 != null && node2 != null) {
                names@ for (name in names) {
                    var _name=name
                    var descending=false
                    if (name.startsWith("-")) {
                        _name=name.dropLeft(1)
                        descending=true

                    }
                    val item1 = node1.retrieve(_name)
                    val item2 = node2.retrieve(_name)
                    if (item1 != null && item2 != null && item1.type == item2.type) {
                        when (item1.type) {
                            JsonType.LONG -> result = item1.asInt.compareTo(item2.asInt)
                            JsonType.DOUBLE -> result = item1.asDouble.compareTo(item2.asDouble)
                            JsonType.STRING -> result = item1.asString.compareTo(item2.asString)
                            JsonType.BOOLEAN -> result = item1.asBoolean.compareTo(item2.asBoolean)
                            JsonType.DATE -> result = item1.asDate.compareTo(item2.asDate)
                            else -> result = 0
                        }
                    }
                    if (descending) result = - result
                    if (result != 0) {
                        break@names
                    }
                }
            }
            result
        })
    }

    val isObject: Boolean
        get() = type == JsonType.OBJECT

    val isArray: Boolean
        get() = type == JsonType.ARRAY

    val isNull: Boolean
        get() = type == JsonType.NULL

    val isNotNull: Boolean
        get() = type != JsonType.NULL

    val isUndefined: Boolean
        get() = type == JsonType.UNDEFINED

    val isDefined: Boolean
        get() = !isUndefined

    val isRoot: Boolean
        get() = root == null || this == root

    val asString: String
        get() = toString("")

    fun arrayToDelimitedString(name: String, delimiter: String): String {
        if (!has(name) || !get(name).isArray) {
            return ""
        }
        var result = ""
        for (element in get(name)) {
            result = result.append(element.asString, delimiter)
        }
        return result
    }

    fun delimitedStringToArray(string: String, name: String, delimiter: String) {
        val items = string.split(delimiter)
        this[name].clear()
        for (item in items) {
            this[name].addElement().setValue(item)
        }
    }

    fun toString(default: String = ""): String {
        when (type) {
            JsonType.UNDEFINED, JsonType.NULL -> return default
            JsonType.LONG -> return _long.toString()
            JsonType.DOUBLE -> return _double.toString()
            JsonType.STRING -> return _string
            JsonType.BOOLEAN -> return if (_boolean) "true" else "false"
            JsonType.DATE -> return _date.toJson()
            JsonType.ARRAY -> return "?Array?"
            JsonType.OBJECT -> return "?Object?"
        }
    }

    val asInt: Int
        get() = toInt(0)

    fun toInt(default: Int = 0): Int {
        when (type) {
            JsonType.UNDEFINED, JsonType.NULL -> return default
            JsonType.LONG -> return _long.toInt()
            JsonType.DOUBLE -> return _double.toInt()
            JsonType.STRING -> return _string.trim().toIntDef(default)
            JsonType.BOOLEAN -> return if (_boolean) 1 else default
            JsonType.DATE -> return _date.time.toInt()
            JsonType.ARRAY -> return default
            JsonType.OBJECT -> return default
        }
    }

    val asLong: Long
        get() {
            when (type) {
                JsonType.UNDEFINED, JsonType.NULL -> return 0
                JsonType.LONG -> return _long
                JsonType.DOUBLE -> return _double.toLong()
                JsonType.STRING -> return _string.toLongDef(0)
                JsonType.BOOLEAN -> return if (_boolean) 1 else 0
                JsonType.DATE -> return _date.time
                JsonType.ARRAY -> return 0
                JsonType.OBJECT -> return 0
            }
        }

    val asDouble: Double
        get() {
            when (type) {
                JsonType.UNDEFINED, JsonType.NULL -> return 0.0
                JsonType.LONG -> return _long.toDouble()
                JsonType.DOUBLE -> return _double
                JsonType.STRING -> return _string.toDoubleDef(0.0)
                JsonType.BOOLEAN -> return if (_boolean) 1.0 else 0.0
                JsonType.DATE -> return _date.time.toDouble()
                JsonType.ARRAY -> return 0.0
                JsonType.OBJECT -> return 0.0
            }

        }
    val asBoolean: Boolean
        get() {
            when (type) {
                JsonType.UNDEFINED, JsonType.NULL -> return false
                JsonType.LONG -> return _long != 0L
                JsonType.DOUBLE -> return _double != 0.0
                JsonType.STRING -> return _string.eq("true")
                JsonType.BOOLEAN -> return _boolean
                JsonType.DATE -> return _date.time != nullDate.time
                JsonType.ARRAY -> return false
                JsonType.OBJECT -> return false
            }


        }

    val isDate
        get() = asDate.isNotEmpty()

    val isValidDate
        get() = type == JsonType.DATE || asDate.isNotEmpty()

    val asDate: Date
        get() {
            when (type) {
                JsonType.UNDEFINED, JsonType.NULL -> return nullDate
                JsonType.LONG -> return Date(_long)
                JsonType.DOUBLE -> return Date(_double.toLong())
                JsonType.STRING -> return stringToDate(_string)
                JsonType.BOOLEAN -> return if (_boolean) Date() else nullDate
                JsonType.DATE -> return _date
                JsonType.ARRAY -> return nullDate
                JsonType.OBJECT -> return nullDate
            }
        }

    fun addElement(): JsonNode {
        mandate(!isReadOnly, "Json: Attempt to addElement() when read only")
        mandate(!isUndefined, "Json: Attempt to addElement() when undefined")
        type = JsonType.ARRAY
        synchronized(children) {
            val child = JsonNode(this, children.size.toString(), JsonType.NULL)
            children.add(child)
            return child
        }
    }

    fun addElement(child: JsonNode): JsonNode {
        mandate(!isReadOnly, "Json: Attempt to addElement() when read only")
        mandate(!isUndefined, "Json: Attempt to addElement() when undefined")
        type = JsonType.ARRAY
        synchronized(children) {
            children.add(child)
            return child
        }
    }

    fun setIntArrayFromList(commaList: String) {
        clear()
        val list = commaList.split(",")
        for (item in list) {
            val int = item.toIntDef(Integer.MIN_VALUE)
            if (int > Integer.MIN_VALUE) {
                addElement().setValue(int)
            }
        }
    }

    fun setStringArrayFromList(commaList: String) {
        clear()
        val list = commaList.split(",")
        for (item in list) {
            addElement().setValue(item)
        }
    }

    fun isEq(value: Any): Boolean {
        when (type) {
            JsonType.UNDEFINED, JsonType.NULL -> return false
            JsonType.LONG -> return (value is Long && value == _long) || (value is Int && value.toLong() == _long)
            JsonType.DOUBLE -> return (value is Double && value == _double)
            JsonType.STRING -> return (value is String && value == _string)
            JsonType.BOOLEAN -> return (value is Boolean && value == _boolean)
            JsonType.DATE -> return (value is Date && value == _date)
            JsonType.ARRAY -> return false
            JsonType.OBJECT -> return false
        }

    }

    fun searchElement(key: String, value: Any, create: Boolean = true, onCreate: ((JsonNode) -> Unit)? = null): JsonNode {
        for (child in children) {
            if (child.has(key) && child[key].isEq(value)) {
                return child
            }
        }
        if (create) {
            val new = addElement()
            new[key] = value
            if (onCreate != null) onCreate(new)
            return new
        } else {
            return Json.nullNode()
        }
    }

    fun hasElement(key: String, value: Any): Boolean {
        return (!searchElement(key, value, create = false).isNull)
    }

    operator fun get(name: String): JsonNode {
        synchronized(children) {
            val thisName = name.substringBefore(".")
            val childName = name.substringAfter(".", "")
            val isIndex= thisName.matches(intRegex)
            val thisIndex =  if (isIndex) thisName.toInt() else -1

            if (thisName == Json.ROOT) {
                return this
            } else if (!isReadOnly) {
                if (thisIndex != -1) {
                    type = JsonType.ARRAY
                    while (children.size < thisIndex + 1) {
                        addElement()
                    }
                    val child = children[thisIndex]
                    return if (childName.isEmpty()) child else child.get(childName)
                } else {
                    type = JsonType.OBJECT
                    for (child in children) {
                        if (child.name == thisName) {
                            return if (childName.isEmpty()) child else child.get(childName)
                        }
                    }
                    val child = JsonNode(this, thisName, JsonType.NULL)
                    children.add(child)
                    return if (childName.isEmpty()) child else child.get(childName)
                }
            } else if (type == JsonType.OBJECT && thisIndex == -1) {
                for (child in children) {
                    if (child.name == thisName) {
                        return if (childName.isEmpty()) child else child.get(childName)
                    }
                }
            } else if (type == JsonType.ARRAY && thisIndex != -1 && thisIndex < children.size) {
                val child = children[thisIndex]
                return if (childName.isEmpty()) child else child.get(childName)
            }
            return Json.undefined()
        }
    }

    fun has(name: String): Boolean {
        synchronized(children) {
            val thisName = name.substringBefore(".")
            val childName = name.substringAfter(".", "")
            val thisIndex = thisName.toIntDef(-1)

            if (thisName == Json.ROOT) {
                return true
            } else if (type == JsonType.OBJECT && thisIndex == -1) {
                for (child in children) {
                    if (child.name == thisName) {
                        return if (childName.isEmpty()) true else child.has(childName)
                    }
                }
            } else if (type == JsonType.ARRAY && thisIndex != -1 && thisIndex < children.size) {
                val child = children[thisIndex]
                return if (childName.isEmpty()) true else child.has(childName)
            }
            return false
        }
    }

/*
    fun has(name: String): Boolean {
        synchronized(children, {
            val thisName = name.substringBefore(".")
            val childName = name.substringAfter(".", "")

            if (type == JsonType.OBJECT) {
                for (child in children) {
                    if (child.name == thisName) {
                        return if (childName.isEmpty()) true else child.has(childName)
                    }
                }
            }
            return false
        })
    }
*/

    fun getNode(path: String): JsonNode {
        var result = this
        val elements = path.split(".")
        for (element in elements) {
            result = result[element]
        }
        return result
    }

    operator fun get(index: Int): JsonNode {
        synchronized(children) {
            if (index == -1) {
                return this
            } else if (!isReadOnly) {
                type = JsonType.ARRAY
                while (children.size < index + 1) {
                    addElement()
                }
                return children.get(index)

            } else if (type == JsonType.ARRAY && index < children.size - 1) {
                return children.get(index)
            }
            return Json.undefined()
        }
    }

    fun compress() {
        synchronized(children) {
            val remove = ArrayList<JsonNode>()
            children.forEach {
                it.compress()
                if (it.isNull || ((it.isArray || it.isObject) && it.children.size == 0)) {
                    remove.add(it)
                }
            }
            remove.forEach {
                children.remove(it)
            }
        }
    }

    fun select(path: String): JsonNode {
        val elements = path.split(".")
        var result = this
        for (element in elements) {
            val index = element.toIntDef(-1)
            if (index > -1) {
                result = result[index]
            } else {
                result = result[element]
            }
        }
        return result
    }


    operator fun set(name: String, value: Any) {
        get(name).setValue(value)
    }

    operator fun set(index: Int, value: Any) {
        get(index).setValue(value)
    }

    fun setValue(value: Any) {
        mandate(!isReadOnly, "Json: Attempt to setValue when read only")
        when (value) {
            is JsonText -> {
                parse(ReaderStack(StringReader(value.text)))
            }
            is JsonNode -> {
                synchronized(children) {
                    mandate(!isRoot || value.isArray || value.isObject || value.isNull, "Json: Attempt to set inappropriate json value to root")
                    when (value.type) {
                        JsonType.NULL -> clear()
                        JsonType.LONG -> setValue(value._long)
                        JsonType.DOUBLE -> setValue(value._double)
                        JsonType.STRING -> setValue(value._string)
                        JsonType.BOOLEAN -> setValue(value._boolean)
                        JsonType.DATE -> setValue(value._date)
                        JsonType.OBJECT -> {
                            clearObject()
                            for (child in value.children) {
                                get(child.name).setValue(child)
                            }
                        }
                        JsonType.ARRAY -> {
                            clearArray()
                            for (child in value.children) {
                                addElement().setValue(child)
                            }
                        }
                        JsonType.UNDEFINED -> throw Wobbly("Json: Attempt to set value to undefined")
                    }
                }
            }
            is String -> {
                mandate(!isRoot, "Json: Attempt to setValue to root")
                type = JsonType.STRING
                _string = value
            }
            is Double -> {
                mandate(!isRoot, "Json: Attempt to setValue to root")
                type = JsonType.DOUBLE
                _double = value
            }
            is Int -> {
                mandate(!isRoot, "Json: Attempt to setValue to root")
                type = JsonType.LONG
                _long = value.toLong()
            }
            is Long -> {
                mandate(!isRoot, "Json: Attempt to setValue to root")
                type = JsonType.LONG
                _long = value
            }
            is Boolean -> {
                mandate(!isRoot, "Json: Attempt to setValue to root")
                type = JsonType.BOOLEAN
                _boolean = value
            }
            is Date -> {
                mandate(!isRoot, "Json: Attempt to setValue to root")
                type = JsonType.DATE
                _date = value
            }
        }
        hasValue = true
    }

    fun clearObject(): JsonNode {
        synchronized(children) {
            mandate(!isReadOnly, "Json: Attempt to clearObject() when read only")
            type = JsonType.OBJECT
            children = ArrayList()
            return this
        }
    }

    fun clearArray(): JsonNode {
        synchronized(children) {
            mandate(!isReadOnly, "Json: Attempt to clearArray() when read only")
            type = JsonType.ARRAY
            children = ArrayList()
            return this
        }
    }

    fun write(writer: Writer, pretty: Boolean = false, compact: Boolean = false, indent: String = "") {

        fun doWrite(text: String) {
            if (pretty) {
                writer.write("${indent}${text}")
            } else {
                writer.write(text)
            }
        }

        fun doOpen(text: String) {
            if (pretty) {
                writer.write("${indent}${text}\n")
            } else {
                writer.write(text)
            }
        }

        fun doClose(text: String) {
            if (pretty) {
                writer.write("\n${indent}${text}")
            } else {
                writer.write(text)
            }
        }

        fun doDelimiter() {
            if (pretty) {
                writer.write(",\n")
            } else {
                writer.write(",")
            }
        }

        val parent = _parent
        val preamble = if (parent?.type ?: JsonType.UNDEFINED == JsonType.OBJECT) "${name.quoted}:" else ""


        when (type) {
            JsonType.UNDEFINED -> {
                if (!compact) {
                    doWrite("${preamble}undefined")
                }
            }
            JsonType.NULL -> {
                if (!compact || isNotDefault || parent?.isArray == true) {
                    doWrite("${preamble}null")
                }
            }
            JsonType.LONG -> {
                if (!compact || isNotDefault || parent?.isArray == true) {
                    doWrite("$preamble${_long}")
                }
            }
            JsonType.DOUBLE -> {
                if (!compact || isNotDefault || parent?.isArray == true) {
                    doWrite("$preamble${_double}")
                }
            }
            JsonType.STRING -> {
                if (!compact || isNotDefault || parent?.isArray == true) {
                    doWrite("$preamble${_string.escapeQuoted}")
                }
            }
            JsonType.BOOLEAN -> {
                if (!compact || isNotDefault || parent?.isArray == true) {
                    doWrite("$preamble${if (_boolean) "true" else "false"}")
                }
            }
            JsonType.DATE -> {
                if (!compact || isNotDefault || parent?.isArray == true) {
                    doWrite("$preamble${_date.toJson().escapeQuoted}")
                }
            }
            JsonType.ARRAY -> {
                if (!compact || hasCompactElements || parent?.isArray == true) {
                    doOpen("$preamble[")
                    var empty = true

                    // remove trailing null elements
                    while (children.isNotEmpty() && children.last().isNull) {
                        children.removeAt(children.size - 1)
                    }

                    for (child in children) {
                        if (!empty) {
                            doDelimiter()
                        }
                        child.write(writer, pretty, compact, indent + "    ")
                        empty = false
                    }
                    doClose("]")
                }
            }
            JsonType.OBJECT -> {
                synchronized(children) {
                    if (!compact || hasCompactElements || parent?.isArray == true) {
                        if (name.isEmpty() || parent?.isArray == true) {
                            doOpen("{")
                        } else {
                            doOpen("$preamble{")
                        }
                        var empty = true
                        for (child in children) {
                            if (!compact || child.isNotDefault) {
                                if (!empty) {
                                    doDelimiter()
                                }
                                child.write(writer, pretty, compact, indent + "    ")
                                empty = false
                            }
                        }
                        doClose("}")
                    }
                }
            }
        }
    }

    val isNotDefault: Boolean
        get() {
            when (type) {
                JsonType.UNDEFINED -> {
                }
                JsonType.NULL -> {
                    if (_parent?.isArray ?: false) {
                        return true
                    }
                }
                JsonType.LONG -> {
                    if (_long != 0L) {
                        return true
                    }
                }
                JsonType.DOUBLE -> {
                    if (_double != 0.0) {
                        return true
                    }
                }
                JsonType.STRING -> {
                    if (_string.isNotEmpty()) {
                        return true
                    }
                }
                JsonType.BOOLEAN -> {
                    if (_boolean != false) {
                        return true
                    }
                }
                JsonType.DATE -> {
                    if (_date != nullDate) {
                        return true
                    }
                }
                JsonType.ARRAY -> {
                    if (hasCompactElements) {
                        return true
                    }
                }
                JsonType.OBJECT -> {
                    if (hasCompactElements) {
                        return true
                    }
                }
            }
            return false
        }

    val hasCompactElements: Boolean
        get() {
            synchronized(children) {
                for (child in children) {
                    if (child.isNotDefault) {
                        return true
                    }
                }
                return false
            }
        }

    fun save(file: File, pretty: Boolean = false, compact: Boolean = false) {
        val writer = FileWriter(file)
        write(writer, pretty, compact)
        writer.close()
    }

    fun save(stream: OutputStream, pretty: Boolean = false, compact: Boolean = false) {
        val writer = OutputStreamWriter(stream)
        write(writer, pretty, compact)
        writer.close()
    }

    open fun saveToTable(table: DbTable<*>, handled: (String, JsonNode) -> Boolean = { _, _ -> false }) {
        if (table.isOnRow) {
            val columns = table.columns
            if (columns != null) {
                for (column in columns) {
                    val item = get(column.columnName)
                    if (!item.isUndefined && !handled(column.columnName, item)) {

                        /*
                        when (column.columnType) {
                            Types.DOUBLE -> table.setValue(column.columnName, item.asDouble)
                            Types.INTEGER, Types.DECIMAL ->  table.setValue(column.columnName, item.asInt)
                            Types.VARCHAR, Types.LONGVARCHAR -> {
                                if (item._string.isEmpty() && column.className.oneOf("java.sql.Date", "java.sql.Timestamp")) {
                                    table.setValue(column.columnName, item.asDate)
                                } else {
                                    table.setValue(column.columnName, item._string)
                                }
                            }
                            Types.DATE, Types.TIMESTAMP -> table.setValue(column.columnName, item.asDate)
                            Types.BOOLEAN, Types.BIT -> table.setValue(column.columnName, item.asBoolean)
                            else -> doNothing()
                        }
                        */
                        when (item.type) {
                            JsonType.DOUBLE -> table.setValue(column.columnName, item._double)
                            JsonType.LONG -> table.setValue(column.columnName, item._long)
                            JsonType.STRING -> {
                                if (column.className.oneOf("java.sql.Date", "java.sql.Timestamp")) {
                                    table.setValue(column.columnName, item.asDate)
                                } else {
                                    table.setValue(column.columnName, item._string)
                                }
                            }
                            JsonType.DATE -> table.setValue(column.columnName, item._date)
                            JsonType.BOOLEAN -> table.setValue(column.columnName, item._boolean)
                            else -> doNothing()
                        }
                    }
                }
            }
            table.post()
        }
    }

    fun loadFromDataset(dataset: DbDatasetCursor<*>, columnNames: String = "", prototype: Boolean = false, handled: (String) -> Boolean = { _ -> false }) {
        mandate(!isReadOnly, "Json: Attempt to loadFromDataset when read only")
        val select = columnNames.split(",")
        val columns = dataset.columns
        if (columns != null) {
            for (column in columns) {
                if ((select.contains(column.columnName) || columnNames.isEmpty() || columnNames == "*") && dataset.isColumn(column.columnName) && column.displaySize != 9999) {
                    if (!handled(column.columnName)) {
                        when (column.columnType) {
                            Types.DOUBLE -> this[column.columnName] = if (prototype) 0.0 else dataset.getDouble(column.columnName)
                            Types.INTEGER, Types.DECIMAL -> this[column.columnName] = if (prototype) 0 else dataset.getInt(column.columnName)
                            Types.VARCHAR, Types.LONGVARCHAR -> this[column.columnName] = if (prototype) "" else dataset.getString(column.columnName)
                            Types.DATE, Types.TIMESTAMP -> this[column.columnName] = if (prototype) nullDate else dataset.getDate(column.columnName)
                            Types.BOOLEAN, Types.BIT -> this[column.columnName] = if (prototype) false else dataset.getBoolean(column.columnName)
                        }
                    }
                }
            }
        }
    }


    fun parseUnicode(stack: CharStack): Int {
        var unicode = ""
        while (!stack.isEmpty) {
            val char = stack.pop()
            when (char) {
                in '0'..'9', in 'a'..'f', in 'A'..'F' -> {
                    unicode += char
                    if (unicode.length == 4) {
                        return Integer.parseInt(unicode, 16)
                    }
                }
                else -> {
                    throw Unexpected(stack, "Unexpected character ($char) in JSON")
                }
            }
        }
        throw Unexpected(stack, "Unexpected EOS in JSON")
    }

    fun parseEscape(stack: CharStack): String {
        while (!stack.isEmpty) {
            val char = stack.pop()
            when (char) {
                '"' -> return "\""
                '\\' -> return "\\"
                '/' -> return "/"
                'b' -> return "\b"
                'f' -> return 12.toChar().toString()
                'n' -> return "\n"
                'r' -> return "\r"
                't' -> return "\t"
                'u' -> return parseUnicode(stack).toChar().toString()
                else -> throw Unexpected(stack, "Json: unexpected escaped character ($char)")
            }
        }
        throw Unexpected(stack, "Unexpected EOS in JSON")
    }

    fun parseString(stack: CharStack) {
        var stringBuilder = StringBuilder()
        var dashCount = 0
        var digitCount = 0
        while (!stack.isEmpty) {
            val char = stack.pop()
            when (char) {
                '"' -> {
                    type = JsonType.STRING
                    _string = stringBuilder.toString()
                    // todo - do proper date parsing
                    if (dashCount==2 && digitCount>=8) {
                        val date = stringBuilder.toString().asJsonDate
                        if (date != null) {
                            type = JsonType.DATE
                            _date = date
                            return
                        }
                    }
                    return
                }
                '-' -> {
                    dashCount++
                    stringBuilder.append(char)                    
                }
                in '0'..'9' -> {
                    digitCount++
                    stringBuilder.append(char)
                }
                '\\' -> {
                    stringBuilder.append(parseEscape(stack))
                }
                else -> {
                    if (char.toInt() < 32) {
                        //throw Unexpected(stack, "Json: Ilegal control character (${char.toInt()} in string")
                        doNothing()
                    } else {
                        stringBuilder.append(char)
                    }
                }
            }
        }
        throw Unexpected(stack, "Unexpected EOS in JSON")
    }

    enum class Number { INITIAL, HAVE_SIGN, IN_INTEGER, HAVE_ZERO_INTEGER, HAVE_DECIMAL_POINT, IN_DECIMAL,
        EXPONENT_INITIAL, HAVE_EXPONENT_SIGN, IN_EXPONENT
    }

    fun parseNumber(stack: CharStack) {
        var state = Number.INITIAL
        var sign = 1
        var integer = 0L
        var decimal = 0
        var decimalPlaces = 0
        var exponentSign = 1
        var exponent = 0
        while (!stack.isEmpty) {
            val char = stack.pop()
            when (char) {
                '-' -> {
                    when (state) {
                        Number.INITIAL -> {
                            sign = -1
                            state == Number.HAVE_SIGN
                        }
                        Number.EXPONENT_INITIAL -> {
                            exponentSign = -1
                            state == Number.HAVE_EXPONENT_SIGN
                        }
                        else -> throw Unexpected(stack, "Json: Illegal char ($char) in number value")
                    }
                }
                '0' -> {
                    when (state) {
                        Number.INITIAL, Number.HAVE_SIGN -> {
                            state = Number.HAVE_ZERO_INTEGER
                        }
                        Number.IN_INTEGER -> {
                            integer *= 10
                        }
                        Number.HAVE_DECIMAL_POINT, Number.IN_DECIMAL -> {
                            state = Number.IN_DECIMAL
                            decimal = decimal * 10
                            decimalPlaces++
                        }
                        Number.EXPONENT_INITIAL, Number.HAVE_EXPONENT_SIGN, Number.IN_EXPONENT -> {
                            state = Number.IN_EXPONENT
                            exponent *= 10
                        }
                        else -> throw Unexpected(stack, "Json: Illegal char ($char) in number value")
                    }
                }
                in '1'..'9' -> {
                    when (state) {
                        Number.INITIAL, Number.HAVE_SIGN, Number.IN_INTEGER -> {
                            state = Number.IN_INTEGER
                            integer = integer * 10 + char.toInt() - '0'.toInt()
                        }
                        Number.HAVE_DECIMAL_POINT, Number.IN_DECIMAL -> {
                            state = Number.IN_DECIMAL
                            decimal = decimal * 10 + char.toInt() - '0'.toInt()
                            decimalPlaces++
                        }
                        Number.EXPONENT_INITIAL, Number.HAVE_EXPONENT_SIGN, Number.IN_EXPONENT -> {
                            state = Number.IN_EXPONENT
                            exponent = exponent * 10 + char.toInt() - '0'.toInt()
                        }
                        else -> throw Unexpected(stack, "Json: Illegal char ($char) in number value")
                    }
                }
                '.' -> {
                    when (state) {
                        Number.IN_INTEGER, Number.HAVE_ZERO_INTEGER -> {
                            state = Number.HAVE_DECIMAL_POINT
                        }
                        else -> throw Unexpected(stack, "Json: Illegal char ($char) in number value")
                    }
                }
                'e', 'E' -> {
                    when (state) {
                        Number.IN_INTEGER, Number.HAVE_ZERO_INTEGER, Number.IN_DECIMAL -> {
                            state = Number.EXPONENT_INITIAL
                        }
                        else -> throw Unexpected(stack, "Json: Illegal char ($char) in number value")
                    }
                }
                '+' -> {
                    when (state) {
                        Number.EXPONENT_INITIAL -> {
                            state = Number.HAVE_EXPONENT_SIGN
                        }
                        else -> throw Unexpected(stack, "Json: Illegal char ($char) in number value")
                    }
                }
                else -> {
                    when (state) {
                        Number.HAVE_ZERO_INTEGER, Number.IN_INTEGER, Number.IN_DECIMAL, Number.IN_EXPONENT -> {
                            if (exponent == 0 && decimal == 0) {
                                setValue(sign * integer)
                            } else {
                                setValue(sign.toDouble() * (integer.toDouble() + decimal / Math.pow(10.0, decimalPlaces.toDouble())) * Math.pow(10.0, (exponentSign * exponent).toDouble()))
                            }
                            stack.unPop()
                            return
                        }
                        else -> throw Unexpected(stack, "Json: Illegal char ($char) in number value")
                    }
                }
            }
        }
        throw Unexpected(stack, "Unexpected EOS in JSON")
    }

    fun parseMatch(stack: CharStack, string: String): Boolean {
        for (i in 0..string.length - 1) {
            if (string[i] != stack.pop()) {
                throw Unexpected(stack, "Unexpected character in JSON")
            }
        }
        return true
    }

    fun parseTrue(stack: CharStack) {
        if (parseMatch(stack, "true")) {
            type = JsonType.BOOLEAN
            _boolean = true
        }
    }

    fun parseFalse(stack: CharStack) {
        if (parseMatch(stack, "false")) {
            type = JsonType.BOOLEAN
            _boolean = false
        }
    }

    fun parseNull(stack: CharStack) {
        if (parseMatch(stack, "null")) {
            type = JsonType.NULL
        }
    }

    fun parseName(stack: CharStack): String {
        var haveName = false
     //   var name = ""
        val name = StringBuilder()

        while (!stack.isEmpty) {
            val char = stack.pop()
            when (char) {
                '\t', '\n', '\r', ' ' -> {
                    // ignore
                }
                '"' -> {
                    if (!haveName) {
                        haveName = true
                    } else {
                        throw Unexpected(stack, "Unexpected character ($char) in JSON")
                    }
                }
                ':' -> {
                    if (haveName) {
                        return name.toString()
                    } else {
                        name.append(char)
                    }
                }
                '\\' -> {
                    if (!haveName) {
                        name.append(parseEscape(stack))
                    } else {
                        throw Unexpected(stack, "Unexpected character ($char) in JSON")
                    }
                }
                else -> {
                    if (!haveName) {
                        name.append(char)
                    } else {
                        throw Unexpected(stack, "Unexpected character ($char) in JSON")
                    }

                }
            }
        }
        throw Unexpected(stack, "Unexpected EOS in JSON")
    }


    fun parseNamedItem(stack: CharStack) {
        var name = parseName(stack)
        while (!stack.isEmpty) {
            val char = stack.pop()
            when (char) {
                '\t', '\n', '\r', ' ' -> {
                    // ignore
                }
                '{' -> {
                    get(name).parseObject(stack)
                }
                '[' -> {
                    get(name).parseArray(stack)
                }
                '"' -> {
                    get(name).parseString(stack)
                }
                '+', '-', in '0'..'9' -> {
                    stack.unPop()
                    get(name).parseNumber(stack)
                }
                't' -> {
                    stack.unPop()
                    get(name).parseTrue(stack)
                }
                'f' -> {
                    stack.unPop()
                    get(name).parseFalse(stack)
                }
                'n' -> {
                    stack.unPop()
                    get(name).parseNull(stack)
                }
                ',' -> {
                    stack.unPop()
                    return
                }
                '}' -> {
                    stack.unPop()
                    return
                }
                else -> {
                    throw Unexpected(stack, "Unexpected character ($char) in JSON")
                }
            }
        }
    }

    protected fun parseObject(stack: CharStack) {
        var itemNeeded = false
        type = JsonType.OBJECT
        while (!stack.isEmpty) {
            val char = stack.pop()
            when (char) {
                '\t', '\n', '\r', ' ' -> {
                    // ignore
                }
                '"' -> {
                    itemNeeded = false
                    parseNamedItem(stack)
                }
                ',' -> {
                    if (size == 0) {
                        throw Unexpected(stack, "Json: Element missing from object (comma before item)")
                    }
                    itemNeeded = true
                }
                '}' -> {
                    if (itemNeeded) {
                        throw Unexpected(stack, "Json: Item missing from object (extra comma)")
                    }
                    return
                }
                else -> {
                    throw Unexpected(stack, "Unexpected character ($char) in JSON")
                }
            }
        }
        throw Unexpected(stack, "Json: Parse object - no closing }")
    }

    protected fun parseArray(stack: CharStack) {
        var elementNeeded = false
        type = JsonType.ARRAY
        while (!stack.isEmpty) {
            val char = stack.pop()
            when (char) {
                '\t', '\n', '\r', ' ' -> {
                    // ignore
                }
                '{' -> {
                    elementNeeded = false
                    val element = addElement()
                    element.parseObject(stack)
                }
                '[' -> {
                    elementNeeded = false
                    val element = addElement()
                    element.parseArray(stack)
                }
                '"' -> {
                    elementNeeded = false
                    val element = addElement()
                    element.parseString(stack)
                }
                '-', in '0'..'9' -> {
                    elementNeeded = false
                    stack.unPop()
                    val element = addElement()
                    element.parseNumber(stack)
                }
                't' -> {
                    elementNeeded = false
                    stack.unPop()
                    val element = addElement()
                    element.parseTrue(stack)
                }
                'f' -> {
                    elementNeeded = false
                    stack.unPop()
                    val element = addElement()
                    element.parseFalse(stack)
                }
                'n' -> {
                    elementNeeded = false
                    stack.unPop()
                    val element = addElement()
                    element.parseNull(stack)
                }
                ',' -> {
                    if (size == 0) {
                        throw Unexpected(stack, "Json: Element missing from array (comma before element)")
                    }
                    elementNeeded = true
                }
                ']' -> {
                    if (elementNeeded) {
                        throw Unexpected(stack, "Json: Element missing from array (extra comma)")
                    }
                    return
                }
                else -> {
                    throw Unexpected(stack, "Unexpected character ($char) in JSON")
                }
            }
        }
        throw Unexpected(stack, "Json: Parse object - no closing ]")
    }

    fun parse(stack: CharStack) {
        while (!stack.isEmpty) {
            val char = stack.pop()
            when (char) {
                '\t', '\n', '\r', ' ' -> {
                    // ignore
                }
                '{' -> {
                    parseObject(stack)
                    val surplus = stack.surplus
                    if (surplus.isNotEmpty()) {
                        throw Unexpected(stack, "Json: Unexpected characters after end of array ($surplus)")
                    }
                    return
                }
                '[' -> {
                    parseArray(stack)
                    val surplus = stack.surplus
                    if (surplus.isNotEmpty()) {
                        throw Unexpected(stack, "Json: Unexpected characters after end of object ($surplus)")
                    }
                    return
                }
                else -> {
                    throw Unexpected(stack, "Unexpected character ($char) in JSON")
                }
            }
        }
        clearObject()
    }

    fun toJson(pretty: Boolean = false, compact: Boolean = false): String {
        if (compact) {
            compress()
        }
        val writer = StringWriter()
        write(writer, pretty, compact)
        writer.close()
        if (compact) {
            var result = writer.toString() // .replace(",}", "}").replace(",]", "]")
            if (result.isEmpty()) {
                result = if (isArray) "[]" else "{}"
            }
            return result
        }
        return writer.toString()
    }

    companion object {
        fun stringToDate(string: String): Date {
            var date = string.replace("\u200E", "").asJsonDate
            if (date != null) return date


            val sample = string.replace("\u200E", "").replace("-", "/").replace(".", "/").replace(",", " ").replace("  ", " ").trim()
            if (sample.isEmpty()) return nullDate

            val result = if (sample.contains("/") && sample.substringBefore("/").length == 4) sample.toDateOrNull("yyyy/M/d")
                    ?: nullDate
            else
                sample.asJsonDate ?: sample.toDateOrNull("d/M/yy") ?: sample.toDateOrNull("d MMM yy")
                ?: sample.toDateOrNull("MMM dd yy") ?: sample.toDateOrNull("ddMMyy") ?: sample.toDateOrNull("ddMMyyyy")
                ?: nullDate
            if (result.isEmpty()) debug("JSON", "Invalid date: $string (${string.length})")
            return result
        }

    }

}

interface JsonContainer {
    fun getJson(): JsonNode
}

class PropertyJsonInt(var itemPath: String) {
    operator fun getValue(wrapper: JsonContainer, property: KProperty<*>): Int {
        if (wrapper.getJson().has(itemPath)) {
            return wrapper.getJson().getNode(itemPath).asInt
        } else {
            return 0
        }
    }

    operator fun setValue(wrapper: JsonContainer, property: KProperty<*>, value: Int) {
        wrapper.getJson().getNode(itemPath).setValue(value)
    }
}

class PropertyJsonLong(var itemPath: String) {
    operator fun getValue(wrapper: JsonContainer, property: KProperty<*>): Long {
        if (wrapper.getJson().has(itemPath)) {
            return wrapper.getJson().getNode(itemPath).asLong
        } else {
            return 0L
        }
    }

    operator fun setValue(wrapper: JsonContainer, property: KProperty<*>, value: Long) {
        wrapper.getJson().getNode(itemPath).setValue(value)
    }
}

class PropertyJsonDate(var itemPath: String) {
    operator fun getValue(wrapper: JsonContainer, property: KProperty<*>): Date {
        if (wrapper.getJson().has(itemPath)) {
            return wrapper.getJson().getNode(itemPath).asDate
        } else {
            return nullDate
        }
    }
    
    operator fun setValue(wrapper: JsonContainer, property: KProperty<*>, value: Date) {
        wrapper.getJson().getNode(itemPath).setValue(value)
    }
}

class PropertyJsonBoolean(var itemPath: String) {
    operator fun getValue(wrapper: JsonContainer, property: KProperty<*>): Boolean {
        if (wrapper.getJson().has(itemPath)) {
            return wrapper.getJson().getNode(itemPath).asBoolean
        } else {
            return false
        }
    }

    operator fun setValue(wrapper: JsonContainer, property: KProperty<*>, value: Boolean) {
        wrapper.getJson().getNode(itemPath).setValue(value)
    }
}

class PropertyJsonString(var itemPath: String) {
    operator fun getValue(wrapper: JsonContainer, property: KProperty<*>): String {
        if (wrapper.getJson().has(itemPath)) {
            return wrapper.getJson().getNode(itemPath).asString
        } else {
            return ""
        }
    }

    operator fun setValue(wrapper: JsonContainer, property: KProperty<*>, value: String) {
        wrapper.getJson().getNode(itemPath).setValue(value)
    }
}

class PropertyJsonNode(var itemPath: String) {
    operator fun getValue(wrapper: JsonContainer, property: KProperty<*>): JsonNode {
        return wrapper.getJson().getNode(itemPath)
    }
}

