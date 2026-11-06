import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.nyyrox.aniverse"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nyyrox.aniverse"
        minSdk = 26
        targetSdk = 36
        versionCode = 162
        versionName = "1.6.2"
        setProperty("archivesBaseName", "ZENKAI")
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            val properties = Properties()
            if (keystorePropertiesFile.exists()) {
                FileInputStream(keystorePropertiesFile).use { properties.load(it) }
            }

            val storeFilePath = System.getenv("KEYSTORE_FILE")
                ?: System.getenv("RELEASE_KEYSTORE_FILE")
                ?: properties.getProperty("storeFile")
                ?: "keystore/zenkai-release.jks"
            val resolvedStore = rootProject.file(storeFilePath)
            if (resolvedStore.exists()) {
                storeFile = resolvedStore
            } else if (file(storeFilePath).exists()) {
                storeFile = file(storeFilePath)
            }

            storePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: System.getenv("RELEASE_KEYSTORE_PASSWORD")
                ?: properties.getProperty("storePassword")

            keyAlias = System.getenv("KEY_ALIAS")
                ?: System.getenv("RELEASE_KEY_ALIAS")
                ?: properties.getProperty("keyAlias")

            keyPassword = System.getenv("KEY_PASSWORD")
                ?: System.getenv("RELEASE_KEY_PASSWORD")
                ?: properties.getProperty("keyPassword")

            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        val releaseSigning = signingConfigs.getByName("release")
        release {
            isMinifyEnabled = false
            isDebuggable = false
            signingConfig = if (releaseSigning.storeFile?.exists() == true && !releaseSigning.storePassword.isNullOrEmpty()) {
                releaseSigning
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    applicationVariants.all {
        outputs.all {
            val output = this as? com.android.build.gradle.api.ApkVariantOutput
            if (output != null) {
                output.outputFileName = "ZENKAI.apk"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    // Keep dependencies compatible with compileSdk 36 / AGP 8.13.
    val composeBom = platform("androidx.compose:compose-bom:2026.02.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation("io.coil-kt.coil3:coil-gif:3.3.0")
    implementation("com.github.penfeizhou.android.animation:apng:3.0.5")
    implementation("androidx.vectordrawable:vectordrawable-animated:1.2.0")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.11.0")
    implementation("androidx.media3:media3-ui:1.11.0")
    implementation("androidx.media3:media3-ui-compose-material3:1.11.0")
    implementation("androidx.media3:media3-session:1.11.0")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.0")
    implementation("androidx.startup:startup-runtime:1.2.0")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Haze — live backdrop blur for glass UI (Android 12+)
    implementation("dev.chrisbanes.haze:haze:1.7.2")

    // Supabase Kotlin SDK
    val supabaseBom = platform("io.github.jan-tennert.supabase:bom:3.1.4")
    implementation(supabaseBom)
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")

    // Ktor engine (required by Supabase SDK)
    implementation("io.ktor:ktor-client-okhttp:3.1.3")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // Google Play Services Auth (Native Android Google Sign-In)
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // AndroidX WorkManager (Background Notifications)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
