@file:Suppress("DEPRECATION", "DEPRECATION_ERROR")

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    androidTarget()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "wird.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)

            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.navigation.compose)

            implementation(libs.kotlinx.serialization.json)

            implementation(libs.coroutines.core)
            implementation(libs.kotlinx.datetime)

            implementation(libs.room.runtime)
            implementation(libs.adhan)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.coroutines.test)
        }
        // Database tests run on the host JVM, where the Android artifact's .so files are
        // useless — BundledSQLiteDriver needs the JVM variant's desktop natives instead.
        androidUnitTest.dependencies {
            implementation(libs.sqlite.bundled.jvm)
        }
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling)
            implementation(libs.androidx.activity.compose)
            implementation(libs.sqlite.bundled)
        }
        wasmJsMain.dependencies {
            implementation(libs.sqlite.web)
        }
    }
}


room3 {
    schemaDirectory(layout.projectDirectory.dir("schemas").asFile.invariantSeparatorsPath)
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspWasmJs", libs.room.compiler)
}

android {
    namespace = "dev.ahmad.wird"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.ahmad.wird"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
}

// ---------------------------------------------------------------------------
// Layering enforcement.
//
// domain/ is the innermost layer: it may see the Kotlin stdlib, kotlinx-datetime,
// kotlinx-coroutines, and itself. Nothing else. The detector lives in buildSrc so it
// can be unit tested; this only configures it.
//
// It runs before every Kotlin compilation on both targets, so `assembleDebug` and
// `wasmJsBrowserDistribution` both fail on a violation.
// ---------------------------------------------------------------------------

val checkDomainPurity = tasks.register<dev.ahmad.wird.gradle.CheckDomainPurityTask>("checkDomainPurity") {
    group = "verification"
    description = "Fails the build if domain/ depends on anything outside the Kotlin stdlib, kotlinx-datetime, kotlinx-coroutines or domain/ itself."
    domainSources.from(
        layout.projectDirectory.dir("src/commonMain/kotlin/dev/ahmad/wird/domain").asFileTree,
    )
    allowedImportPrefixes.set(
        listOf(
            "kotlin.",
            "kotlinx.datetime.",
            "kotlinx.coroutines.",
            "dev.ahmad.wird.domain.",
        ),
    )
    report.set(layout.buildDirectory.file("reports/layering/domain-purity.txt"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(checkDomainPurity)
}
tasks.named("check") { dependsOn(checkDomainPurity) }
