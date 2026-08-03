plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
}

android {
    namespace = "com.cardinalich.kardinalgrab"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cardinalich.kardinalgrab"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        ndk {
            // Современные Android-телефоны. Это уменьшает размер APK.
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*"
        )
    }
}

chaquopy {
    defaultConfig {
        version = "3.13"
        buildPython("python3.13")
        pip {
            // Берём актуальный yt-dlp на момент сборки: Instagram часто меняет выдачу.
            install("yt-dlp")
        }
    }
}
