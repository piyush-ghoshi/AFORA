package com.academia.shared.platform

/**
 * Platform-agnostic image representation.
 * Actual implementations provided by androidMain/iosMain.
 */
expect class PlatformImage {
    fun getWidth(): Int
    fun getHeight(): Int
    fun crop(boundingBox: BoundingBox): PlatformImage
    fun toByteArray(): ByteArray
}

/**
 * Bounding box for face detection/cropping.
 */
data class BoundingBox(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
)
