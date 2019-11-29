/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.GridLayout
import org.egility.library.general.*
import org.egility.android.tools.*
import org.egility.android.views.QuickButton
import java.util.*

/**
 * Created by mbrickman on 06/12/14.
 */


open class BaseActivity(val layout: Int, val isMain: Boolean = false) : FragmentActivity(), SignalListener {
    private var _defaultFragmentContainerId = 0
    var defaultButtonWidth = 240
    var defaultButtonMargin = 12


    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (
//                View.SYSTEM_UI_FLAG_IMMERSIVE
                2048
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    // Shows the system bars by removing all the flags
// except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }


    protected var defaultFragmentContainerId: Int
        get() {
            if (_defaultFragmentContainerId == 0) {
                return window.decorView.id
            } else {
                return _defaultFragmentContainerId
            }
        }
        set(value) {
            _defaultFragmentContainerId = value
        }

    private var threadInstance = 0

    private val BACK_ON_PIN = 1
    private val UNLOCK_ON_PIN = 2

    var dnr = false

    init {
        doDebug("init")
        dnr = !isMain && !Global.initialized
        if (dnr) {
            doDebug("Do not resuscitate")
        }
    }

    enum class LoadType { TOP, STACK, SWAP, BACK }

    var keyPattern = ""
    private var isActive = false

    private var resumed = false
    private var windowFocused = false
    private var surfaceReady = false
    private var hasPaused = false


    private lateinit var fragmentManager: FragmentManager
    private var _activeFragment: BaseFragment? = null
    private val stack = Stack<BaseFragment>()

    private var _pulseTask: PulseTask? = null
    var pulseRate = 0
        set(value) {
            if (value != field) {
                field = value
                val pulseTask = _pulseTask
                if (value > 0 && isActive && pulseTask == null) {
                    startPulse()
                } else if (value <= 0) {
                    stopPulse()
                }
            }
        }

    private fun startPulse() {
        val pulseTask = _pulseTask
        if (pulseRate > 0 && pulseTask == null) {
            val newPulseTask = PulseTask("Pulse_${javaClass.simpleName}_${++threadInstance}")
            _pulseTask = newPulseTask
            newPulseTask.start()
        }
    }

    private fun stopPulse() {
        val pulseTask = _pulseTask
        if (pulseTask != null) {
            pulseTask.interrupt()
            _pulseTask = null
        }
    }

    private fun doDebug(message: String) {
        debug("Activity", "${this.javaClass.simpleName}: $message")
    }

    override final fun onAttachedToWindow() {
        doDebug("onAttachedToWindow")
        super.onAttachedToWindow()
    }

    override final fun onCreate(savedInstanceState: Bundle?) {
        if (dnr) {
            super.onCreate(savedInstanceState)
            finish()
            return
        }
        if (isMain) {
            Global.initialized = true
        }
        resumed = false
        hasPaused = false
        windowFocused = false
        surfaceReady = false
        doDebug("onCreate")
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        fragmentManager = supportFragmentManager
        setContentView(layout)
        whenInitialize()
        sendSignal(SignalCode.RESET, queued = true)

    }

    override final fun onRestart() {
        doDebug("onRestart")
        super.onRestart()
    }

    override final fun onStart() {
        doDebug("onStart")
        super.onStart()
    }

    override final fun onResume() {

        if (dnr) {
            super.onResume()
            return
        }


        resumed = true
        isActive = true
        doDebug("onResume")
        startPulse()
        super.onResume()
        whenResume()
        if (hasPaused) {
            doDebug("onResumeFromPause")
            whenResumeFromPause()
        }
        hasPaused = false
    }

    override final fun onPause() {
        hasPaused = true
        resumed = false
        stopPulse()
        isActive = false
        doDebug("onPause")
        super.onPause()
        whenPause()
    }

    override final fun onStop() {
        doDebug("onStop")
        super.onStop()
    }

    override final fun onDestroy() {
        doDebug("onDestroy")
        whenFinalize()
        super.onDestroy()
    }

    override final fun onDetachedFromWindow() {
        doDebug("onDetachedFromWindow")
        super.onDetachedFromWindow()
    }

    override final fun onPostCreate(savedInstanceState: Bundle?) {
        doDebug("onPostCreate")
        super.onPostCreate(savedInstanceState)
    }

    override final fun onRestoreInstanceState(savedInstanceState: Bundle) {
        doDebug("savedInstanceState")
        super.onRestoreInstanceState(savedInstanceState)
    }

    override final fun onSaveInstanceState(savedInstanceState: Bundle) {
        doDebug("onSaveInstanceState")
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (dnr) {
            super.onWindowFocusChanged(hasFocus)
            return
        }
        windowFocused = hasFocus
        doDebug("onWindowFocusChanged = ${if (hasFocus) "true" else "false"}")
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            Global.activityName = this.localClassName.substringAfter(".")
            if (AndroidUtils.isFire) hideSystemUI()
            whenFocus()
        }
    }

