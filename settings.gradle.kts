pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "polocloudv3"
include("runner")
include("cli")
include("proto")
include("common")
include("node")
include("api")
include("bridge")
include("shared")
include("updater")
include("addons")
include("addons:sign-system")
include("addons:hub-addon")
include("addons:notifiy-addon")
include("addons:proxy-addon")
include("addons:server-mobs-addon")