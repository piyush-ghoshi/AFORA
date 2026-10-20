package com.afora.backend.attendance.service

import com.afora.backend.academic.entity.LectureSession
import com.afora.backend.attendance.dto.AmendAttendanceRequestDto
import com.afora.backend.attendance.dto.AmendmentResultResponse
import com.afora.backend.attendance.dto.AttendanceConfirmationResponse
import com.afora.backend.attendance.dto.AuditHistoryResponse
import com.afora.backend.attendance.dto.ConfirmAttendanceRequestDto
import com.afora.backend.attendance.dto.RosterResponse
import com.afora.backend.attendance.dto.SessionSummary
import com.afora.backend.attendance.security.CallerContext
import org.springframework.stereotype.Service

/**
 * Thin public facade over the four attendance collaborators. The controller
 * sees only this class and the [com.afora.backend.attendance.security.AttendanceAccessGuard],
 * never a repository directly (R10.12).
 */
@Service
class AttendanceService(
    private val rosterService: AttendanceRosterService,
    private val lifecycleService: SessionLifecycleService,
    private val confirmationService: AttendanceConfirmationService,
    private val amendmentService: AttendanceAmendmentService
) {
    fun roster(session: LectureSession): RosterResponse =
        rosterService.roster(session)

    fun activate(session: LectureSession, caller: CallerContext): SessionSummary =
        lifecycleService.activate(session, caller)

    fun cancel(session: LectureSession, caller: CallerContext): SessionSummary =
        lifecycleService.cancel(session, caller)

    fun confirm(
        sessionId: Long,
        caller: CallerContext,
        request: ConfirmAttendanceRequestDto
    ): AttendanceConfirmationResponse =
        confirmationService.confirm(sessionId, caller, request)

    fun amend(
        sessionId: Long,
        caller: CallerContext,
        request: AmendAttendanceRequestDto
    ): AmendmentResultResponse =
        amendmentService.amend(sessionId, caller, request)

    fun auditHistory(sessionId: Long): AuditHistoryResponse =
        amendmentService.auditHistory(sessionId)
}