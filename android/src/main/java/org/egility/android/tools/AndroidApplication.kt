/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.android.tools

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.Display
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import org.egility.library.general.Wobbly
import org.egility.library.general.debug
import org.egility.library.general.panic
import org.egility.android.BaseActivity
//import org.egility.android.BaseActivity
import java.util.*

var _application: AndroidApplication? = null
val androidApplication: AndroidApplication
    get() {
        return _application ?: throw Wobbly("No Application")
    }

class AndroidApplication : Application(), Application.ActivityLifecycleCallbacks {

    private enum class State {
        UNDEFINED, CREATED, STARTED, RESUMED, PAUSED, STOPPED, DESTROYED
    }

    private data class ActivityState(var activity: Activity, var state: State)

    private val activityStack = ArrayList<ActivityState>()
    lateinit public var mainThread: Thread

    val currentActivity: Activity
        get() {
            val activityState = activityStack[activityStack.size - 1]
            return activityState.activity
        }

    override fun onCreate() {
        super.onCreate()
        _application = this
        mainThread = Thread.currentThread()
        debug("Application", "onCreate")
        NetworkObject.disconnect()
        AndroidServices.initialize()
        registerActivityLifecycleCallbacks(this)
        Thread.setDefaultUncaughtExceptionHandler(UncaughtHandler())
        broadcastReceiver.register(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        getActivityState(activity).state = State.CREATED
        debug("Application", activity.localClassName + " - onActivityCreated")
    }

    override fun onActivityStarted(activity: Activity) {
        getActivityState(activity).state = State.STARTED
        debug("Application", activity.localClassName + " - onActivityStarted")
    }

    override fun onActivityResumed(activity: Activity) {
        getActivityState(activity).state = State.RESUMED
        debug("Application", activity.localClassName + " - onActivityResumed")
    }

    override fun onActivityPaused(activity: Activity) {
        getActivityState(activity).state = State.PAUSED
        debug("Application", activity.localClassName + " - onActivityPaused")
    }

    override fun onActivityStopped(activity: Activity) {
        getActivityState(activity).state = State.STOPPED
        debug("Application", activity.localClassName + " - onActivityStopped")
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        debug("Application", activity.localClassName + " - onActivitySaveInstanceState")
    }

    override fun onActivityDestroyed(activity: Activity) {
        activityStack.remove(getActivityState(activity))
        debug("Application", activity.localClassName + " - onActivityDestroyed")
    }

    private fun getActivityState(activity: Activity): ActivityState {
        for (activityState in activityStack) {
            if (activity === activityState.activity) {
                return activityState
            }
        }
        val activityState = ActivityState(activity, State.UNDEFINED)
        activityStack.add(activityState)
        return activityState
    }

    private fun getIndex(activity: Activity): Int {
        return activityStack.indexOf(getActivityState(activity))
    }


    fun sendSignal(signalCode: SignalCode, _payload: Any? = null, _payload2: Any? = null, queued: Boolean = false) {
        val activeActivity = _focusedActivity
        if (activeActivity != null && activeActivity is BaseActivity) {
            val signal = Signal(activeActivity, signalCode, _payload, _payload2)
            activeActivity.sendSignal(signal, queued)
        }
    }

    val _focusedActivity: Activity?
        get() {
            for (activityState in activityStack) {
                if (activityState.activity.hasWindowFocus()) {
                    return activityState.activity
                }
            }
            return activityStack.last().activity
        }

    val inflater: LayoutInflater
        get() {
            return getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }

    val inputManager: InputMethodManager
        get() {
            return getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        }

    fun kill() {
        for (item in activityStack) {
            val activity = item.activity
            if (activity is BaseActivity && !activity.isMain) {
                activity.finish()
            }
        }
        AndroidUtils.kill()
    }

    val display: Display
    get() {

        val windowManager=getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return windowManager.defaultDisplay?:throw Wobbly("unable to access display")

    }
}


private class UncaughtHandler() : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, e: Throwable) {
        val main = Looper.myLooper() == Looper.getMainLooper()
        if (main) {
            debug("Panic", "UncaughtHandler (main thread)")
        } else {
            debug("Panic", "UncaughtHandler (sub thread: ${thread.id})")
        }
        panic(e)
    }

}