package com.example.diagnostics

import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, CRASH
}

data class LogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val threadName: String = Thread.currentThread().name,
    val throwableStackTrace: String? = null
)

/**
 * High-performance, instantaneous diagnostic logger that persists logs in real-time
 * to Android internal and external app data directories (/Android/data/<pkg>/files/logs/).
 */
object AppLogger {
    private const val DEFAULT_TAG = "CineStreamAdmin"
    private const val MAX_MEMORY_LOGS = 1000
    private const val MAX_LOG_FILE_SIZE = 5 * 1024 * 1024 // 5 MB

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val timeShortFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val memoryLogBuffer = ConcurrentLinkedDeque<LogEntry>()
    private val _logsState = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsState: StateFlow<List<LogEntry>> = _logsState.asStateFlow()

    @Volatile
    private var appContext: Context? = null
    private var externalLogDir: File? = null
    private var internalLogDir: File? = null
    private var runtimeLogFile: File? = null
    private val fileLock = Any()

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        try {
            // 1. External files dir: /storage/emulated/0/Android/data/<package>/files/logs
            val extDir = app.getExternalFilesDir(null)
            if (extDir != null) {
                val logsDir = File(extDir, "logs")
                if (!logsDir.exists()) logsDir.mkdirs()
                externalLogDir = logsDir
            }

            // 2. Internal files dir: /data/user/0/<package>/files/logs
            val intLogsDir = File(app.filesDir, "logs")
            if (!intLogsDir.exists()) intLogsDir.mkdirs()
            internalLogDir = intLogsDir

            // Target runtime log file in external storage (preferred for user access) or internal
            val targetDir = externalLogDir ?: internalLogDir
            if (targetDir != null) {
                runtimeLogFile = File(targetDir, "app_runtime.log")
                rotateLogIfNeeded(runtimeLogFile!!)
            }

            i("AppLogger", "=== Real-Time Diagnostic Logger Initialized ===")
            i("AppLogger", "Package: ${app.packageName}, Process PID: ${Process.myPid()}")
            i("AppLogger", "Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
            i("AppLogger", "Log Directory: ${targetDir?.absolutePath ?: "Memory-only"}")
        } catch (e: Exception) {
            Log.e(DEFAULT_TAG, "Failed to initialize AppLogger disk target: ${e.message}", e)
        }
    }

    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String, tr: Throwable? = null) = log(LogLevel.WARN, tag, message, tr)
    fun e(tag: String, message: String, tr: Throwable? = null) = log(LogLevel.ERROR, tag, message, tr)
    fun crash(tag: String, message: String, tr: Throwable? = null) = log(LogLevel.CRASH, tag, message, tr)

    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val now = System.currentTimeMillis()
        val formattedTime = synchronized(timeShortFormat) { timeShortFormat.format(Date(now)) }
        val threadName = Thread.currentThread().name

        val stackTrace = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString()
        }

        val entry = LogEntry(
            timestamp = now,
            timeFormatted = formattedTime,
            level = level,
            tag = tag,
            message = message,
            threadName = threadName,
            throwableStackTrace = stackTrace
        )

        // 1. Android Logcat (Safely guarded against unmocked JVM testing environments)
        try {
            when (level) {
                LogLevel.DEBUG -> Log.d(tag, message, throwable)
                LogLevel.INFO -> Log.i(tag, message, throwable)
                LogLevel.WARN -> Log.w(tag, message, throwable)
                LogLevel.ERROR -> Log.e(tag, message, throwable)
                LogLevel.CRASH -> Log.wtf(tag, message, throwable)
            }
        } catch (_: Throwable) {
            // Ignored on pure JVM test environments where android.util.Log is not mocked
        }

        // 2. Memory Ring Buffer
        memoryLogBuffer.addLast(entry)
        while (memoryLogBuffer.size > MAX_MEMORY_LOGS) {
            memoryLogBuffer.pollFirst()
        }
        _logsState.value = memoryLogBuffer.toList()

        // 3. Instantaneous Real-Time Disk Write & Flush
        writeToFileImmediately(entry)
    }

    private fun writeToFileImmediately(entry: LogEntry) {
        val file = runtimeLogFile ?: return
        synchronized(fileLock) {
            try {
                rotateLogIfNeeded(file)
                val fullTimestamp = synchronized(timeFormat) { timeFormat.format(Date(entry.timestamp)) }
                val line = buildString {
                    append("[$fullTimestamp] [${entry.level.name}] [${entry.threadName}] ${entry.tag}: ${entry.message}\n")
                    if (entry.throwableStackTrace != null) {
                        append(entry.throwableStackTrace)
                        if (!entry.throwableStackTrace.endsWith("\n")) append("\n")
                    }
                }

                FileOutputStream(file, true).use { fos ->
                    val bytes = line.toByteArray(Charsets.UTF_8)
                    fos.write(bytes)
                    fos.flush()
                    try {
                        fos.fd.sync()
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.e(DEFAULT_TAG, "Error writing to runtime log file: ${e.message}")
            }
        }
    }

    private fun rotateLogIfNeeded(file: File) {
        if (file.exists() && file.length() > MAX_LOG_FILE_SIZE) {
            try {
                val parent = file.parentFile
                val backupFile = File(parent, "app_runtime_prev.log")
                if (backupFile.exists()) backupFile.delete()
                file.renameTo(backupFile)
            } catch (_: Exception) {}
        }
    }

    fun getLogFile(): File? = runtimeLogFile

    fun getLogsDirectory(): File? = externalLogDir ?: internalLogDir

    fun getCrashFile(): File? {
        val dir = getLogsDirectory() ?: return null
        val latestCrash = File(dir, "crash_report_latest.txt")
        return if (latestCrash.exists()) latestCrash else null
    }

    fun getRecentLogsText(limit: Int = 100): String {
        val list = memoryLogBuffer.toList().takeLast(limit)
        return list.joinToString("\n") { entry ->
            val trace = if (entry.throwableStackTrace != null) "\n" + entry.throwableStackTrace else ""
            "[${entry.timeFormatted}] [${entry.level}] [${entry.threadName}] ${entry.tag}: ${entry.message}$trace"
        }
    }

    fun getAllLogsText(): String {
        return memoryLogBuffer.joinToString("\n") { entry ->
            val trace = if (entry.throwableStackTrace != null) "\n" + entry.throwableStackTrace else ""
            "[${entry.timeFormatted}] [${entry.level}] [${entry.threadName}] ${entry.tag}: ${entry.message}$trace"
        }
    }

    fun clearMemoryLogs() {
        memoryLogBuffer.clear()
        _logsState.value = emptyList()
        i("AppLogger", "Memory logs cleared by user")
    }
}
