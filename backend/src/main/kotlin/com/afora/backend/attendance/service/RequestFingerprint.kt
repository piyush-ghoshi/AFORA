package com.afora.backend.attendance.service

import com.afora.backend.attendance.dto.SubmittedAttendanceEntry
import java.security.MessageDigest

/**
 * The Request_Fingerprint of Requirement 6, criterion 6.3.
 *
 * A pure, dependency-free object: no repository, no Spring annotation, no
 * database access. [of] computes a SHA-256 digest over the Lecture_Session
 * identifier and the submitted entries sorted by student id ascending, so
 * the result is independent of the order the entries arrived in but
 * sensitive to any change in session id, student id, status or notes.
 *
 * Canonical form, one line per entry:
 *   "<sessionId>\n<studentId>:<status>:<notes ?: "">\n..."
 *
 * `notes` is included because `notes` is persisted; a replay that changes a
 * note is a different submission and must conflict under criterion 6.7.
 * `markUnspecifiedAsAbsent` is deliberately **not** included — that flag
 * changes which students are in the effective entry set, and the effective
 * set is what the digest covers, so the flag's effect is already captured.
 */
object RequestFingerprint {

    /**
     * SHA-256 hex over the session id and the entries sorted by student id
     * ascending. Returned as lowercase hex, 64 characters, matching the
     * `request_fingerprint VARCHAR(64)` column.
     */
    fun of(sessionId: Long, entries: List<SubmittedAttendanceEntry>): String {
        val canonical = buildString {
            append(sessionId)
            entries.sortedBy { it.studentId }.forEach { entry ->
                append('\n')
                append(entry.studentId)
                append(':')
                append(entry.status)
                append(':')
                append(entry.notes ?: "")
            }
        }

        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
