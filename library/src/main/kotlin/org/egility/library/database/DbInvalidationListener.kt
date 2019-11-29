/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.database

/**
 * Created by mbrickman on 21/04/15.
 */
interface DbInvalidationListener {
    fun invalidated(listeners: MutableList<DbInvalidationListener>)
}
