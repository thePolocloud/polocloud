pluginManagement {
    includeBuild("gradle-plugin")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "polocloudv3"
include("frames", "frames:frame-gradle-plugin", "frames:frame-api", "cli", "runner")
include("proto")
include("common")