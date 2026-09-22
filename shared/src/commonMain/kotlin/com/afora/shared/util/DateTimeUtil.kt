package com.afora.shared.util

import kotlinx.datetime.*

/**
 * Date/time utility functions using kotlinx.datetime.
 */
object DateTimeUtil {
    
    /**
     * Get current datetime as ISO 8601 string.
     */
    fun now(): String {
        return Clock.System.now().toString()
    }
    
    /**
     * Get current date as ISO 8601 string (YYYY-MM-DD).
     */
    fun today(): String {
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    }
    
    /**
     * Parse ISO 8601 datetime string to Instant.
     */
    fun parseDateTime(dateTimeString: String): Instant? {
        return try {
            Instant.parse(dateTimeString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse ISO 8601 date string to LocalDate.
     */
    fun parseDate(dateString: String): LocalDate? {
        return try {
            LocalDate.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Format Instant to human-readable string.
     */
    fun formatDateTime(instant: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val localDateTime = instant.toLocalDateTime(timeZone)
        return "${localDateTime.date} ${localDateTime.time}"
    }
    
    /**
     * Format LocalDate to human-readable string.
     */
    fun formatDate(date: LocalDate): String {
        return date.toString()
    }
    
    /**
     * Check if date is in the past.
     */
    fun isPast(dateString: String): Boolean {
        val date = parseDate(dateString) ?: return false
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return date < today
    }
    
    /**
     * Check if date is in the future.
     */
    fun isFuture(dateString: String): Boolean {
        val date = parseDate(dateString) ?: return false
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return date > today
    }
    
    /**
     * Calculate days between two dates.
     */
    fun daysBetween(startDate: String, endDate: String): Int? {
        val start = parseDate(startDate) ?: return null
        val end = parseDate(endDate) ?: return null
        return end.toEpochDays() - start.toEpochDays()
    }
}
