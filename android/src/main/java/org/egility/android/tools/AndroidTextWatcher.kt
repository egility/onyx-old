/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.android.tools

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView

/**
 * Created by mbrickman on 27/05/15.
 */
class AndroidTextWatcher(private var view: TextView, private var androidOnTextChange: AndroidOnTextChange) : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

    }

    override fun afterTextChanged(s: Editable) {
        androidOnTextChange.onTextChange(view, s)
    }


}
