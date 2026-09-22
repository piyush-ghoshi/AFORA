package com.afora.shared.platform

/**
 * Platform-agnostic face recognition interface.
 * Android: TensorFlow Lite with FaceNet/ArcFace model
 * iOS: CoreML with same model (future)
 * 
 * IMPORTANT: This generates embeddings and matches faces.
 * It does NOT make attendance decisions - that's business logic.
 */
expect class FaceRecognizer {
    /**
     * Generate face embedding from detected face.
     * @return 512-dimensional embedding vector (model-dependent)
     */
    suspend fun generateEmbedding(
        face: DetectedFace,
        image: PlatformImage
    ): FloatArray
    
    /**
     * Match face embedding against known embeddings.
     * @param embedding Current face embedding
     * @param knownEmbeddings Map of studentId -> list of their embeddings
     * @return Recognition result with best match, or null if no match
     */
    suspend fun matchFaces(
        embedding: FloatArray,
        knownEmbeddings: Map<Long, List<FloatArray>>
    ): RecognitionResult?
    
    fun isAvailable(): Boolean
}

/**
 * Face recognition result (CANDIDATE, not final attendance decision).
 */
data class RecognitionResult(
    val studentId: Long,
    val confidence: Double,  // Similarity score (0.0 to 1.0)
    val embeddingDistance: Double? = null,  // Optional cosine distance
    val metadata: Map<String, Any>? = null
)
