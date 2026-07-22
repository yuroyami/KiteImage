import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.vanniktech.publish)
    alias(libs.plugins.dokka)
}

/*
 * :kiteimage is the pure-Kotlin image codec core. NO external runtime deps — only
 * kotlin-stdlib is on the classpath, exactly like :kitearchive, :kitepdf and
 * :kitetorrent. Everything here is pure computation (no sockets, no threads, no
 * disk): format sniffing, the decoders, the ARGB pixel buffer, and the
 * malformed-input / decompression-bomb guards.
 *
 * The Compose interop (ImageBitmap conversion, animated playback) will live in a
 * separate :kiteimage-compose module so this core stays portable to every target
 * and Compose never reaches consumers who don't want it.
 */
kotlin {
    jvmToolchain(21)

    android {
        namespace = "io.github.yuroyami.kiteimage"
        compileSdk = 36
        minSdk = 21
    }

    listOf(
        iosSimulatorArm64(),
        iosArm64(),
        iosX64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "KiteImage"
            isStatic = false
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    js(IR) {
        browser()
        nodejs()
        binaries.library()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    jvm()

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlin.experimental.ExperimentalNativeApi")
            }
        }

        commonMain.dependencies {
            // Intentionally empty. KiteImage core depends on kotlin-stdlib only.
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
