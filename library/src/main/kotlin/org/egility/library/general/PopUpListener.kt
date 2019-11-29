/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.library.general

/**
 * Created by mbrickman on 02/10/15.
 */
interface PopUpListener {


    fun onPopupClick(tag: Int, button: Int)

    companion object {

        val BUTTON_POSITIVE = 1
        val BUTTON_NEGATIVE = -1
        val BUTTON_NEUTRAL = 0
    }
}
