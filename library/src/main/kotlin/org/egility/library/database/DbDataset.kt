/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.database

import org.egility.library.general.*
import java.sql.Statement
import java.sql.Time
import java.sql.Timestamp
import java.sql.Types
import java.util.*

class DbDataset(private val connection: DbConnection, private val statement: Statement) {

    private enum class RowState { SELECTED, APPENDING, APPENDED }

    data class Table(val tableAlias: String, val tableName: String) {
        var hasPrimary: Boolean = false
        var autoIncrement: Boolean = false
        val primaryKeys = ArrayList<String>()
    }
    
    protected fun finalize() {
        statement.close()
    }

    private val rows = ArrayList<Int>()
    val columns = ArrayList<DbColumn>()

    private val tables = HashMap<String, Table>()
    private val columnsHash = LinkedHashMap<String, DbColumn>()
    private val values = Array(statement.resultSet.metaData.columnCount) { Variant() }
    private var rowState = RowState.SELECTED
    
    private var mainTable: Table? = null
    var mainTableName: String=""
        set(value) {
            mainTable = tables[value]
            field = mainTable?.tableName?:""
        }
    
    val rowCount: Int
        get() = if (rowState == RowState.SELECTED) rows.size else 1

    val isOnRow: Boolean
        get() = rowIndex.between(0, rowCount - 1)

    var rowIndex = -1
        set(value) {
            val old = field
            field = when {
                (value < 0 || rowCount==0) -> -1
                (value < rowCount) -> value
                else -> rowCount
            }
            if (field != old && isOnRow) loadRow()
        }

    val isAppending: Boolean
        get() = rowState == RowState.APPENDING

    init {
        load()
    }
    
    private fun getColumn(name: String): DbColumn? {
        return columnsHash[name.toLowerCase()]
    }
    
    private fun load() {
        // load rows
        while (statement.resultSet.next()) {
            rows.add(rowCount + 1)
        }

        // load column definitions
        getMetaColumns(connection, statement.resultSet.metaData) { column->
            columns.add(column)
            columnsHash.getOrPut(column.columnLabel.toLowerCase(), {column})
            columnsHash.getOrPut(column.qualifiedName.toLowerCase(), {column})
            if (column.tableName.isNotEmpty()) {
                tables.getOrPut(column.tableAlias, { Table(column.tableAlias, column.tableName) })
            }
        }

        // lookup table details from the database schema
        for (item in tables) {
            val table = item.value
            val schema = connection._getTableSchema(table.tableName)
            if (schema != null) {
                table.primaryKeys.addAll(schema.primaryKeys)
                table.autoIncrement = schema.hasAutoIncrement
                table.hasPrimary = true
                for (key in table.primaryKeys) {
                    if (!isColumn("${table.tableAlias}.$key")) table.hasPrimary = false
                }
            }
        }

        // check writable (parent table must have all its primary key values)
        for (column in columns) {
            val table = tables.get(column.tableAlias)
            if (table != null) {
                column.isWritable = column.isWritable && table.hasPrimary
            }
        }
    }

    private fun loadRow() {
        when (rowState) {
            RowState.SELECTED -> {
                for (column in columns) {
                    statement.resultSet.absolute(rows[rowIndex])
                    values[column.columnIndex].setEmpty()
                }
            }
            else -> {
                for (column in columns) {
                    values[column.columnIndex].setNull()
                }
                rowState = RowState.APPENDING
            }
        }
        checkPoint()
    }

    private fun setValueAsVariant(column: Int, columnType: Int, columnSize: Int, variant: Variant, columnName: String) {
        when (columnType) {
            Types.TINYINT, Types.SMALLINT, //case Types.MEDIUMINT:
            Types.INTEGER, Types.BIGINT, Types.ROWID -> {
                val long: Long? = statement.resultSet.getLong(column)
                if (long == null) {
                    variant.set(0)
                } else {
                    variant.set(long)
                }
            }
            Types.DECIMAL, Types.NUMERIC, Types.REAL, Types.FLOAT, Types.DOUBLE, Types.BIT -> {
                val double: Double? = statement.resultSet.getDouble(column)
                if (double == null) {
                    variant.set(0.0)
                } else {
                    variant.set(double)
                }
            }
            Types.DATE -> {
                val date: Date? = statement.resultSet.getDate(column)
                val sqlDate: java.sql.Date? = statement.resultSet.getDate(column)
                if (date == null) {
                    variant.set(nullDate)
                } else {
                    variant.set(date)
                }
            }
            Types.TIMESTAMP -> {
                val timestamp: Timestamp? = statement.resultSet.getTimestamp(column)
                if (timestamp == null) {
                    variant.set(Timestamp(0))
                } else {
                    variant.set(timestamp)
                }
            }
            Types.TIME -> {
                val time: Time? = statement.resultSet.getTime(column)
                if (time == null) {
                    variant.set(Time(0))
                } else {
                    variant.set(time)
                }
            }
            Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NVARCHAR -> {
                val string: String? = statement.resultSet.getString(column)
                if (columnSize.oneOf(9999, 49999) || columnSize == 2147483647 && string != null && (string.eq("null") || string.startsWith("[") || string.startsWith("{"))) { // implied JSON
                    if (string == null) {
                        variant.set(Json())
                    } else {
                        /*
                        val json = if (string.eq("null")) Json() else Json(string)
                        variant.set(json)
                        */
                        variant.setAsJson(string)
                    }
                } else {
                    if (string == null) {
                        variant.set("")
                    } else {
                        variant.set(string)
                    }
                }
            }
            else -> throw Wobbly("Unsupported data type " + columnType)
        }

    }

