package com.afora.shared.platform

/**
 * Platform-agnostic logging interface.
 * Android: Logcat
 * iOS: OSLog (future)
 */
expect object Logger {
    fun debug(tag: String, message: String, throwable: Throwable? = null)
    fun info(tag: String, message: String, throwable: Throwable? = null)
    fun warn(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

/**
 * Log levels for filtering.
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}
