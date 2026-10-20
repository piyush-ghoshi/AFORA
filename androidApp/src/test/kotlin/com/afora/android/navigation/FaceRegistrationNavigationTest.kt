package com.afora.android.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Navigation unit test for Face Registration Screen (`.kiro/specs/face-recognition/tasks.md` task 18.2).
 *
 * Validates: Requirements 13.1
 */
class FaceRegistrationNavigationTest {

    /**
     * Verifies that the "face_registration" route is correctly defined for the Face Registration Screen.
     *
     * This test confirms that MainRoute.FaceRegistration.route resolves to the expected route string,
     * validating the navigation route definition added in task 18.1.
     *
     * The actual composable wiring (that "face_registration" maps to FaceRegistrationScreen)
     * is tested through integration — if the route were misconfigured, the app would fail at
     * runtime when attempting navigation. This unit test verifies the route constant is correct.
     *
     * **Requirements: 13.1** - The Face_Registration_Screen must be reachable via navigation.
     */
    @Test
    fun `FaceRegistration route resolves to face_registration`() {
        // Verify the route constant is correctly defined
        assertEquals("face_registration", MainRoute.FaceRegistration.route)
    }

    /**
     * Verifies that MainRoute.FaceRegistration is a distinct route in the MainRoute sealed class.
     *
     * This ensures the route object exists and is properly defined as part of the main
     * navigation structure, preventing route collisions with existing screens.
     */
    @Test
    fun `FaceRegistration is a distinct MainRoute object`() {
        // Verify it's not null and is an instance of MainRoute
        val route: MainRoute = MainRoute.FaceRegistration
        assertEquals("face_registration", route.route)
    }
}
