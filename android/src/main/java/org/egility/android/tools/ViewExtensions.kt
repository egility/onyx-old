package org.egility.android.tools

import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import org.egility.android.BaseFragment
import org.egility.android.R

fun RadioGroup.addButton(fragment: BaseFragment, caption: String, signalCode: SignalCode, _payload: Any? = null, _payload2: Any? = null): RadioButton? {
    val thisActivity = fragment.activity
    if (thisActivity!=null) {
        val radioButton = thisActivity.layoutInflater.inflate(R.layout.template_radio_button, null) as RadioButton
        radioButton.text = caption
        radioButton.id = View.generateViewId()
        radioButton.tag = fragment.prepareSignal(signalCode, _payload, _payload2)
        this.addView(radioButton)
        return radioButton
    }
    return null
}