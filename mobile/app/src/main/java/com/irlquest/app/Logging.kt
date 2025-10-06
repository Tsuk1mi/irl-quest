package com.irlquest.app

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import timber.log.Timber

object AppLogger {
    private const val LOG_FILE = "app_log.txt"

    fun init(context: Context) {
        // Plant debug tree to log to logcat
        Timber.plant(Timber.DebugTree())
        // Plant file tree to persist logs to cacheDir
        val file = File(context.cacheDir, LOG_FILE)
        try {
            Timber.plant(FileLoggingTree(file))
            Timber.i("AppLogger initialized, logs -> %s", file.absolutePath)
        } catch (e: Exception) {
            // fallback to logcat if file tree fails
            Timber.e(e, "Failed to initialize FileLoggingTree")
        }
    }

    private class FileLoggingTree(private val file: File) : Timber.Tree() {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val level = when (priority) {
                Log.VERBOSE -> "V"
                Log.DEBUG -> "D"
                Log.INFO -> "I"
                Log.WARN -> "W"
                Log.ERROR -> "E"
                Log.ASSERT -> "A"
                else -> "?"
            }

            val timestamp = dateFormat.format(Date())
            val tagPart = tag ?: "App"
            val logLine = "$timestamp $level/$tagPart: $message\n"

            // append to file
            try {
                val writer = FileWriter(file, true)
                writer.use {
                    it.append(logLine)
                    if (t != null) {
                        val sw = java.io.StringWriter()
                        val pw = PrintWriter(sw)
                        t.printStackTrace(pw)
                        it.append(sw.toString())
                    }
                    it.flush()
                }
            } catch (e: Exception) {
                // best effort: if writing to file fails, log to Logcat
                Log.e("AppLogger", "Failed to write log to file: ${e.message}")
            }
        }
    }
}

