package com.afora.shared.di

import com.afora.shared.data.api.ApiClient
import org.koin.dsl.module

/**
 * Koin module for networking dependencies.
 */
val networkModule = module {
    
    /**
     * API Client singleton.
     * Token provider can be injected from auth repository.
     */
    single {
        ApiClient(
            baseUrl = "http://localhost:8080",  // TODO: Make configurable
            tokenProvider = {
                // Will be provided by AuthRepository
                null
            }
        )
    }
}
