/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.database

import org.egility.library.general.Variant
import org.egility.library.general.networkDate
import org.egility.library.general.oneOf

abstract class DbConnection {

    enum class Dialect {
        UNDEFINED, MYSQL, SQLITE, SQL_SERVER
    }

    var dialect = Dialect.UNDEFINED
    var url = ""
    private var transactionCount = 0

    abstract fun createDataset(sql: String, silent: Boolean, reference: String = ""): DbDataset

    abstract fun execute(sql: String): Boolean

    abstract fun execute(sql: String, vararg args: Any): Boolean

    abstract fun close()

    abstract fun transaction(body: () -> Unit)

    val schema: DbSchema
        get() = DbSchema.getSchema(url)

    fun _getColumnSchema(tableName: String, columnName: String, databaseName: String): DbSchemaColumn? {
        val _schemaTable = _getTableSchema(tableName.trim { it <= ' ' }, databaseName.trim{ it <= ' ' })
        return _schemaTable?.getColumn(columnName)
    }

    fun _getTableSchema(tableName: String, _databaseName: String="sandstone"): DbSchemaTable? {
        val databaseName = if (_databaseName.isNotEmpty()) _databaseName else "sandstone"
        if (tableName == "TABLE_NAMES" || tableName == "objects" || tableName.isEmpty()) {
            return null
        }
        if (schema.tables[tableName] == null) {
            val query: DbQuery
            val table = DbSchemaTable(tableName)
//            try {
                when (dialect) {
                    Dialect.MYSQL -> if (!tableName.oneOf("COLUMNS", "global_variables")) {
                        query = DbQuery("SHOW COLUMNS FROM `$databaseName`.`$tableName`", _connection = this)
                        while (query.next()) {
                            table.addColumn(query.getString("Field"), query.getString("Type"),
                                    query.getString("Null") == "NO", query.getString("Key").contains("PRI"),
                                    query.getString("Extra").contains("auto_increment"), Variant())
                        }
                    }
                    Dialect.SQLITE -> if (!tableName.equals("UNKNOWN", ignoreCase = true)) {
                        query = DbQuery("PRAGMA table_info ('$tableName')", _connection = this)
                        while (query.next()) {
                            table.addColumn(query.getString("name"), query.getString("type"),
                                    query.getBoolean("notnull"), query.getBoolean("pk"),
                                    query.getBoolean("pk") and query.getString("type").equals("INTEGER", ignoreCase = true),
                                    Variant())
                        }
                    }
                    Dialect.SQL_SERVER -> if (!tableName.equals("UNKNOWN", ignoreCase = true)) {
                        query = DbQuery("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='$tableName'", _connection = this)
                        while (query.next()) {
                            table.addColumn(query.getString("COLUMN_NAME"), query.getString("DATA_TYPE"),
                                    query.getBoolean("IS_NULLABLE"), false,
                                    false,
                                    Variant())
                        }
                    }
                }
//            } catch (e: Throwable) {
//                return null // table does not exist
//            }
            schema.tables.put(tableName, table)
        }
        return schema.tables[tableName]
    }

    fun updateNetworkTime() {
        val query = DbQuery("SELECT NOW(3) AS networkDate", _connection = this)
        if (query.found()) {
            networkDate = query.getDate("networkDate")
        }
    }


}
