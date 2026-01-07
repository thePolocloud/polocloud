pluginManagement {
    //includeBuild("dependency-platform")
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "polocloudv3"
include("launcher")
include("frames")
include("frames:frame-gradle-plugin")
include("frames:frame-api")
include("cli")
include("dependencies")
include("dependencies:platform")
include("dependencies:plugin")