plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    // Future iOS targets - uncomment when iOS development begins
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                
                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                
                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
                
                // Networking (Ktor)
                implementation("io.ktor:ktor-client-core:2.3.5")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
                implementation("io.ktor:ktor-client-logging:2.3.5")
                implementation("io.ktor:ktor-client-auth:2.3.5")
                
                // DI (Koin)
                implementation("io.insert-koin:koin-core:3.5.0")
            }
        }
        
        val androidMain by getting {
            dependencies {
                // Android Core
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
                
                // Camera
                implementation("androidx.camera:camera-core:1.3.0")
                implementation("androidx.camera:camera-camera2:1.3.0")
                implementation("androidx.camera:camera-lifecycle:1.3.0")
                implementation("androidx.camera:camera-view:1.3.0")
                
                // ML Kit (Face Detection)
                implementation("com.google.mlkit:face-detection:16.1.5")
                
                // TensorFlow Lite (Face Recognition)
                implementation("org.tensorflow:tensorflow-lite:2.14.0")
                implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
                implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
                
                // Ktor Android Engine
                implementation("io.ktor:ktor-client-okhttp:2.3.5")
            }
        }
        
        // Future iOS implementation
        // val iosMain by creating {
        //     dependsOn(commonMain)
        //     dependencies {
        //         implementation("io.ktor:ktor-client-darwin:2.3.5")
        //     }
        // }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation("io.mockk:mockk:1.13.8")
            }
        }
        
        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("io.mockk:mockk-android:1.13.8")
            }
        }
    }
}

android {
    namespace = "com.academia.shared"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 24
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
