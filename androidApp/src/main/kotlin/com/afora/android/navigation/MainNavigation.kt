package com.afora.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navigation
import androidx.navigation.compose.composable
import com.afora.android.ui.attendance.MARK_ATTENDANCE_SESSION_ID_ARG
import com.afora.android.ui.attendance.MarkAttendanceScreen
import com.afora.android.ui.attendance.MarkAttendanceViewModel
import com.afora.android.ui.attendance.StudentAttendanceScreen
import com.afora.android.ui.attendance.StudentAttendanceViewModel
import com.afora.android.ui.attendance.camera.CAMERA_ATTENDANCE_SESSION_ID_ARG
import com.afora.android.ui.attendance.camera.CameraAttendanceScreen
import com.afora.android.ui.attendance.camera.CameraAttendanceViewModel
import com.afora.android.ui.dashboard.profile.ProfileScreen
import com.afora.android.ui.dashboard.profile.ProfileViewModel
import com.afora.android.ui.dashboard.student.StudentDashboardScreen
import com.afora.android.ui.dashboard.student.StudentDashboardViewModel
import com.afora.android.ui.dashboard.teacher.TeacherDashboardScreen
import com.afora.android.ui.dashboard.teacher.TeacherDashboardViewModel
import com.afora.android.ui.faceprofile.FaceRegistrationScreen
import com.afora.android.ui.faceprofile.FaceRegistrationViewModel
import com.afora.android.ui.leave.StudentLeaveScreen
import com.afora.android.ui.leave.StudentLeaveViewModel
import com.afora.android.ui.leave.TeacherLeaveReviewScreen
import com.afora.android.ui.leave.TeacherLeaveReviewViewModel

/** Root graph route for the main app (post-auth). */
const val MAIN_GRAPH_ROUTE = "main"

/**
 * Main app navigation routes.
 */
sealed class MainRoute(val route: String) {
    data object StudentDashboard : MainRoute("student_dashboard")
    data object TeacherDashboard : MainRoute("teacher_dashboard")
    data object Profile : MainRoute("profile")
    data object MarkAttendance : MainRoute("mark_attendance/{$MARK_ATTENDANCE_SESSION_ID_ARG}") {
        fun createRoute(sessionId: Long): String = "mark_attendance/$sessionId"
    }
    data object CameraAttendance : MainRoute("camera_attendance/{$CAMERA_ATTENDANCE_SESSION_ID_ARG}") {
        fun createRoute(sessionId: Long): String = "camera_attendance/$sessionId"
    }
    data object StudentLeave : MainRoute("student_leave")
    data object TeacherLeaveReview : MainRoute("teacher_leave_review")
    data object StudentAttendance : MainRoute("student_attendance")
    data object FaceRegistration : MainRoute("face_registration")
}

/**
 * Main app navigation graph.
 *
 * Entry is determined by role:
 *  - STUDENT  → StudentDashboard
 *  - TEACHER  → TeacherDashboard
 *
 * Both roles can navigate to Profile. A teacher can navigate from their
 * dashboard's schedule into MarkAttendance for one of their own sessions, or
 * from the "Pending Leaves" stat into TeacherLeaveReview. A student can
 * navigate from their own "Pending Leaves" stat into StudentLeave.
 */
