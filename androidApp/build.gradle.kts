plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.prajwalch.torrentsearch.android"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.prajwalch.torrentsearch"
        minSdk = 25
        targetSdk = 36
        versionCode = 17
        versionName = "0.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs (for IzzyOnDroid/F-Droid).
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles (for Google Play).
        includeInBundle = false
    }

    packaging {
        resources.excludes += "DebugProbesKt.bin"
    }

    androidResources {
        generateLocaleConfig = true
    }

    lint {
        lintConfig = file("src/lint.xml")
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(project(":app"))

    implementation(libs.coil.compose)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