    override final fun onBackPressed() {
        /* do nothing */
    }

    open protected fun whenFocus() {
        /* do nothing */
    }


    fun clearPattern() {
        keyPattern = ""
    }

    fun isPatternMatch(match: String): Boolean {
        val result = keyPattern.endsWith(match)
        if (result) {
            clearPattern()
        }
        return result
    }

    protected fun loadTopFragment(fragment: BaseFragment) {
        fragment.reset()
        loadFragment(fragment, LoadType.TOP)
    }

    protected fun loadFragment(fragment: BaseFragment) {
        fragment.reset()
        loadFragment(fragment, LoadType.STACK)
    }

    protected fun resumeFragment(fragment: BaseFragment) {
        //fragment.reset()
        fragment.isResuming = true
        loadFragment(fragment, LoadType.STACK)
    }

    protected fun swapFragment(fragment: BaseFragment) {
        fragment.reset()
        loadFragment(fragment, LoadType.SWAP)
    }

    private val stackList: String
        get() {
            var result = ""
            for (fragment in stack) {
                result = result.append(fragment.javaClass.simpleName)
            }
            return result
        }

    protected fun back() {
        doDebug("back")
        if (stack.empty()) {
            finish()
        } else {
            val fragment = stack.pop()
            doDebug("back - stack.pop: $stackList")
            whenBeforeBack(fragment)
            loadFragment(fragment, LoadType.BACK)
        }
    }

    private fun isFragmentOnStack(fragment: BaseFragment): Boolean {
        return stack.search(fragment) != -1
    }

    fun setActiveFragmentTitle(title: String) {
        val activeFragment = _activeFragment
        if (activeFragment != null) {
            activeFragment.title = title
        }
    }

    fun loadFragment(fragment: BaseFragment, type: LoadType) {
        doDebug("loadFragment (${fragment.javaClass.simpleName}, ${type.toString()})")
        if (!isActiveFragment(fragment)) {
            fragment.returnedViaBack = type == LoadType.BACK
            val activeFragment = _activeFragment
            if (activeFragment != null) {
                val view = activeFragment.view
                if (view != null) {
                    view.visibility = View.INVISIBLE
                }
            }
            whenBeforeLoadFragment(fragment)
            when (type) {
                LoadType.TOP -> {
                    while (!stack.empty()) {
                        stack.pop()
                        doDebug("top - stack.pop: $stackList")
                    }
                }
                LoadType.STACK, LoadType.BACK -> {
                    if (isFragmentOnStack(fragment)) {
                        while (stack.peek() != fragment) {
                            stack.pop()
                            doDebug("back 1 - stack.pop: $stackList")
                        }
                        stack.pop()
                        doDebug("back 2 - stack.pop: $stackList")
                    } else if (activeFragment != null && activeFragment.isBackable && type == LoadType.STACK) {
                        stack.push(activeFragment)
                        doDebug("stack.push: $stackList")
                    }
                }
                LoadType.SWAP -> {
                }
            }
            val transaction = fragmentManager.beginTransaction()
            transaction.replace(defaultFragmentContainerId, fragment)
            transaction.commit()
            _activeFragment = fragment
        }
    }

    fun removeFragment(fragment: BaseFragment) {
        doDebug("removeFragment (${fragment.javaClass.simpleName})")
        val transaction = fragmentManager.beginTransaction()
        transaction.remove(fragment)
        transaction.commit()
    }

    protected fun isActiveFragment(fragment: BaseFragment?): Boolean {
        return _activeFragment != null && fragment != null && _activeFragment === fragment
    }

    fun doActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    var whenResult: (Int, Intent) -> Unit = { _, _ -> doNothing()}
    
    var whenRequestCode = 0

