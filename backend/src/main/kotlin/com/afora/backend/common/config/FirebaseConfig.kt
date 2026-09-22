package com.afora.backend.common.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import java.io.FileNotFoundException
import javax.annotation.PostConstruct

/**
 * Firebase Admin SDK Configuration.
 *
 * Initializes Firebase Admin SDK for token verification and user management.
 * Reads the service account key from application.yml → firebase.service-account-key.
 */
@Configuration
@EnableConfigurationProperties(FirebaseProperties::class)
class FirebaseConfig(
    private val firebaseProperties: FirebaseProperties,
    private val resourceLoader: ResourceLoader
) {
    
    private val logger = LoggerFactory.getLogger(FirebaseConfig::class.java)

    /**
     * Initialize Firebase Admin SDK on application startup.
     */
    @PostConstruct
    fun initializeFirebase() {
        try {
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                logger.info("Initializing Firebase Admin SDK...")
                
                // Load service account key
                val resource = resourceLoader.getResource(firebaseProperties.serviceAccountKey)
                
                if (!resource.exists()) {
                    throw FileNotFoundException(
                        "Firebase service account key not found at: ${firebaseProperties.serviceAccountKey}. " +
                        "Please download it from Firebase Console and place it in backend/src/main/resources/"
                    )
                }
                
                // Initialize Firebase with service account
                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(resource.inputStream))
                    .build()
                
                FirebaseApp.initializeApp(options)
                
                logger.info("✅ Firebase Admin SDK initialized successfully")
                logger.info("Firebase project ID: ${FirebaseApp.getInstance().options.projectId}")
            } else {
                logger.info("Firebase Admin SDK already initialized")
            }
        } catch (ex: FileNotFoundException) {
            logger.error("❌ Firebase initialization failed: ${ex.message}")
            logger.error("📋 Follow FIREBASE_SETUP.md to configure Firebase")
            throw ex
        } catch (ex: Exception) {
            logger.error("❌ Firebase initialization failed", ex)
            throw RuntimeException("Failed to initialize Firebase Admin SDK", ex)
        }
    }

    /**
     * Provide FirebaseAuth bean for dependency injection.
     */
    @Bean
    fun firebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}
