plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

android {
    namespace = "com.agency.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.agency.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    
    // Material Design 3
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // ViewModel & Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // Dependency Injection - Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Room Database (Offline-first)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // WebSocket (Real-time Sync)
    implementation("org.java-websocket:Java-WebSocket:1.5.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Camera & Barcode
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    
    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    
    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Date/Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}
