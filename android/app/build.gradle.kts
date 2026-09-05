import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Release signing credentials. key.properties is gitignored and only exists on
// the maintainer's machine — a fresh clone or CI without secrets falls back to
// the debug keystore so builds still succeed.
val keystoreProperties = Properties().apply {
    val f = rootProject.file("key.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hasReleaseKey = keystoreProperties.isNotEmpty()

android {
    namespace = "com.wallkraft.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wallkraft.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 25
        versionName = "1.17.1"
        resourceConfigurations += setOf("en")
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKey) {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Real release key when key.properties exists; debug keystore
            // otherwise (local dev only).
            signingConfig = if (hasReleaseKey) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
        jniLibs {
            excludes += setOf(
                "lib/armeabi-v7a/*",
                "lib/x86/*",
                "lib/x86_64/*",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Fail CI builds that try to produce a release APK without a signing key.
// This runs at execution time (not configuration time) so `test` and
// `assembleDebug` are unaffected.
gradle.taskGraph.whenReady {
    if (System.getenv("CI") != null && !hasReleaseKey && hasTask(":app:assembleRelease")) {
        error("Release signing key not found in CI. Check GitHub Secrets.")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.navigation.compose)
    implementation(libs.splashscreen)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.serialization.json)

    // Images
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Persistence
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore)
    implementation(libs.security.crypto)

    // Coroutines
    implementation(libs.coroutines.android)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.test.ext)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
