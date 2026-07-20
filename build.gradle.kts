// File: build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.trackdaysp.interlagos"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.trackdaysp.interlagos"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "2.6"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    
    // CameraX (RF02)
    val camerax_version = "1.3.1"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-video:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")

    // Google Sign-In & Drive (RF01 & RF06)
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.api-client:google-api-client-android:1.33.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")

    // WorkManager (RF06)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // EncryptedSharedPreferences (RF09)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Gson for JSON Parsing (RF03)
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Room
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
}
