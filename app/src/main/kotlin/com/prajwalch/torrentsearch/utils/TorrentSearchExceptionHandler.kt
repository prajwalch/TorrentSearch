package com.prajwalch.torrentsearch.utils

import android.content.Context
import android.content.Intent
import android.util.Log

import com.prajwalch.torrentsearch.BuildConfig

class TorrentSearchExceptionHandler(
    private val context: Context,
    private val activityToLaunch: Class<*>,
) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        Log.e(TAG, "Application crashed!", exception)

        startGivenActivity(exception)
        defaultHandler?.uncaughtException(thread, exception)
    }

    private fun startGivenActivity(exception: Throwable) {
        val crashIntent = Intent(context, activityToLaunch).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_CRASH_STACKTRACE, exception.stackTraceToString())
        }
        context.startActivity(crashIntent)

        Log.i(TAG, "${activityToLaunch.simpleName} started successfully")
    }

    companion object {
        private const val TAG = "TorrentSearchExceptionHandler"
        private const val EXTRA_CRASH_STACKTRACE = "${BuildConfig.VERSION_NAME}.CRASH_STACKTRACE"

        fun getCrashStackTrace(intent: Intent): String =
            intent.getStringExtra(EXTRA_CRASH_STACKTRACE)!!
    }
}