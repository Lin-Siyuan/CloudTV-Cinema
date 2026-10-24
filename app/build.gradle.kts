plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tv.stream"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tv.stream"
        minSdk = 23
        targetSdk = 28 // Matched to Akai CloudTV518 AOSP base
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            // MANDATORY: Akai CloudTV518 runs a 32-bit userland (armeabi-v7a)
            abiFilters.addAll(setOf("armeabi-v7a"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable.addAll(listOf("ExpiredTargetSdkVersion", "MissingDefaultResource"))
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    // Core AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Media3 ExoPlayer (Bounded 10s-20s low-memory playback engine)
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-datasource:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")

    // Networking & Concurrency
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
