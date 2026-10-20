package com.afora.android.ui.attendance

import androidx.lifecycle.SavedStateHandle
import com.afora.shared.data.api.AttendanceConfirmationResponse
import com.afora.shared.data.api.SessionSummary
import com.afora.shared.domain.model.AttendanceStatus
import com.afora.shared.domain.model.SessionStatus
import com.afora.shared.util.AppError
import com.afora.shared.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers the flows added with the Academic Precision restyle:
 * starting a SCHEDULED session, and retrying after a failed confirmation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MarkAttendanceFlowTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeAttendanceRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = MarkAttendanceViewModel(
        repository,
        SavedStateHandle(mapOf(MARK_ATTENDANCE_SESSION_ID_ARG to 1L))
    )

    private fun scheduledRoster() {
        val ok = repository.rosterResult as Result.Success
        repository.rosterResult = Result.Success(ok.data.copy(sessionStatus = SessionStatus.SCHEDULED))
    }

    private fun summary(status: SessionStatus) = SessionSummary(
        sessionId = 1,
        sessionStatus = status,
        sessionDate = "2026-09-17",
        startTime = "09:00",
        endTime = "10:30",
        totalStudents = 2,
        presentCount = 0,
        absentCount = 0,
        onLeaveCount = 0
    )

    private fun confirmation(submissionId: String) = AttendanceConfirmationResponse(
        sessionId = 1,
        submissionId = submissionId,
        sessionStatus = SessionStatus.FINALIZED,
        totalMarked = 2,
        presentCount = 1,
        absentCount = 0,
        onLeaveCount = 1,
        confirmedAt = "2026-09-17T10:00:00",
        replayed = false
    )

    @Test
    fun `startSession activates a scheduled session`() = runTest {
        scheduledRoster()
        repository.activateResult = Result.Success(summary(SessionStatus.ACTIVE))

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(SessionStatus.SCHEDULED, (vm.uiState.value as MarkAttendanceUiState.Success).header.sessionStatus)

        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as MarkAttendanceUiState.Success
        assertEquals(SessionStatus.ACTIVE, state.header.sessionStatus)
        assertEquals(false, state.isSubmitting)
        assertEquals(2, state.rows.size)
    }

    @Test
    fun `startSession failure keeps the roster and surfaces the message`() = runTest {
        scheduledRoster()
        repository.activateResult = Result.Error(AppError.ApiError(403, "Only an administrator may activate"))

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as MarkAttendanceUiState.Error
        assertEquals("Only an administrator may activate", state.message)
        assertEquals(2, state.rows.size)
        assertNotNull(state.header)
    }

    @Test
    fun `startSession does nothing for a session that is already active`() = runTest {
        repository.activateResult = Result.Success(summary(SessionStatus.ACTIVE))

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        val before = vm.uiState.value

        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(before, vm.uiState.value)
    }

    @Test
    fun `confirm can be retried straight from the error state with the same submissionId`() = runTest {
        repository.confirmResult = Result.Error(AppError.NetworkError("offline"))

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.confirm()
        dispatcher.scheduler.advanceUntilIdle()

        val failed = vm.uiState.value as MarkAttendanceUiState.Error
        assertEquals(2, failed.rows.size)
        val firstAttempt = repository.lastConfirmRequest!!.submissionId

        repository.confirmResult = Result.Success(confirmation(firstAttempt))
        vm.confirm() // no row edit in between
        dispatcher.scheduler.advanceUntilIdle()

        val recovered = vm.uiState.value as MarkAttendanceUiState.Success
        assertNotNull(recovered.confirmation)
        assertEquals(SessionStatus.FINALIZED, recovered.header.sessionStatus)
        assertEquals(firstAttempt, repository.lastConfirmRequest!!.submissionId)
    }

    @Test
    fun `editing a row after a failure starts a new attempt with a new submissionId`() = runTest {
        repository.confirmResult = Result.Error(AppError.NetworkError("offline"))

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.confirm()
        dispatcher.scheduler.advanceUntilIdle()
        val firstAttempt = repository.lastConfirmRequest!!.submissionId

        vm.updateStatus(201, AttendanceStatus.ABSENT)
        repository.confirmResult = Result.Success(confirmation("x"))
        vm.confirm()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.lastConfirmRequest!!.submissionId != firstAttempt)
        val request = repository.lastConfirmRequest!!
        assertEquals(AttendanceStatus.ABSENT, request.entries.first { it.studentId == 201L }.status)
    }

    @Test
    fun `selections survive a failed confirmation`() = runTest {
        repository.confirmResult = Result.Error(AppError.ApiError(409, "conflict"))

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.updateStatus(201, AttendanceStatus.LATE)
        vm.confirm()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as MarkAttendanceUiState.Error
        assertEquals(AttendanceStatus.LATE, state.rows.first { it.studentId == 201L }.selectedStatus)
        assertNull(repository.lastAmendRequest)
    }
}