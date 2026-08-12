plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    android {
        namespace = "com.prajwalch.torrentsearch"

        compileSdk = 37
        minSdk = 25

        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }

        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    // Non-Android JVM target so portable commonMain code can be exercised later
    // without an Android runtime.
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

        androidMain.dependencies {
            api(libs.androidx.activity.compose)
            api(libs.androidx.core.ktx)
            api(libs.androidx.core.splashscreen)
            api(libs.androidx.lifecycle.runtime.ktx)
            api(libs.androidx.material3)
            api(libs.androidx.ui)
            api(libs.androidx.ui.graphics)
            api(libs.koin.android)
            api(libs.koin.androidx.compose)
            api(platform(libs.androidx.compose.bom))

            implementation(libs.androidx.datastore.preferences)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.room.ktx)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.ui.tooling.preview)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.compose.markdown)
            implementation(libs.jsoup)
            implementation(libs.koin.annotations)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.lazycolumnscrollbar)
            implementation(libs.okhttp.dnsoverhttps)
        }

        androidDeviceTest.dependencies {
            implementation(libs.androidx.espresso.core)
            implementation(libs.androidx.junit)
            implementation(libs.androidx.ui.test.junit4)
            implementation(platform(libs.androidx.compose.bom))
        }
    }
}

// Keep existing sources under src/main/kotlin without a mass rename.
androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addStaticSourceDirectory("src/main/kotlin")
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    "androidRuntimeClasspath"(libs.androidx.ui.tooling)
    "androidRuntimeClasspath"(libs.androidx.ui.test.manifest)

    add("kspAndroid", libs.androidx.room.compiler)
}
