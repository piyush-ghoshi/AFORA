plugins {
    // Kotlin
    kotlin("jvm") version "1.9.10" apply false
    kotlin("multiplatform") version "1.9.10" apply false
    kotlin("android") version "1.9.10" apply false
    kotlin("plugin.serialization") version "1.9.10" apply false
    
    // Android
    id("com.android.application") version "8.1.2" apply false
    id("com.android.library") version "8.1.2" apply false
    
    // Hilt
    id("com.google.dagger.hilt.android") version "2.48" apply false
    
    // Google Services (Firebase)
    id("com.google.gms.google-services") version "4.4.0" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
