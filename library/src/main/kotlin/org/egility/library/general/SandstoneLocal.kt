/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import org.egility.library.database.DbConnection
import org.egility.library.database.DbJdbcConnection
import org.egility.library.database.buildMySqlUrl

/**
 * Created by mbrickman on 13/08/15.
 */
class SandstoneLocal : DbBuilder {

    override val url: String
        get() = buildMySqlUrl("localhost", Global.databasePort, Global.databaseName)

    override val username: String
        get() = Global.MYSQL_USER

    override val password: String
        get() = Global.MYSQL_PASSWORD

    override val dialect: DbConnection.Dialect
        get() = DbConnection.Dialect.MYSQL

    companion object {

        var builder = SandstoneLocal()
    }
}