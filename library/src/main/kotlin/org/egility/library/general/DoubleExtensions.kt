/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

/**
 * Created by mbrickman on 30/09/17.
 */

val Double.pence
    get() = Math.round(this * 100.00).toInt()