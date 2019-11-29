/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import org.egility.library.general.debug
import org.egility.android.tools.AndroidUtils
import org.egility.android.tools.Signal
import org.egility.android.tools.SignalCode
import org.egility.android.views.QuickButton


data class NavigationItem(val caption: String, val signal: SignalCode, val _payload: Any? = null)

class NavigationGroup() : ArrayList<NavigationItem>() {
    fun add(caption: String, signalCode: SignalCode, _payload: Any? = null) {
        add(NavigationItem(caption, signalCode, _payload))
    }
}

class SpinAdapter(val context: Context, val spinItems: NavigationGroup) : BaseAdapter() {
    override fun getItem(position: Int): Any {
        return spinItems[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return spinItems.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = LayoutInflater.from(context).inflate(R.layout.template_spinner_item, null) as TextView
        view.setText(spinItems[position].caption)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = LayoutInflater.from(context).inflate(R.layout.template_spinner_list_item, null) as TextView
        view.setText(spinItems[position].caption)
        return view
    }

}

class SpinListener(var base: BaseFragment, var spinItems: NavigationGroup) : OnItemSelectedListener {

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        base.sendSignal(spinItems[position].signal, spinItems[position]._payload)

    }

}

abstract class BaseFragment(var layout: Int) : Fragment() {

    init {
        doDebug("init")
    }

    var savedState = Bundle()
    var isBackable = true
    var hasView = false
    var hasPaused = false
    var returnedViaBack = false
    var defaultButtonWidth = 240
    var defaultButtonMargin = 12
    var isResuming = false

    open var title = ""

    abstract fun whenClick(view: View)
    abstract fun whenSignal(signal: Signal)

    private fun doDebug(message: String) {
        debug("Activity", "Fragment.${this.javaClass.simpleName}: $message")
    }

    override final fun onActivityCreated(_savedInstanceState: Bundle?) {
        doDebug("onActivityCreated")
        super.onActivityCreated(_savedInstanceState)
    }

    override final fun onCreate(_savedInstanceState: Bundle?) {
        doDebug("onCreate")
        super.onCreate(_savedInstanceState)
    }

    override final fun onCreateView(_inflater: LayoutInflater, _container: ViewGroup?, _savedInstanceState: Bundle?): View? {
        doDebug("onCreateView")
        hasView = true
        return _inflater.inflate(layout, _container, false)
    }

    override final fun onDestroy() {
        doDebug("onDestroy")
        super.onDestroy()
    }

    override /*final*/ fun onDestroyView() {
        doDebug("onDestroyView")
        whenFinalize()
        super.onDestroyView()
        hasView = false
    }

    override final fun onDetach() {
        doDebug("onDetach")
        super.onDetach()
    }

    override final fun onPause() {
        hasPaused = true
        doDebug("onPause")
        super.onPause()
    }

    override final fun onResume() {
        doDebug("onResume")
        super.onResume()
        whenResume()
        if (hasPaused) {
            doDebug("onResumeFromPause")
            whenResumeFromPause()
        }
        hasPaused = false
    }

    override final fun onSaveInstanceState(_savedInstanceState: Bundle) {
        doDebug("onSaveInstanceState")
        super.onSaveInstanceState(_savedInstanceState)
    }

    override final fun onStart() {
        doDebug("onStart")
        super.onStart()
    }

    override final fun onStop() {
        doDebug("onStop")
        super.onStop()
    }

    override final fun onViewCreated(view: View, _savedInstanceState: Bundle?) {
        doDebug("onViewCreated")
    }

    override final fun onViewStateRestored(_savedInstanceState: Bundle?) {
        doDebug("onViewStateRestored")
        super.onViewStateRestored(_savedInstanceState)
        if (isResuming) {
            isResuming = false
        } else {
            whenInitialize()
            sendSignal(SignalCode.RESET_FRAGMENT)
        }
    }

    fun sendSignal(signalCode: SignalCode, _payload: Any? = null, _payload2: Any? = null, queued: Boolean = false) {
        if (activity is BaseActivity) {
            val signal = Signal((activity as BaseActivity), signalCode, _payload, _payload2)
            sendSignal(signal, queued)
        }
    }

    fun sendSignal(signal: Signal, queued: Boolean = false) {
        if (activity is BaseActivity) {
            if (signal.signalCode != SignalCode.PULSE) {
                doDebug("sendSignal (${signal.signalCode.toString()}, delayed=${if (queued) "true" else "false"})")
            }
            (activity as BaseActivity).doSendSignal(signal, queued)
        }
    }

    fun prepareSignal(signalCode: SignalCode, _payload: Any? = null, _payload2: Any? = null): Signal {
        return Signal(activity as BaseActivity, signalCode, _payload, _payload2)
    }

