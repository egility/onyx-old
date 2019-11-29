/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.android.tools

import android.app.Activity
import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Process.sendSignal
import android.view.ContextThemeWrapper
import org.egility.android.BaseActivity
import org.egility.android.R

/**
 * Created by mbrickman on 15/04/16.
 */
class BackgroundTask(val body: ()->Unit, val onComplete: ()->Unit, val onFail: (Throwable)->Unit) : AsyncTask<Int, Int, Throwable>() {

    internal var _exception: Throwable? = null

    override fun doInBackground(vararg params: Int?): Throwable? {
        try {
            body()
        } catch (e: Throwable) {
            return e
        }
        return null
    }

    override fun onPostExecute(result: Throwable?) {
        if (result == null) {
            onComplete()
        } else {
            onFail(result)
        }
    }
}

class SignalTask(val activity: BaseActivity, val body: ()->Signal) : AsyncTask<Int, Int, Signal>() {
    
    override fun doInBackground(vararg params: Int?): Signal {
        try {
            return body()
        } catch (e: Throwable) {
            return activity.prepareSignal(SignalCode.BACKGROUND_EXCEPTION, e)
        }
    }

    override fun onPostExecute(result: Signal) {
        activity.sendSignal(result)
        
    }

}

fun doBackground(body: ()->Unit, onComplete: ()->Unit) {
    BackgroundTask(body, onComplete, { e-> throw e}).execute()
}

fun doHourglass(activity: Activity?, message: String, body: ()->Unit, onComplete: ()->Unit) {
    if (activity!=null) {
        val dialog = ProgressDialog(ContextThemeWrapper(activity, R.style.Text24))
        dialog.setMessage(message)
        dialog.isIndeterminate = true
        dialog.setCancelable(false)
        dialog.show()
        doBackground(body, {
            dialog.cancel()
            onComplete()
        })
    }
}

fun doThinking(activity: Activity?, body: ()->Unit, onComplete: ()->Unit) {
    doHourglass(activity, "Thinking about it", body, onComplete)

}
