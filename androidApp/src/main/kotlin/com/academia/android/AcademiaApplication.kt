package com.academia.android

import android.app.Application
import com.academia.shared.di.initKoin
import dagger.hilt.android.HiltAndroidApp
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

/**
 * Application class for AFORA Android app.
 * Initializes Hilt (Android DI) and Koin (shared KMP DI).
 */
@HiltAndroidApp
class AcademiaApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Koin for shared module
        initKoin {
            androidLogger(Level.ERROR)  // Only log errors to avoid spam
            androidContext(this@AcademiaApplication)
        }
    }
}
