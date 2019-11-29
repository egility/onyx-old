/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.database

import org.egility.library.general.Global
import org.egility.library.general.removeWhiteSpace

class DbQuery(val sql: String="", val _connection: DbConnection? = null, val silent: Boolean=false) : DbDatasetCursor<DbQuery>() {

    var dataset: DbDataset?=null;

    init {
        if (sql.isNotEmpty()) {
            load(sql)
        }
    }

    constructor(format: String, vararg args: Any) : this(format.format(*args))

    override fun _getDataset(): DbDataset? {
        return dataset
    }

    fun load(sql: String) {
        debugInfo = "QUERY: " + sql.removeWhiteSpace
        if (_connection == null) {
            dataset = Global.connection.createDataset(sql.removeWhiteSpace, silent, reference)
        } else {
            dataset = _connection.createDataset(sql.removeWhiteSpace, silent, reference)
        }
    }

    fun toFirst() : DbQuery {
        first()
        return this
    }


}