package com.afora.backend.common.security

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseToken
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Firebase Authentication Filter.
 * 
 * Intercepts HTTP requests and validates Firebase ID tokens from Authorization header.
 * 
 * Flow:
 * 1. Extract token from "Authorization: Bearer <token>" header
 * 2. Validate token using Firebase Admin SDK
 * 3. Extract user info (UID, email, etc.) from token
 * 4. Set Spring Security authentication context
 * 5. Allow request to proceed to controller
 */
@Component
class FirebaseAuthenticationFilter(
    private val firebaseAuth: FirebaseAuth
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(FirebaseAuthenticationFilter::class.java)

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // Extract token from header
            val token = extractTokenFromRequest(request)

            if (token != null) {
                // Verify token with Firebase
                val decodedToken = verifyFirebaseToken(token)

                if (decodedToken != null) {
                    // Set authentication in Spring Security context
                    setAuthentication(decodedToken, request)
                    logger.debug("Authenticated user: ${decodedToken.uid} (${decodedToken.email})")
                }
            }
        } catch (ex: Exception) {
            logger.error("Error processing Firebase token", ex)
            // Don't block request - let it proceed to endpoint (which may require auth)
        }

        // Continue filter chain
        filterChain.doFilter(request, response)
    }

    /**
     * Extract Firebase ID token from Authorization header.
     * 
     * Expected format: "Authorization: Bearer <firebase-id-token>"
     */
    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val authHeader = request.getHeader(AUTHORIZATION_HEADER)

        return if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            authHeader.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }

    /**
     * Verify Firebase ID token using Firebase Admin SDK.
     * 
     * @param token Firebase ID token string
     * @return Decoded Firebase token with user claims, or null if invalid
     */
    private fun verifyFirebaseToken(token: String): FirebaseToken? {
        return try {
            // Verify token with Firebase Admin SDK
            firebaseAuth.verifyIdToken(token)
        } catch (ex: FirebaseAuthException) {
            logger.warn("Invalid Firebase token: ${ex.message}")
            null
        } catch (ex: Exception) {
            logger.error("Error verifying Firebase token", ex)
            null
        }
    }

    /**
     * Set Spring Security authentication context with Firebase user info.
     * 
     * Creates authentication object with:
     * - Principal: Firebase UID
     * - Credentials: null (token already verified)
     * - Authorities: User roles from custom claims
     */
    private fun setAuthentication(decodedToken: FirebaseToken, request: HttpServletRequest) {
        // Extract user role from custom claims (if set)
        val role = decodedToken.claims["role"] as? String ?: "ROLE_USER"
        val authorities = listOf(SimpleGrantedAuthority(role))

        // Create authentication object
        val authentication = UsernamePasswordAuthenticationToken(
            decodedToken.uid,  // Principal: Firebase UID
            null,              // Credentials: not needed (token verified)
            authorities        // Authorities: user roles
        )

        // Set request details
        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

        // Set in Security Context
        SecurityContextHolder.getContext().authentication = authentication
    }

    /**
     * Override to skip filter for certain paths (optional).
     * Currently applies to all requests.
     */
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        
        // Skip filter for public endpoints
        return path.startsWith("/api/health") ||
               path.startsWith("/api/public") ||
               path.startsWith("/actuator") ||
               path.startsWith("/error")
    }
}
