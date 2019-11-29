/*
 * Copyright (c) Mike Brickman 2014-2017
 */

package org.egility.android.tools

import android.content.Context
import org.egility.library.general.*
import java.util.*

/**
 * Created by mbrickman on 04/11/15.
 */

object AndroidPanic {

    var haveChecked = false
    private var _hasPanic = false
    var panicTime = nullDate
    var panicClass = ""
    var message = ""
    var stack = ""
    var userInformed = false

    fun panic(throwable: Throwable) {
        try {
            debugLog(throwable)
        } catch (e: Throwable) {
            AndroidUtils.kill()
        }
//        androidApplication.kill()
        AndroidUtils.kill()

    }

    fun debugLog(throwable: Throwable) {
        val cause = throwable.cause
        if (cause == null) {
            debug("AndroidPanic", "${throwable.javaClass.simpleName}: ${throwable.message} at $realNow")
            writeJson(throwable)
        } else {
            debugLog(cause)
            debug("AndroidPanic", "Causing - ${throwable.javaClass.simpleName}: ${throwable.message} at $realNow")
        }
        val stackTrace = throwable.stackTrace
        if (stackTrace != null) {
            for (element in stackTrace) {
                debug("AndroidPanic", "...... $element")
            }
        } else {
            debug("AndroidPanic", "...... No Stack Information")
        }
    }

    fun writeJson(throwable: Throwable) {
        val filesList = androidApplication.fileList()
        if (filesList.contains("panic")) {
            androidApplication.deleteFile("panic")
        }

        val outFile = androidApplication.openFileOutput("panic", Context.MODE_WORLD_READABLE)
        val report = Json()
        report["OK"] = true
        report["kind"] = "panic"
        report["time"] = now.time
        report["class"] = throwable.javaClass.simpleName
        report["message"] = throwable.message ?: "Unknown"
        report["stack"] = throwable.stack
        report.save(outFile)
        outFile.flush()
        outFile.close()
    }

    val hasPanic: Boolean
        get() {
            if (!haveChecked) {
                clearPanic()
                val filesList = androidApplication.fileList()
                if (filesList.contains("panic")) {
                    try {
                        val inFile = androidApplication.openFileInput("panic")
                        val report = Json(inFile)
                        _hasPanic = true
                        panicTime = Date(report["time"].asLong)
                        panicClass = report["class"].asString
                        message = report["message"].asString
                        stack = report["stack"].asString
                    } catch (e: Throwable) {
                        clearPanic()
                        e.printStackTrace()
                    }
                    androidApplication.deleteFile("panic")
                }
                haveChecked = true;
                userInformed = false
            }
            return _hasPanic

        }

    fun clearPanic() {
        _hasPanic = false
        panicTime = nullDate
        panicClass = ""
        message = ""
        stack = ""
    }

}





