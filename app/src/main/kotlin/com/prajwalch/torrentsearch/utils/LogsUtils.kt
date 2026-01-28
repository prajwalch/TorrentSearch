package com.prajwalch.torrentsearch.utils

import android.os.Build

import com.prajwalch.torrentsearch.BuildConfig

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

import java.io.OutputStream
import java.io.PrintWriter

object LogsUtils {
    /** Message to write to the logs file when `logcat` execution fails. */
    private const val LOGCAT_EXEC_ERROR_MESSAGE = "FAILED TO EXECUTE LOGCAT"

    /**
     * Exports the logs from the `logcat` to the given [OutputStream].
     *
     * If stack trace is provided, it is placed above the actual logs.
     */
    suspend fun exportLogsToOutputStream(
        outputStream: OutputStream,
        stackTrace: String? = null,
    ): Unit = withContext(NonCancellable) {
        val logsPrintWriter = PrintWriter(outputStream.bufferedWriter(Charsets.UTF_8))
        val logcatExecResult = executeLogcatCatching()

        logsPrintWriter.use { logsPrinter ->
            logsPrinter.println(getAppAndDeviceInfo())

            stackTrace?.let {
                logsPrinter.println(it)
                logsPrinter.println()
            }

            // If logcat execution was successful, write the actual logs.
            logcatExecResult.onSuccess { process ->
                val logsBufferedReader = process.inputStream.bufferedReader(Charsets.UTF_8)
                logsBufferedReader.forEachLine(logsPrinter::println)

                process.destroy()
            }

            // If logcat execution was unsuccessful, write the message with
            // the exception that was occurred when attempting to execute.
            logcatExecResult.onFailure { exception ->
                logsPrinter.println(LOGCAT_EXEC_ERROR_MESSAGE)
                exception.printStackTrace(writer = logsPrinter)
            }
        }
    }

    /**
     * Safely executes the `logcat` by catching all exception.
     *
     * When writing logs file, any additional errors shouldn't occur.
     * If some function or method throws then catch all the exceptions
     * and report them too.
     */
    private fun executeLogcatCatching(): Result<Process> = runCatching {
        Runtime.getRuntime().exec("logcat -d")
    }

    /** Returns the information of TorrentSearch and current device. */
    private fun getAppAndDeviceInfo() = """
        App ID              : ${BuildConfig.APPLICATION_ID}
        App version         : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) 
        Device manufacturer : ${Build.MANUFACTURER}
        Device brand        : ${Build.BRAND}
        Device name         : ${Build.DEVICE} (${Build.PRODUCT})
        Device model        : ${Build.MODEL}
        Android version     : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
    """.trimIndent()
}