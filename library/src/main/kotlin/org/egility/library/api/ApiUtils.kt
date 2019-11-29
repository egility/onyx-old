/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.api

import org.egility.library.database.DbDatasetCursor
import org.egility.library.database.DbJdbcConnection
import org.egility.library.dbobject.Account
import org.egility.library.dbobject.Competitor
import org.egility.library.dbobject.Dog
import org.egility.library.general.*
import java.sql.Types
import java.util.*

/**
 * Created by mbrickman on 15/07/15.
 */
object ApiUtils {
    val BROADCAST_ADDRESS = "192.168.2.255"
    val BROADCAST_PORT = 8888
    val BROADCAST_SLEEP_SECONDS = 10
    val API_PORT = 9000
    val API_PORT_OLD = 8000

    fun generateUdpService(service: String, version: String): Json {
        val json = Json()
        json["OK"] = true
        json["kind"] = service
        json["version"] = version
        json["timestamp"] = machineDate.time
        return json
    }

    fun syncAccount(dogCode: Int): Int {
        val plaza = DbJdbcConnection(Sandstone(databaseHost = "10.8.0.1"))
        var idAccount = -1

        dbQuery("SELECT idAccount FROM dog WHERE dogCode=$dogCode", plaza) { idAccount = getInt("idAccount") }
        if (idAccount > 0) {
            val account = Account()
            Account(connection = plaza).seek(idAccount) {
                account.find(id)
                if (!account.found()) {
                    account.append()
                }
                cloneTable(this, account)
                account.post(ignore = true)
            }

            val competitor = Competitor()
            Competitor(connection = plaza).where("idAccount=$idAccount") {
                competitor.find(id)
                if (!competitor.found()) {
                    competitor.append()
                }
                cloneTable(this, competitor)
                competitor.post(ignore = true)
            }

            val dog = Dog()
            Dog(connection = plaza).where("idAccount=$idAccount") {
                dog.find(id)
                if (!dog.found()) {
                    dog.append()
                }
                cloneTable(this, dog)
                dog.post(ignore = true)
            }
            return 0
        }

        return 1

    }


    fun cloneTable(from: DbDatasetCursor<*>, to: DbDatasetCursor<*>) {
        val columns = to.columns
        if (columns != null) {
            for (column in columns) {
                when (column.columnType) {
                    Types.DOUBLE -> to.setValue(column.columnName, from.getDouble(column.columnName))
                    Types.INTEGER, Types.DECIMAL -> to.setValue(column.columnName, from.getInt(column.columnName))
                    Types.VARCHAR, Types.LONGVARCHAR -> to.setValue(column.columnName, from.getString(column.columnName))
                    Types.DATE, Types.TIMESTAMP -> to.setValue(column.columnName, from.getDate(column.columnName))
                    Types.BOOLEAN, Types.BIT -> to.setValue(column.columnName, from.getBoolean(column.columnName))
                    else -> println("unknown type ${column.columnType} for column ${column.className}")
                }
            }
        }

    }


}