    fun doActivityForResult(intent: Intent, requestCode: Int, whenResult: (Int, Intent) -> Unit) {
        this.whenRequestCode = requestCode
        this.whenResult = whenResult
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,  data: Intent?) {
        if (whenRequestCode == requestCode) {
            if (data!=null) whenResult(resultCode, data)
        } else if (requestCode == BACK_ON_PIN) {
            finish()
        } else if (requestCode == UNLOCK_ON_PIN) {
            /* do nothing */
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }


    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> {
                sendSignal(SignalCode.KEYBOARD_ENTER)
                return true
            }
            KeyEvent.KEYCODE_PAGE_UP -> {
                if (AndroidServices.device.equals("T62D") || AndroidServices.device.equals("T61") || AndroidServices.isT63) {
                    keyPattern += "u"
                    sendSignal(SignalCode.PAGE_UP)
                } else {
                    keyPattern += "d"
                    sendSignal(SignalCode.PAGE_DOWN)
                }
                return true
            }
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                if (AndroidServices.device.equals("T62D") || AndroidServices.device.equals("T61") || AndroidServices.isT63) {
                    keyPattern += "d"
                    sendSignal(SignalCode.PAGE_DOWN)
                } else {
                    keyPattern += "u"
                    sendSignal(SignalCode.PAGE_UP)
                }
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                keyPattern += "b"
                sendSignal(SignalCode.BACK)
                return true
            }
            else -> return super.onKeyUp(keyCode, event)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        /*
        if (event.repeatCount == 0) {
            when (keyCode) {
            }
        }
        */
        return false
    }

    fun prepareSignal(signalCode: SignalCode, _payload: Any? = null, _payload2: Any? = null): Signal {
        return Signal(this, signalCode, _payload, _payload2)
    }

    fun sendSignal(signalCode: SignalCode, _payload: Any? = null, _payload2: Any? = null, queued: Boolean = false) {
        val signal = Signal(this, signalCode, _payload, _payload2)
        sendSignal(signal, queued)
    }

    fun sendSignal(signal: Signal, queued: Boolean = false) {
        if (signal.signalCode != SignalCode.PULSE) {
            doDebug("sendSignal (${signal.signalCode.toString()}, delayed=${if (queued) "true" else "false"})")
        }
        doSendSignal(signal, queued)
    }

    fun doSendSignal(signal: Signal, queued: Boolean = true) {
        val mainHandler = Handler(mainLooper)
        if (queued) {
            mainHandler.post(signal)
        } else {
            runOnUiThread(signal)
        }
    }

    open fun whenClick(view: View) {
        keyPattern = ""
        _activeFragment?.whenClick(view)
    }

    override fun onSignal(signal: Signal) {
        whenSignalReceived(signal)
    }

    protected fun whenSignalReceived(signal: Signal) {
        _activeFragment?.whenSignal(signal)
        if (!signal.isConsumed) {
            whenSignal(signal)
        }
    }

    protected open fun whenSignal(signal: Signal) {
        when (signal.signalCode) {
            SignalCode.BACK -> {
                back()
                signal.consumed()
            }
            else -> {
                doNothing()
            }
        }
    }

    fun keypress(keyCode: Int) {
        val target = currentFocus
        if (target is EditText) {
            val text = target.text.toString()
            if (text.isEmpty() && keyCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z) {
                target.dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, 0))
                target.dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0))
                target.dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode, 0))
                target.dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT, 0))
            } else {
                target.dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0))
                target.dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode, 0))
            }
        }
    }


    protected open fun whenInitialize() {
    }

    protected open fun whenFinalize() {
    }

    protected open fun whenPause() {
    }

    protected open fun whenResumeFromPause() {

    }

    protected open fun whenResume() {
    }

    protected open fun whenBeforeBack(goingTo: BaseFragment) {
    }

    protected open fun whenBeforeLoadFragment(fragment: BaseFragment) {
    }

    internal inner class PulseTask(name: String) : Thread(name) {

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        private var wakeLock= powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, name + "_wake_lock")

        override fun run() {
            var lastExecute = 0L
            debug("Thread", getName() + " - Running")
            wakeLock.acquire()
            while (pulseRate > 0 && !isInterrupted) {
                try {
                    if (pulseRate==9999 || cpuTime > lastExecute + pulseRate * 1000) {
                        lastExecute = cpuTime
                        sendSignal(SignalCode.PULSE)
                    }
                    sleep(100L)
                } catch (e: InterruptedException) {
                    interrupt()
                } catch (e: Throwable) {
                    /* just keep going */
                }

            }
            debug("Thread", getName() + " - Exiting")
            wakeLock.release()
        }
    }

    fun addMenuButton(gridLayout: GridLayout, caption: String, signalCode: SignalCode, _payload: Any? = null, _payload2: Any? = null,
                      buttonWidth: Int = defaultButtonWidth, buttonMargin: Int = defaultButtonMargin, singleLine: Boolean = false): QuickButton {
        val buttonWidthPx = AndroidUtils.dpToPx(this, buttonWidth)
        val buttonMarginPx = AndroidUtils.dpToPx(this, buttonMargin)
        val signal = prepareSignal(signalCode, _payload, _payload2)
        val resource = if (singleLine) R.layout.template_option_button_single_line else R.layout.template_option_button
        val button = this.layoutInflater.inflate(resource, null) as QuickButton
        button.text = caption.replace("  ", " ").trim()
        button.id = View.generateViewId()
        button.tag = signal

        val params = GridLayout.LayoutParams()
        params.width = buttonWidthPx
        params.setMargins(buttonMarginPx, buttonMarginPx, buttonMarginPx, buttonMarginPx)

        gridLayout.addView(button, params)
        return button
    }

    fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            androidApplication.inputManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun showKeyboard() {
        val view = currentFocus
        androidApplication.inputManager.showSoftInput(view, 0)
    }

    fun doInBackground(success: SignalCode, fail: SignalCode, body: () -> Unit) {
        BackgroundTask(body, { sendSignal(success) }, { e -> sendSignal(fail, e) }).execute()
    }

    fun doBackground(body: () -> Signal) {
        SignalTask(this, body).execute()
    }
    
    

    
}
