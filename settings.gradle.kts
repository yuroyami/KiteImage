enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "KiteImage-KMP"

// :kiteimage — the pure-Kotlin image codec core (stdlib only). Format sniffing,
// decoders (BMP, PNG, …), the ARGB pixel-buffer type, and the malformed-input
// guards all live here and port cleanly to every KMP target.
include(":kiteimage")