fun NavGraphBuilder.mainNavGraph(
    navController: NavHostController,
    startRoute: String,
    onLogout: () -> Unit
) {
    navigation(
        startDestination = startRoute,
        route = MAIN_GRAPH_ROUTE
    ) {

        // ── Student Dashboard ─────────────────────────────────────────────────
        composable(MainRoute.StudentDashboard.route) {
            val viewModel: StudentDashboardViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            StudentDashboardScreen(
                uiState = uiState,
                onNavigateToProfile = {
                    navController.navigate(MainRoute.Profile.route)
                },
                onRetry = viewModel::load,
                onNavigateToLeave = {
                    navController.navigate(MainRoute.StudentLeave.route)
                },
                onNavigateToAttendance = {
                    navController.navigate(MainRoute.StudentAttendance.route)
                }
            )
        }

        // ── Teacher Dashboard ─────────────────────────────────────────────────
        composable(MainRoute.TeacherDashboard.route) {
            val viewModel: TeacherDashboardViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            TeacherDashboardScreen(
                uiState = uiState,
                onNavigateToProfile = {
                    navController.navigate(MainRoute.Profile.route)
                },
                onRetry = viewModel::load,
                onSessionClick = { sessionId ->
                    navController.navigate(MainRoute.MarkAttendance.createRoute(sessionId))
                },
                onNavigateToLeaveReview = {
                    navController.navigate(MainRoute.TeacherLeaveReview.route)
                },
                onScanCameraClick = { sessionId ->
                    navController.navigate(MainRoute.CameraAttendance.createRoute(sessionId))
                }
            )
        }

        // ── Mark Attendance ──────────────────────────────────────────────────
        composable(
            route = MainRoute.MarkAttendance.route,
            arguments = listOf(
                navArgument(MARK_ATTENDANCE_SESSION_ID_ARG) { type = NavType.LongType }
            )
        ) {
            val viewModel: MarkAttendanceViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            MarkAttendanceScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onStatusChange = viewModel::updateStatus,
                onConfirm = viewModel::confirm,
                onAmendmentReasonChange = viewModel::updateAmendmentReason,
                onSubmitAmendment = viewModel::submitAmendment,
                onStartSession = viewModel::startSession,
                onRetry = viewModel::load,
                onScanWithCamera = {
                    navController.navigate(MainRoute.CameraAttendance.createRoute(viewModel.sessionId))
                }
            )
        }

        // ── Camera Attendance ────────────────────────────────────────────────
        composable(
            route = MainRoute.CameraAttendance.route,
            arguments = listOf(
                navArgument(CAMERA_ATTENDANCE_SESSION_ID_ARG) { type = NavType.LongType }
            )
        ) {
            val viewModel: CameraAttendanceViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            CameraAttendanceScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onSwitchToManual = {
                    val sessionId = viewModel.sessionId
                    navController.popBackStack()
                    navController.navigate(MainRoute.MarkAttendance.createRoute(sessionId))
                },
                onFaceDetections = viewModel::onFaceDetections,
                onToggleTorch = viewModel::toggleTorch,
                onToggleScanning = viewModel::toggleScanning,
                onSetReviewMode = viewModel::setReviewMode,
                onUpdateCandidateStatus = viewModel::updateCandidateStatus,
                onConfirmAttendance = viewModel::confirmAttendance,
                onRetry = viewModel::loadSession
            )
        }

        // ── Student Leave Requests ───────────────────────────────────────────
        composable(MainRoute.StudentLeave.route) {
            val viewModel: StudentLeaveViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            StudentLeaveScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onSubmit = { from, to, reason, classSectionId ->
                    viewModel.submit(from, to, reason, classSectionId)
                },
                onCancel = viewModel::cancel,
                onRetry = viewModel::load,
                onNavigateHome = {
                    navController.popBackStack(MainRoute.StudentDashboard.route, inclusive = false)
                },
                onNavigateProfile = {
                    navController.navigate(MainRoute.Profile.route)
                }
            )
        }

        // ── Face Registration ────────────────────────────────────────────────
        composable(MainRoute.FaceRegistration.route) {
            val viewModel: FaceRegistrationViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            FaceRegistrationScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onAffirmConsent = viewModel::affirmConsent,
                onCaptureShot = viewModel::captureShot,
                onRegister = viewModel::register
            )
        }

        // ── Student Attendance Detail ────────────────────────────────────────
        composable(MainRoute.StudentAttendance.route) {
            val viewModel: StudentAttendanceViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            StudentAttendanceScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onToggleSubject = viewModel::toggleSubject,
                onRetry = viewModel::load
            )
        }

        // ── Teacher Leave Review ─────────────────────────────────────────────
        composable(MainRoute.TeacherLeaveReview.route) {
            val viewModel: TeacherLeaveReviewViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            TeacherLeaveReviewScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onApprove = { leaveRequestId -> viewModel.approve(leaveRequestId) },
                onReject = { leaveRequestId, notes -> viewModel.reject(leaveRequestId, notes) },
                onRetry = viewModel::load,
                onNavigateHome = {
                    navController.popBackStack(MainRoute.TeacherDashboard.route, inclusive = false)
                },
                onNavigateProfile = {
                    navController.navigate(MainRoute.Profile.route)
                }
            )
        }

        // ── Profile (shared) ──────────────────────────────────────────────────
        composable(MainRoute.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            ProfileScreen(
                uiState = uiState,
                onSave = viewModel::onSave,
                onFirstNameChange = viewModel::onFirstNameChange,
                onLastNameChange = viewModel::onLastNameChange,
                onBackClick = { navController.popBackStack() },
                onLogout = onLogout,
                onRetry = viewModel::load,
                onNavigateHome = {
                    // Pop back to whichever dashboard this user's graph started on.
                    navController.popBackStack(MainRoute.StudentDashboard.route, inclusive = false) ||
                        navController.popBackStack(MainRoute.TeacherDashboard.route, inclusive = false)
                },
                onNavigateToLeave = { navController.navigate(MainRoute.StudentLeave.route) },
                onNavigateToLeaveReview = { navController.navigate(MainRoute.TeacherLeaveReview.route) }
            )
        }
    }
}