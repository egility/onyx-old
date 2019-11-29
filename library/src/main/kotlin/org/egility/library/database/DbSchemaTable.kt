/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.database

import org.egility.library.general.*
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by mbrickman on 13/05/15.
 */

class DbSchema(var url: String) {

    val tables = HashMap<String, DbSchemaTable>()

    companion object {
        private val schema = HashMap<String, DbSchema>()

        fun getSchema(url: String): DbSchema {
            val old=schema.get(url)
            if (old!=null) {
                return old
            } else {
                val new=DbSchema(url)
                schema.put(url, new)
                return new
            }
        }
    }
}

class DbSchemaTable(val tableName: String) {
    val columns = LinkedHashMap<String, DbSchemaColumn>()
    val primaryKeys = ArrayList<String>()
    var hasAutoIncrement = false

    fun addColumn(columnName: String, type: String, notNull: Boolean, primaryKey: Boolean, autoIncrement: Boolean, defaultValue: Variant) {
        val column = DbSchemaColumn(this, columnName, type, notNull, primaryKey, autoIncrement, defaultValue)
        columns.put(columnName, column)
        if (column.isPrimaryKey) {
            primaryKeys.add(column.columnName)
        }
        if (autoIncrement) hasAutoIncrement = true
    }

    fun getColumn(columnName: String): DbSchemaColumn? {
        return columns[columnName]
    }

    fun hasColumn(columnName: String): Boolean {
        return columns.containsKey(columnName)
    }

    fun generateRawCode(open: Boolean=true) {
        for (item in columns) {
            val column = item.value
            var type = "Unknown_" + column.type

            if (column.type.oneOf("varchar(9999)", "varchar(49999)")) {
                type = "Json"
            } else if (column.type.startsWith("int")) {
                type = "Int"
            } else if (column.type.startsWith("varchar") || column.type.startsWith("longtext") || column.type.startsWith("char")) {
                type = "String"
            } else if (column.type.startsWith("tinyint")) {
                type = "Boolean"
            } else if (column.type.startsWith("date")) {
                type = "Date"
            } else if (column.type.startsWith("decimal") || column.type.startsWith("double")) {
                type = "Double"
            } else if (column.type.startsWith("json")) {
                type = "Json"
            }

            var columnName = column.columnName
            var propertyName = column.columnName.initialLower
            var shortTableName = if (tableName eq "agilityClass") "class" else ""


            if (propertyName eq "id" + tableName) {
                propertyName = "id"
            } else if (propertyName.startsWith(tableName, ignoreCase = true) ) {
                propertyName = propertyName.dropLeft(tableName.length).initialLower
            } else if (!shortTableName.isEmpty() && propertyName.startsWith(shortTableName, ignoreCase = true) ) {
                propertyName = propertyName.dropLeft(shortTableName.length).initialLower
            }

            if (open) {
                println("open var $propertyName: $type by DbProperty$type(\"$columnName\")")
            } else {
                println("var $propertyName: $type by DbProperty$type(\"$columnName\")")
            }
        }
    }

}

class DbSchemaColumn(val schemaTable: DbSchemaTable, var columnName: String, var type: String, notNull: Boolean, primaryKey: Boolean, autoIncrement: Boolean, defaultValue: Variant) {
    var isNotNull = true
    var isPrimaryKey = false
    var isAutoIncrement = false
    var defaultValue = Variant()

    init {
        this.isNotNull = notNull
        this.isPrimaryKey = primaryKey
        this.isAutoIncrement = autoIncrement
        this.defaultValue = defaultValue
    }

    val tableName: String
        get() = schemaTable.tableName

}