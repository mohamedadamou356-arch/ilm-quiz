plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ilmquiz.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ilmquiz.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "11.19.0"
    }

    // Une seule clé fixe pour toutes les compilations : les mises à jour s'installent par-dessus.
    signingConfigs {
        create("fixed") {
            storeFile = file("ilm-debug.jks")
            storePassword = "ilmquiz2026"
            keyAlias = "ilmkey"
            keyPassword = "ilmquiz2026"
            storeType = "pkcs12"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fixed")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixed")
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
