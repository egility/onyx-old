/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.android.tools

import android.text.Editable
import android.widget.TextView

/**
 * Created by mbrickman on 27/05/15.
 */
interface AndroidOnTextChange {
    fun onTextChange(view: TextView, editable: Editable)
}
