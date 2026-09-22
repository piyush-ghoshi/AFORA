package com.afora.shared.platform

/**
 * Platform-agnostic image processing utilities.
 * Used for preprocessing faces before recognition.
 */
expect class ImageProcessor {
    /**
     * Resize image to target dimensions.
     */
    suspend fun resize(
        image: PlatformImage,
        targetWidth: Int,
        targetHeight: Int
    ): PlatformImage
    
    /**
     * Normalize image pixel values for ML model input.
     * Typically converts to [-1, 1] or [0, 1] range.
     */
    suspend fun normalize(
        image: PlatformImage,
        mean: FloatArray = floatArrayOf(127.5f, 127.5f, 127.5f),
        std: FloatArray = floatArrayOf(128.0f, 128.0f, 128.0f)
    ): FloatArray
    
    /**
     * Convert image to grayscale.
     */
    suspend fun toGrayscale(image: PlatformImage): PlatformImage
    
    /**
     * Rotate image by degrees.
     */
    suspend fun rotate(image: PlatformImage, degrees: Int): PlatformImage
}
