plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.simplet.nav"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.simplet.nav"
        minSdk = 21
        targetSdk = 34
        versionCode = 13
        versionName = "0.13.0-nav"
    }

    signingConfigs {
        // Committed key so every CI build shares one signature and installs in place.
        getByName("debug") {
            storeFile = file("simplet.keystore")
            storePassword = "simplet"
            keyAlias = "simplet"
            keyPassword = "simplet"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    implementation("androidx.core:core-ktx:1.13.1")
}
