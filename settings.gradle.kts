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
include("sdk")
include("agent")
include("platforms")
include("launcher")
include("common")
include("bridges")
include("updater")
include("bridges:bridge-api")
include("bridges:bridge-gate")
include("bridges:bridge-velocity")
include("bridges:bridge-fabric")
include("bridges:bridge-bungeecord")
include("shared")
include("sdk:sdk-java")
include("bridges:bridge-fabric:v1_21_8")
include("bridges:bridge-fabric:v1_21_5")
include("bridges:bridge-waterdog")
include("modules")
include("modules:example")
include("modules:rest")