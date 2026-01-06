pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "polocloudv3"
include("launcher")
include("runtime")
include("runtime:k8s-runtime")
include("runtime:docker-runtime")
include("runtime:local-runtime")
include("frames")
include("frames:frame-gradle-plugin")
include("frames:frame-api")