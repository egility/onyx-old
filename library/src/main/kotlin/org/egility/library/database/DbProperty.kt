/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.database

import org.egility.library.general.*
import java.util.*
import kotlin.reflect.KProperty

/**
 * Created by mbrickman on 01/12/15.
 */

class DbLink<T : DbTable<*>>(val factory: () -> T, val label: String = "", val on: String = "", vararg keyNames: String) {

    var link: T? = null
    val keys = keyNames

    operator fun getValue(table: DbTable<*>, property: KProperty<*>): T {
        val old = link
        if (old == null) {
            val new = factory().linkTo(table, on, *keys) as T
            new.label = label
            link = new
            return new
        } else {
            return old
        }
    }
}

class DbPropertyInt(var columnName: String) {

    operator fun getValue(table: DbTable<*>, property: KProperty<*>): Int {
        return table.getInt(columnName)
    }

    operator fun setValue(table: DbTable<*>, property: KProperty<*>, value: Int) {
        table.setValue(columnName, value)
    }

}

class DbPropertyString(var columnName: String) {

    operator fun getValue(table: DbTable<*>, property: KProperty<*>): String {
        return table.getString(columnName)
    }

    operator fun setValue(table: DbTable<*>, property: KProperty<*>, value: String) {
        table.setValue(columnName, value)
    }

}

class DbPropertyJson(var columnName: String) {

    operator fun getValue(table: DbTable<*>, property: KProperty<*>): Json {
        val result = table.getJson(columnName)
        return result
    }

    operator fun setValue(table: DbTable<*>, property: KProperty<*>, value: Json) {
        table.setValue(columnName, value)
    }
}

class DbPropertyBoolean(var columnName: String) {

    operator fun getValue(table: DbTable<*>, property: KProperty<*>): Boolean {
        return table.getBoolean(columnName)
    }

    operator fun setValue(table: DbTable<*>, property: KProperty<*>, value: Boolean) {
        table.setValue(columnName, value)
    }
}

class DbPropertyDate(var columnName: String) {

    operator fun getValue(table: DbTable<*>, property: KProperty<*>): Date {
        return table.getDate(columnName)
    }

    operator fun setValue(table: DbTable<*>, property: KProperty<*>, value: Date) {
        table.setValue(columnName, value)
    }
}

class DbPropertyDouble(var columnName: String) {

    operator fun getValue(table: DbTable<*>, property: KProperty<*>): Double {
        return table.getDouble(columnName)
    }

    operator fun setValue(table: DbTable<*>, property: KProperty<*>, value: Double) {
        table.setValue(columnName, value)
    }
}

class DbPropertyBit(var columnName: String, var bit: Int) {

    operator fun getValue(table: DbTable<*>, property: KProperty<*>): Boolean {
        return table.getInt(columnName).isBitSet(bit)
    }

    operator fun setValue(table: DbTable<*>, property: KProperty<*>, value: Boolean) {
        if (value) {
            table.setValue(columnName, table.getInt(columnName).setBit(bit))
        } else {
            table.setValue(columnName, table.getInt(columnName).resetBit(bit))
        }
    }
}

class DbPropertyJsonInt(val jsonColumnName: String, var itemPath: String) {

    operator fun getValue(table: DbTable<*>, property: KProperty<*>): Int {
        return table.getJson(jsonColumnName).getNode(itemPath).asInt
    }

    operator fun setValue(table: DbTable<*>, property: KProperty<*>, value: Int) {
        table.getJson(jsonColumnName).getNode(itemPath).setValue(value)
    }

}

class DbPropertyJsonBoolean(val jsonColumnName: String, var itemPath: String) {

    operator fun getValue(table: DbTable<*>, property: KProperty<*>): Boolean {
        return table.getJson(jsonColumnName).getNode(itemPath).asBoolean
    }

    operator fun setValue(table: DbTable<*>, property: KProperty<*>, value: Boolean) {
        table.getJson(jsonColumnName).getNode(itemPath).setValue(value)
    }

}

class DbPropertyJsonString(val jsonColumnName: String, var itemPath: String) {

    operator fun getValue(table: DbTable<*>, property: KProperty<*>): String {
        return table.getJson(jsonColumnName).getNode(itemPath).asString
    }

    operator fun setValue(table: DbTable<*>, property: KProperty<*>, value: String) {
        table.getJson(jsonColumnName).getNode(itemPath).setValue(value)
    }

}

class DbPropertyJsonDate(val jsonColumnName: String, var itemPath: String) {

    operator fun getValue(table: DbTable<*>, property: KProperty<*>): Date {
        return table.getJson(jsonColumnName).getNode(itemPath).asDate.dateOnly()
    }

    operator fun setValue(table: DbTable<*>, property: KProperty<*>, value: Date) {
        table.getJson(jsonColumnName).getNode(itemPath).setValue(value.dateOnly())
    }

}

class DbPropertyJsonDateTime(val jsonColumnName: String, var itemPath: String) {

    operator fun getValue(table: DbTable<*>, property: KProperty<*>): Date {
        return table.getJson(jsonColumnName).getNode(itemPath).asDate
    }

    operator fun setValue(table: DbTable<*>, property: KProperty<*>, value: Date) {
        table.getJson(jsonColumnName).getNode(itemPath).setValue(value)
    }

}

class DbPropertyJsonObject(val jsonColumnName: String, var itemPath: String) {

    operator fun getValue(table: DbTable<*>, property: KProperty<*>): JsonNode {
        return table.getJson(jsonColumnName).getNode(itemPath)
    }

    operator fun setValue(table: DbTable<*>, property: KProperty<*>, value: JsonNode) {
        table.getJson(jsonColumnName).getNode(itemPath).setValue(value)
    }


}

