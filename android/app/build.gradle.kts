import java.util.Properties

plugins {
    id("jacoco")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        ktlint("1.5.0")
    }
    kotlinGradle {
        ktlint()
    }
}

// Release signing credentials. key.properties is gitignored and only exists on
// the maintainer's machine — a fresh clone or CI without secrets falls back to
// the debug keystore so builds still succeed.
val keystoreProperties =
    Properties().apply {
        val f = rootProject.file("key.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
val hasReleaseKey = keystoreProperties.isNotEmpty()

android {
    namespace = "com.wallkraft.app"
    compileSdk = 36

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    defaultConfig {
        applicationId = "com.wallkraft.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 57
        versionName = "3.1.0"
        resourceConfigurations += setOf("en", "es", "hi", "ja", "pt")
        // Required so on-device tests run under AndroidJUnitRunner (without
        // this the legacy InstrumentationTestRunner crashes the test process).
        testInstrumentationRunner = "dagger.hilt.android.testing.HiltTestRunner"
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
                "proguard-rules.pro",
            )
            // Real release key when key.properties exists; debug keystore
            // otherwise (local dev only).
            signingConfig =
                if (hasReleaseKey) {
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
            excludes +=
                setOf(
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

    // Room schema exports, so migration tests can validate upgrades.
    sourceSets {
        getByName("androidTest").assets.srcDirs(files("$projectDir/schemas"))
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter =
        listOf(
            "**/R.class",
            "**/R\$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "**/di/*",
            "**/*_Factory.*",
            "**/*_MembersInjector.*",
        )

    val debugTree =
        fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        }

    sourceDirectories.setFrom(files("src/main/java"))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree("${layout.buildDirectory.get()}/jacoco/testDebugUnitTest.exec"))
}

tasks.register("coverageSummary") {
    dependsOn("jacocoTestReport")
    doLast {
        val report = file("${layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/html/index.html")
        println("Coverage report: ${report.absolutePath}")
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

    // Background work (wallpaper rotation schedule)
    implementation(libs.work.runtime.ktx)

    // Palette
    implementation(libs.palette.ktx)

    // Widget (Glance)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockwebserver)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.test.ext)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.uiautomator)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.compose.ui.test.manifest)
}
