package com.afora.shared.platform

/**
 * Platform-agnostic file storage interface.
 * Android: Internal/external storage
 * iOS: Documents directory (future)
 */
expect class FileStorage {
    /**
     * Save data to local file storage.
     * @return File path/identifier
     */
    suspend fun saveFile(
        data: ByteArray,
        fileName: String,
        directory: StorageDirectory = StorageDirectory.CACHE
    ): String
    
    /**
     * Read file from local storage.
     */
    suspend fun readFile(filePath: String): ByteArray?
    
    /**
     * Delete file from local storage.
     */
    suspend fun deleteFile(filePath: String): Boolean
    
    /**
     * Check if file exists.
     */
    suspend fun fileExists(filePath: String): Boolean
    
    /**
     * Get available storage space in bytes.
     */
    suspend fun getAvailableSpace(): Long
}

/**
 * Storage directory types.
 */
enum class StorageDirectory {
    CACHE,       // Temporary files (can be cleared by system)
    FILES,       // App-specific persistent files
    EXTERNAL     // External storage (if available)
}