    private fun getVariant(column: DbColumn): Variant {
        val variant = values[column.columnIndex]
        if (variant.isEmpty) {
            setValueAsVariant(column.columnIndex + 1, column.columnType, column.displaySize, variant, column.columnName)
            variant.checkPoint()
        }
        return variant
    }

    fun getValue(columnName: String): Variant {
        val column = getColumn(columnName)
        if (column == null) {
            throw  Wobbly("DbDataset.getValue: unknown DbMetaColumn '%s'", columnName)
        } else {
            return getValue(column)
        }
    }

    fun setValue(columnName: String, value: Variant) {
        val column = getColumn(columnName)
        if (column == null) {
            throw  Wobbly("DbDataset.setValue: unknown DbMetaColumn '%s'", columnName)
        } else {
            setValue(column, value)
        }
    }

    private fun getValue(column: DbColumn): Variant {
        if (isOnRow) {
            column.isAccessed = true
            return getVariant(column)
        } else {
            return Variant.nullValue()
        }
    }

    private fun setValue(column: DbColumn, value: Variant) {
        mandate(column.isWritable, "DbDataset.setValue: attempt to set value for unwritable column '${column.columnName}'")
        mandate(isOnRow, "DbDataset.setValue: not on row")
        mandate(rowState==RowState.SELECTED || column.tableName.eq(mainTableName), "DbDataset.setValue: attempt to update sub-table (${column.tableName}) on appended dataset")
        getVariant(column).set(value)
    }

