package com.academia.backend.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Firebase Configuration properties.
 * Phase A2: Firebase Authentication integration.
 */
@Configuration
@ConfigurationProperties(prefix = "firebase")
data class FirebaseProperties(
    var serviceAccountKey: String = "classpath:firebase-service-account.json"
)
