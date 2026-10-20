package com.afora.backend.attendance.service

import com.afora.backend.academic.entity.SessionStatus
import com.afora.backend.common.exception.ConflictException

/**
 * The single declared mapping of legal SessionStatus transitions in the
 * Manual_Marking_Flow (Requirement 3, criterion 3.10).
 *
 * A pure, dependency-free object: no repository, no Spring annotation, no
 * database access. [SessionLifecycleService] and [AttendanceConfirmationService]
 * consult it and raise [ConflictException] (HTTP 409) when it reports a
 * transition as illegal.
 *
 * Exactly four transitions are legal (criterion 3.1):
 *   SCHEDULED -> ACTIVE
 *   SCHEDULED -> CANCELLED
 *   ACTIVE    -> CANCELLED
 *   ACTIVE    -> FINALIZED
 *
 * CREATED, SCANNING, REVIEW, FINALIZING and FAILED are the target of no edge
 * here (criterion 3.3) — they stay reserved for the later camera flow, while
 * the backend SessionStatus enum keeps all nine values unchanged (criterion 3.4).
 *
 * `FINALIZED to FINALIZED` is deliberately absent: an amendment (criterion 3.9)
 * is not a transition, it leaves the session at FINALIZED, so the amend path
 * never consults this map.
 */
object SessionTransitions {

    /** The only four legal transitions in the Manual_Marking_Flow (R3.1). */
    val LEGAL: Set<Pair<SessionStatus, SessionStatus>> = setOf(
        SessionStatus.SCHEDULED to SessionStatus.ACTIVE,
        SessionStatus.SCHEDULED to SessionStatus.CANCELLED,
        SessionStatus.ACTIVE to SessionStatus.CANCELLED,
        SessionStatus.ACTIVE to SessionStatus.FINALIZED
    )

    /** True exactly when (from, to) is one of the four declared legal transitions. */
    fun isLegal(from: SessionStatus, to: SessionStatus): Boolean = (from to to) in LEGAL

    /**
     * Throws [ConflictException] for any (from, to) pair outside [LEGAL] (R3.2).
     * No-op when the transition is legal.
     */
    fun require(from: SessionStatus, to: SessionStatus) {
        if (!isLegal(from, to)) {
            throw ConflictException(
                "Illegal session status transition from $from to $to"
            )
        }
    }
}