    fun columnsAccessed(): String {
        var result = ""
        for (column in columns) {
            if (column.isAccessed) {
                result = result.append(column.columnLabel)
            }
        }
        return result
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    private fun isModified(column: DbColumn): Boolean {
        if (!column.isWritable) {
            return false
        } else {
            return !values[column.columnIndex].isEmpty && values[column.columnIndex].isModified
        }
    }

    fun isModified(columnName: String): Boolean {
        val column = getColumn(columnName)
        if (column == null) {
            throw  Wobbly("DbDataset.isModified: unknown DbMetaColumn '%s'", columnName)
        } else {
            return isModified(column)
        }
    }

    fun isModified(): Boolean {
        for (column in columns) {
            if (isModified(column)) {
                return true
            }
        }
        return false
    }

    fun isTableModified(tableAlias: String): Boolean {
        for (column in columns) {
            if (column.tableAlias.eq(tableAlias) && isModified(column)) {
                return true
            }
        }
        return false
    }

    fun isColumn(name: String): Boolean {
        return getColumn(name) != null
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    private fun checkPoint() {
        for (value in values) {
            value.checkPoint()
        }
    }

    fun cancel() {
        for (value in values) {
            value.rollBack()
        }
    }

    fun post(ignore: Boolean=false) {
        for (item in tables) {
            val table = item.value
            if (table.hasPrimary && isTableModified(table.tableAlias)) {
                if (rowState == RowState.APPENDING) {
                    if (table==mainTable) {
                        insertTable(table, ignore)
                    }
                    rowState = RowState.APPENDED
                } else {
                    updateTable(table)
                }
            }
        }
        checkPoint()
    }

    fun append(tableName: String): Int {
        mandate(mainTable!=null && tableName.eq(mainTableName), "DbDataset.append: attempt to append to non main table ($tableName)")
        mandate(mainTable?.hasPrimary ?: false, "DbDataset.append: append is not permitted for this dataset")
        rowState = RowState.APPENDING
        rows.clear()
        rowIndex = -1
        return 0
    }

    fun delete(tableName: String): Int {
        mandate(mainTable!=null && tableName.eq(mainTableName), "DbDataset.delete: attempt to delete from non main table ($tableName)")
        if (isOnRow) {
            val _mainTable = mainTable
            if (_mainTable != null && _mainTable.hasPrimary) {
                if (rowState != RowState.APPENDING) {
                    deleteTable(_mainTable)
                }
                if (rowState == RowState.APPENDED) {
                    append(tableName)
                } else {
                    rows.removeAt(rowIndex)
                }
            } else {
                throw Wobbly("DbDataset.delete: delete is not permitted for this dataset")
            }
            if (rowIndex < rowCount) {
                return rowIndex
            } else {
                return rowIndex - 1
            }
        } else {
            return rowIndex
        }
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////
    
    private fun deleteTable(table: Table) {
        var whereClause = ""
        for (column in columns) {
            if (column.tableAlias.eq(table.tableAlias)) {
                if (table.primaryKeys.contains(column.columnName)) {
                    val whereStatement = "%s=%s".format(column.columnName, getValue(column).savedSql)
                    whereClause = whereClause.append(whereStatement, " AND ")
                }
            }
        }
        if (!whereClause.isEmpty()) {
            connection.execute("DELETE FROM %s WHERE %s".format(table.tableName, whereClause))
        }
    }

    private fun updateTable(table: Table) {
        var setClause = ""
        var whereClause = ""

        for (column in columns) {
            if (column.tableAlias.eq(table.tableAlias)) {
                if (!isModified(column)) {
                    when (column.columnName.toLowerCase()) {
                        "datemodified" -> setValue(column, Variant(now))
                        "devicemodified" -> setValue(column, Variant(Global.idDevice))
                    }
                }
                if (isModified(column)) {
                    val setStatement = "`${column.columnName}`=${getValue(column).sql}"
                    setClause = setClause.append(setStatement)
                }
                if (table.primaryKeys.contains(column.columnName)) {
                    val whereStatement = "`${column.columnName}`=${getValue(column).savedSql}"
                    whereClause = whereClause.append(whereStatement, " AND ")
                }
            }
        }

        if (!setClause.isEmpty() && !whereClause.isEmpty()) {
            connection.execute("UPDATE %s SET %s WHERE %s".format(table.tableName, setClause, whereClause))
        }
    }

    private fun insertTable(table: Table, ignore: Boolean, repeat: Int = 0) {
        val columnList = CommaList()
        val valueList = CommaList()
        var emptyListSize = 0
        var randomPrimary = false

        fun addColumn(column: DbColumn) {
            columnList.add(column.columnName.sqlQuoted)
            valueList.add(getValue(column).sql)
        }

        for (column in columns) {
            if (column.tableAlias.eq(table.tableAlias)) {
                if (table.primaryKeys.contains(column.columnName) && !table.autoIncrement) {
                    if (column.columnType == Types.INTEGER && (repeat.between(1, 4) || !isModified(column))) {
                        emptyListSize++
                        setValue(column, Variant(generateId()))
                        randomPrimary = true
                    }
                }

                if (!isModified(column)) {
                    when (column.columnName.toLowerCase()) {
                        "datecreated", "datemodified" -> setValue(column, Variant(now))
                        "devicecreated", "devicemodified" -> setValue(column, Variant(Global.idDevice))
                    }
                }

                if (isModified(column)) {
                    addColumn(column)
                }
            }
        }

        mandate(columnList.size > emptyListSize, "Attempt to form insert SQL when nothing to do")

        try {
            if (ignore) {
                connection.execute("INSERT IGNORE INTO ${table.tableName} ($columnList) VALUES ($valueList)")
            } else {
                connection.execute("INSERT INTO ${table.tableName} ($columnList) VALUES ($valueList)")
            }
        } catch (e: Throwable) {
            if (e.cause is java.sql.SQLIntegrityConstraintViolationException && repeat < 5 && randomPrimary) {
                insertTable(table, ignore, repeat + 1)
            } else {
                throw e
            }
        }


        if (table.autoIncrement) {
            var sqlLookupId: String = ""
            val autoIncColumn=table.primaryKeys[0]
            when (connection.dialect) {
                DbConnection.Dialect.MYSQL -> sqlLookupId = "SELECT LAST_INSERT_ID() as id"
                DbConnection.Dialect.SQLITE -> sqlLookupId = "SELECT last_insert_rowid() as id"
            }
            val query = DbQuery(sqlLookupId, _connection = connection)
            if (query.found()) {
                setValue(autoIncColumn, query.getVariant("id"))
            }
        }
    }



    
}

