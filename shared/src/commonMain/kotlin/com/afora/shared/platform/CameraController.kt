package com.afora.shared.platform

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic camera control interface.
 * Android: CameraX implementation
 * iOS: AVFoundation implementation (future)
 */
expect class CameraController {
    fun startCamera(config: CameraConfig): Flow<CameraFrame>
    fun stopCamera()
    suspend fun captureFrame(): CameraFrame
    fun isAvailable(): Boolean
}

/**
 * Camera configuration for attendance scanning.
 */
data class CameraConfig(
    val targetResolution: Resolution = Resolution.HD_720P,
    val frameRate: Int = 30,
    val autoFocus: Boolean = true
) {
    companion object {
        fun default() = CameraConfig()
    }
}

/**
 * Single camera frame with metadata.
 */
data class CameraFrame(
    val image: PlatformImage,
    val timestamp: Long,
    val rotation: Int = 0
)

/**
 * Common resolution presets.
 */
enum class Resolution(val width: Int, val height: Int) {
    VGA(640, 480),
    HD_720P(1280, 720),
    HD_1080P(1920, 1080)
}
