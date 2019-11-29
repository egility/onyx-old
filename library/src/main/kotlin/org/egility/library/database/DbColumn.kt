/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.database

import org.egility.library.general.debug
import java.sql.ResultSetMetaData
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

data class DbColumn(
    var columnIndex: Int = -1,
    val tableAlias: String = "",
    val tableName: String = "",
    val columnName: String = "",
    val columnLabel: String = "",
    var databaseName: String = "",
    var schemaName: String = "",
    var className: String = "",
    var columnType: Int = -1,
    var displaySize: Int = -1,
    var precision: Int = -1,
    var decimals: Int = -1,
    var isCurrency: Boolean = false,
    var isReadable: Boolean = true,
    var isSearchable: Boolean = false,
    var isSigned: Boolean = false,
    var isWritable: Boolean = false,
    var isNotNull: Boolean = false
) {
    var isAccessed = false
    val qualifiedName: String
        get() = if (tableAlias.isEmpty()) columnLabel else tableAlias + "." + columnLabel
}

fun getExtra(metaData: ResultSetMetaData): ArrayList<HashMap<String, String>> {
    val result = ArrayList<HashMap<String, String>>()
    val lines = metaData.toString().split("\n")
    lines.drop(0)
    for (line in lines) {
        val fields = line.substringAfter("[").substringBefore("]").split(",")
        val map = HashMap<String, String>()
        result.add(map)
        for (field in fields) {
            map[field.substringBefore("=")] = field.substringAfter("=")
        }
    }
    return result
}

fun getMetaColumns(connection: DbConnection, metaData: ResultSetMetaData, add: (DbColumn) -> Unit) {
    val extra = getExtra(metaData)
    for (i in 1..metaData.columnCount) {
        val _schemaColumn =
            connection._getColumnSchema(metaData.getTableName(i), metaData.getColumnName(i), metaData.getCatalogName(i))
        add(
            DbColumn(
                columnIndex = i - 1,
                tableAlias = extra[i]["tableName"] ?: "",
                tableName = metaData.getTableName(i),
                columnName = metaData.getColumnName(i),
                columnLabel = metaData.getColumnLabel(i),
                databaseName = metaData.getCatalogName(i),
                className = metaData.getColumnClassName(i),
                displaySize = metaData.getColumnDisplaySize(i),
                columnType = metaData.getColumnType(i),
                precision = metaData.getPrecision(i),
                decimals = metaData.getScale(i),
                schemaName = metaData.getSchemaName(i),
                isCurrency = metaData.isCurrency(i),
                isSearchable = metaData.isSearchable(i),
                isSigned = metaData.isSigned(i),
                isWritable = metaData.isWritable(i),
                isNotNull = _schemaColumn?.isNotNull ?: true
            )
        )
    }
}


