package com.afora.shared.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * STUB IMPLEMENTATIONS FOR PHASE A1 FOUNDATION
 * These will be fully implemented in Task #7 (Android platform implementations).
 * 
 * Purpose: Allow KMP module to compile successfully.
 * Status: NOT FUNCTIONAL - placeholder only.
 */

actual class CameraController {
    actual fun startCamera(config: CameraConfig): Flow<CameraFrame> {
        TODO("CameraController implementation - Task #7")
    }
    
    actual fun stopCamera() {
        TODO("CameraController implementation - Task #7")
    }
    
    actual suspend fun captureFrame(): CameraFrame {
        TODO("CameraController implementation - Task #7")
    }
    
    actual fun isAvailable(): Boolean {
        return false // Stub: always return false for now
    }
}

actual class FaceDetector {
    actual suspend fun detectFaces(image: PlatformImage): List<DetectedFace> {
        TODO("FaceDetector implementation - Task #7")
    }
    
    actual fun isAvailable(): Boolean {
        return false // Stub: always return false for now
    }
}

actual class FaceRecognizer {
    actual suspend fun generateEmbedding(
        face: DetectedFace,
        image: PlatformImage
    ): FloatArray {
        TODO("FaceRecognizer implementation - Task #7")
    }
    
    actual suspend fun matchFaces(
        embedding: FloatArray,
        knownEmbeddings: Map<Long, List<FloatArray>>
    ): RecognitionResult? {
        TODO("FaceRecognizer implementation - Task #7")
    }
    
    actual fun isAvailable(): Boolean {
        return false // Stub: always return false for now
    }
}

actual class ImageProcessor {
    actual suspend fun resize(
        image: PlatformImage,
        targetWidth: Int,
        targetHeight: Int
    ): PlatformImage {
        TODO("ImageProcessor implementation - Task #7")
    }
    
    actual suspend fun normalize(
        image: PlatformImage,
        mean: FloatArray,
        std: FloatArray
    ): FloatArray {
        TODO("ImageProcessor implementation - Task #7")
    }
    
    actual suspend fun toGrayscale(image: PlatformImage): PlatformImage {
        TODO("ImageProcessor implementation - Task #7")
    }
    
    actual suspend fun rotate(image: PlatformImage, degrees: Int): PlatformImage {
        TODO("ImageProcessor implementation - Task #7")
    }
}

actual class FileStorage {
    actual suspend fun saveFile(
        data: ByteArray,
        fileName: String,
        directory: StorageDirectory
    ): String {
        TODO("FileStorage implementation - Task #7")
    }
    
    actual suspend fun readFile(filePath: String): ByteArray? {
        TODO("FileStorage implementation - Task #7")
    }
    
    actual suspend fun deleteFile(filePath: String): Boolean {
        TODO("FileStorage implementation - Task #7")
    }
    
    actual suspend fun fileExists(filePath: String): Boolean {
        TODO("FileStorage implementation - Task #7")
    }
    
    actual suspend fun getAvailableSpace(): Long {
        TODO("FileStorage implementation - Task #7")
    }
}

actual class NetworkMonitor {
    actual fun observeConnectivity(): Flow<NetworkStatus> {
        return flowOf(NetworkStatus.UNKNOWN) // Stub: return unknown status
    }
    
    actual suspend fun isConnected(): Boolean {
        return false // Stub: assume not connected
    }
    
    actual suspend fun getNetworkType(): NetworkType {
        return NetworkType.UNKNOWN // Stub: return unknown type
    }
}
