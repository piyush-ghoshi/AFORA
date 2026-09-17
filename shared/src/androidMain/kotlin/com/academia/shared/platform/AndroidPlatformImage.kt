package com.academia.shared.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/**
 * Android implementation of PlatformImage using Bitmap.
 */
actual class PlatformImage(private val bitmap: Bitmap) {
    
    actual fun getWidth(): Int = bitmap.width
    
    actual fun getHeight(): Int = bitmap.height
    
    actual fun crop(boundingBox: BoundingBox): PlatformImage {
        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            boundingBox.left.coerceAtLeast(0),
            boundingBox.top.coerceAtLeast(0),
            boundingBox.width.coerceAtMost(bitmap.width - boundingBox.left),
            boundingBox.height.coerceAtMost(bitmap.height - boundingBox.top)
        )
        return PlatformImage(croppedBitmap)
    }
    
    actual fun toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }
    
    /**
     * Android-specific: Get underlying Bitmap.
     */
    fun getBitmap(): Bitmap = bitmap
    
    companion object {
        /**
         * Create PlatformImage from byte array.
         */
        fun fromByteArray(bytes: ByteArray): PlatformImage {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            return PlatformImage(bitmap)
        }
        
        /**
         * Create PlatformImage from existing Bitmap.
         */
        fun fromBitmap(bitmap: Bitmap): PlatformImage {
            return PlatformImage(bitmap)
        }
    }
}
