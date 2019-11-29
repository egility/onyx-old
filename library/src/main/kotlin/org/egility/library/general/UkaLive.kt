/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import org.egility.library.database.DbConnection
import org.egility.library.database.DbJdbcConnection
import org.egility.library.database.buildSqlServerUrl


/**
 * Created by mbrickman on 13/11/15.
 */
class UkaLive : DbBuilder {

    override val url: String
        get() = buildSqlServerUrl("channel.safesecureweb.com", 1433, "ukagilit")

    override val username: String
        get() = "ukagilit_readonly"

    override val password: String
        get() = "lkjzdFR&832DS"

    override val dialect: DbConnection.Dialect
        get() = DbConnection.Dialect.SQL_SERVER

    companion object {

        var builder = UkaLive()
    }
}
