package com.academia.shared.util

/**
 * Application configuration.
 * In production, these should be loaded from environment/build config.
 */
object Config {
    /**
     * API base URL.
     * Android: Can be set via BuildConfig
     * iOS: Can be set via Info.plist or build settings
     */
    var API_BASE_URL: String = "http://localhost:8080"
    
    /**
     * API timeout configurations (milliseconds).
     */
    const val API_TIMEOUT_REQUEST = 30_000L
    const val API_TIMEOUT_CONNECT = 15_000L
    const val API_TIMEOUT_SOCKET = 30_000L
    
    /**
     * Pagination defaults.
     */
    const val DEFAULT_PAGE_SIZE = 20
    
    /**
     * Face recognition defaults.
     * IMPORTANT: These are starting points only. Will be configurable.
     */
    const val FACE_RECOGNITION_HIGH_THRESHOLD = 0.85
    const val FACE_RECOGNITION_MEDIUM_THRESHOLD = 0.75
    const val FACE_RECOGNITION_LOW_THRESHOLD = 0.65
    
    /**
     * Camera settings.
     */
    const val CAMERA_TARGET_FPS = 30
    const val CAMERA_TARGET_WIDTH = 1280
    const val CAMERA_TARGET_HEIGHT = 720
    
    /**
     * Storage settings.
     */
    const val MAX_IMAGE_SIZE_MB = 5
    const val MAX_ATTACHMENT_SIZE_MB = 10
}