    fun keypress(keyCode: Int) {
        if (activity is BaseActivity) {
            (activity as BaseActivity).keypress(keyCode)
        }
    }

    fun isPatternMatch(match: String): Boolean {
        if (activity is BaseActivity) {
            return (activity as BaseActivity).isPatternMatch(match)
        }
        return false
    }

    fun clearPattern() {
        if (activity is BaseActivity) {
            (activity as BaseActivity).clearPattern()
        }
    }


    fun reset() {
        savedState = Bundle()
    }

    protected open fun whenFinalize() {
        /* placeholder */
    }

    protected open fun whenInitialize() {
        /* placeholder */
    }

    protected open fun whenResume() {
        /* placeholder */
    }

    protected open fun whenResumeFromPause() {
        /* placeholder */
    }

    fun addNavigationButton(viewGroup: ViewGroup, caption: String, signalCode: SignalCode, _payload: Any? = null): QuickButton {
        val signal = prepareSignal(signalCode, _payload)
        val button = activity!!.layoutInflater.inflate(R.layout.template_navigation_button, null) as QuickButton
        button.text = ""
        when (caption) {
            "Back" -> button.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.back, 0, 0, 0)
            "Refresh" -> button.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.refresh, 0, 0, 0)
            "Search" -> button.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.search, 0, 0, 0)
            else -> button.text = caption
        }

        button.id = View.generateViewId()
        button.tag = signal

        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val margin = AndroidUtils.dpToPx(activity!!, 8)
        if (viewGroup.childCount == 0) {
            params.setMargins(0, margin, 0, margin)
        } else {
            params.setMargins(margin * 3, margin, 0, margin)
        }

        viewGroup.addView(button, params)

        return button

    }

    fun addNavigationSpinner(viewGroup: ViewGroup, navigationGroup: NavigationGroup, selected: Int = 0): Spinner {
        val spinner = activity!!.layoutInflater.inflate(R.layout.template_navigation_spinner, null) as Spinner
        spinner.adapter = SpinAdapter(spinner.context, navigationGroup)
        spinner.onItemSelectedListener = SpinListener(this, navigationGroup)
        spinner.setSelection(selected)
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val margin = AndroidUtils.dpToPx(activity!!, 8)
        if (viewGroup.childCount == 0) {
            params.setMargins(0, margin, 0, margin)
        } else {
            params.setMargins(margin * 3, margin, 0, margin)
        }

        viewGroup.addView(spinner, params)

        return spinner
    }

    fun addNavigationGroup(viewGroup: ViewGroup, navigationGroup: NavigationGroup, selected: Int = 0, maxButtons: Int = 0) {
        if (maxButtons > 0 && navigationGroup.size > maxButtons) {
            addNavigationSpinner(viewGroup, navigationGroup, selected)
        } else {
            for (navigationItem in navigationGroup) {
                addNavigationButton(viewGroup, navigationItem.caption, navigationItem.signal, navigationItem._payload)
            }
        }
    }


    fun dpToPx(dp: Int): Int {
        return AndroidUtils.dpToPx(activity!!, dp)
    }

    fun addMenuButton(gridLayout: GridLayout, caption: String, signalCode: SignalCode, _payload: Any? = null, _payload2: Any? = null,
                      buttonWidth: Int = defaultButtonWidth, buttonMargin: Int = defaultButtonMargin, singleLine: Boolean = false): QuickButton {
        return (activity as BaseActivity).addMenuButton(gridLayout, caption, signalCode, _payload, _payload2, buttonWidth, buttonMargin, singleLine)
    }

    fun hideKeyboard() {
        (activity as BaseActivity).hideKeyboard()
    }

    fun showKeyboard() {
        (activity as BaseActivity).showKeyboard()
    }

    fun doActivityForResult(intent: Intent, requestCode: Int, whenResult: (Int, Intent) -> Unit) {
        (activity as BaseActivity).doActivityForResult(intent, requestCode, whenResult)
    }

    fun oneOf(vararg fragments: BaseFragment): Boolean {
        for (fragment in fragments) {
            if (fragment == this) {
                return true
            }
        }
        return false

    }

    fun prepareButton(button: View, signalCode: SignalCode, _payload: Any? = null) {
        button.setOnClickListener { _ ->
            sendSignal(signalCode, _payload)
        }
    }

    var pulseRate: Int
        get() {
            if (activity is BaseActivity) {
                return (activity as BaseActivity).pulseRate
            }
            return 0
        }
        set(value) {
            if (activity is BaseActivity) {
                (activity as BaseActivity).pulseRate = value
            }
        }

}