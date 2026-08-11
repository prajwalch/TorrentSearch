plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.prajwalch.torrentsearch.shared"

        compileSdk = 37
        minSdk = 25
    }

    // Non-Android JVM target (e.g. desktop) so that portable code can be
    // exercised/tested without requiring an Android runtime.
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
