pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AFORA"

include(":shared")
include(":androidApp")

// Backend uses separate Gradle project (Java/Gradle vs Kotlin/KGP)
