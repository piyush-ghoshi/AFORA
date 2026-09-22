package com.academia.backend.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Firebase configuration properties.
 * Bound from application.yml under the "firebase" prefix.
 *
 * Activated by @EnableConfigurationProperties in FirebaseConfig.
 */
@ConfigurationProperties(prefix = "firebase")
data class FirebaseProperties(
    var serviceAccountKey: String = "classpath:firebase-service-account.json"
)
