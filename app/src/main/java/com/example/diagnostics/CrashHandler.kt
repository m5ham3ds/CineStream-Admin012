package com.example.diagnostics

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import com.example.AppState
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

/**
 * Global uncaught exception handler that detects crashes automatically,
 * generates a comprehensive diagnostic report, persists it to Android data files,
 * and presents a diagnostic recovery screen.
 */
class CrashHandler private constructor(
    private val application: Application,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // 1. Immediately log to AppLogger (which flushes to app_runtime.log on disk)
            AppLogger.crash("CrashHandler", "FATAL CRASH in thread '${thread.name}' (id=${thread.id}): ${throwable.javaClass.simpleName} - ${throwable.message}", throwable)

            // 2. Build full diagnostic crash report
            val report = buildDiagnosticReport(thread, throwable)

            // 3. Persist report to Android data files
            val savedFiles = saveCrashReportToFiles(report)
            val primaryFilePath = savedFiles.firstOrNull()?.absolutePath ?: "Internal Storage"

            // 4. Save to SharedPreferences for in-app retrieval on next launch
            saveToPreferences(report)

            // 5. Launch dedicated CrashReportActivity
            launchCrashReportActivity(report, primaryFilePath)

            // 6. Give time for file IO and activity launch before terminating process
            Thread.sleep(1000)
        } catch (e: Exception) {
            Log.e("CrashHandler", "Error while handling uncaught exception: ${e.message}", e)
        } finally {
            // Kill current crashed process cleanly
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    private fun buildDiagnosticReport(thread: Thread, throwable: Throwable): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", Locale.US)
        val timestampStr = dateFormat.format(Date())

        val stackTrace = StringWriter().also { sw ->
            throwable.printStackTrace(PrintWriter(sw))
        }.toString()

        val runtime = Runtime.getRuntime()
        val maxMemoryMb = runtime.maxMemory() / (1024 * 1024)
        val totalMemoryMb = runtime.totalMemory() / (1024 * 1024)
        val freeMemoryMb = runtime.freeMemory() / (1024 * 1024)
        val usedMemoryMb = totalMemoryMb - freeMemoryMb

        val recentLogs = AppLogger.getRecentLogsText(80)

        val threadsDump = StringBuilder()
        try {
            val allStackTraces = Thread.getAllStackTraces()
            for ((t, stack) in allStackTraces) {
                if (t != thread && stack.isNotEmpty()) {
                    threadsDump.append("Thread: ${t.name} (state=${t.state})\n")
                    stack.take(5).forEach { elem ->
                        threadsDump.append("    at $elem\n")
                    }
                }
            }
        } catch (_: Exception) {}

        return buildString {
            append("=================================================================\n")
            append("           CINESTREAM ADMIN - CRASH DIAGNOSTIC REPORT            \n")
            append("=================================================================\n")
            append("Timestamp        : $timestampStr\n")
            append("Package Name     : ${application.packageName}\n")
            append("Process PID      : ${Process.myPid()}\n")
            append("Crashed Thread   : ${thread.name} (ID: ${thread.id}, State: ${thread.state})\n")
            append("\n")
            append("--- [CRASH ROOT CAUSE] ---\n")
            append("Exception Class  : ${throwable.javaClass.name}\n")
            append("Exception Message: ${throwable.message ?: "No error message provided"}\n")
            append("Cause            : ${throwable.cause?.javaClass?.name ?: "None"}: ${throwable.cause?.message ?: ""}\n")
            append("\n")
            append("--- [STACK TRACE] ---\n")
            append(stackTrace)
            append("\n")
            append("--- [DEVICE & ENVIRONMENT] ---\n")
            append("Brand & Model    : ${Build.BRAND} ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Device Hardware  : ${Build.DEVICE} / ${Build.HARDWARE} (${Build.BOARD})\n")
            append("Android OS       : Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("Build Fingerprint: ${Build.FINGERPRINT}\n")
            append("Supported ABIs   : ${Build.SUPPORTED_ABIS.joinToString(", ")}\n")
            append("\n")
            append("--- [MEMORY & RESOURCE USAGE] ---\n")
            append("Allocated Memory : ${usedMemoryMb}MB\n")
            append("Free Memory      : ${freeMemoryMb}MB\n")
            append("Total Heap Size  : ${totalMemoryMb}MB\n")
            append("Max Heap Limit   : ${maxMemoryMb}MB\n")
            append("\n")
            append("--- [APPLICATION CONTEXT STATE] ---\n")
            append("Firebase Init Err: ${AppState.firebaseInitError ?: "None (Initialized successfully)"}\n")
            append("\n")
            append("--- [ACTIVE THREADS SNAPSHOT] ---\n")
            append(threadsDump.toString().ifEmpty { "No other threads active\n" })
            append("\n")
            append("--- [RECENT LOGCAT / APP DIAGNOSTIC LOGS] ---\n")
            append(recentLogs.ifEmpty { "No runtime logs available\n" })
            append("\n=================================================================\n")
        }
    }

    private fun saveCrashReportToFiles(report: String): List<File> {
        val writtenFiles = mutableListOf<File>()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        // 1. External Files Directory: /Android/data/<package>/files/logs/
        try {
            val extDir = application.getExternalFilesDir(null)
            if (extDir != null) {
                val logsDir = File(extDir, "logs")
                if (!logsDir.exists()) logsDir.mkdirs()

                val latestFile = File(logsDir, "crash_report_latest.txt")
                val historicalFile = File(logsDir, "crash_report_$timeStamp.txt")

                writeStringToFile(latestFile, report)
                writeStringToFile(historicalFile, report)
                writtenFiles.add(latestFile)
            }
        } catch (e: Exception) {
            Log.e("CrashHandler", "Failed to write crash to external storage: ${e.message}")
        }

        // 2. Internal Files Directory: /data/user/0/<package>/files/logs/
        try {
            val intLogsDir = File(application.filesDir, "logs")
            if (!intLogsDir.exists()) intLogsDir.mkdirs()

            val internalLatest = File(intLogsDir, "crash_report_latest.txt")
            val internalHistoric = File(intLogsDir, "crash_report_$timeStamp.txt")

            writeStringToFile(internalLatest, report)
            writeStringToFile(internalHistoric, report)
            if (writtenFiles.isEmpty()) {
                writtenFiles.add(internalLatest)
            }
        } catch (e: Exception) {
            Log.e("CrashHandler", "Failed to write crash to internal storage: ${e.message}")
        }

        return writtenFiles
    }

    private fun writeStringToFile(file: File, content: String) {
        FileOutputStream(file, false).use { fos ->
            fos.write(content.toByteArray(Charsets.UTF_8))
            fos.flush()
            try {
                fos.fd.sync()
            } catch (_: Exception) {}
        }
    }

    private fun saveToPreferences(report: String) {
        try {
            val prefs = application.getSharedPreferences("app_diagnostics_pref", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("last_crash_report", report)
                .putLong("last_crash_time", System.currentTimeMillis())
                .commit()
        } catch (e: Exception) {
            Log.e("CrashHandler", "Failed to write crash to SharedPreferences: ${e.message}")
        }
    }

    private fun launchCrashReportActivity(report: String, filePath: String) {
        try {
            val intent = Intent(application, CrashReportActivity::class.java).apply {
                putExtra(CrashReportActivity.EXTRA_CRASH_REPORT, report)
                putExtra(CrashReportActivity.EXTRA_LOG_PATH, filePath)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            application.startActivity(intent)
        } catch (e: Exception) {
            Log.e("CrashHandler", "Failed to launch CrashReportActivity: ${e.message}", e)
        }
    }

    companion object {
        fun install(application: Application) {
            val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (currentHandler !is CrashHandler) {
                val handler = CrashHandler(application, currentHandler)
                Thread.setDefaultUncaughtExceptionHandler(handler)
                AppLogger.i("CrashHandler", "Global Uncaught Exception & Crash Interceptor installed successfully")
            }
        }
    }
}
