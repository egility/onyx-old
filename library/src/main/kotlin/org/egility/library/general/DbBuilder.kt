/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

import org.egility.library.database.DbConnection

/**
 * Created by mbrickman on 13/08/15.
 */
interface DbBuilder {

    val url: String
    val username: String
    val password: String
    val dialect: DbConnection.Dialect

}
