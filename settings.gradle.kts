plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "polocloudv3"
include("sdk")
include("sdk:sdk-kotlin")
include("proto")
include("agent")
include("platforms")
include("launcher")
include("common")
include("bridges")
include("updater")
include("bridges:bridge-api")
include("bridges:bridge-gate")
include("bridges:bridge-velocity")
include("bridges:bridge-bungeecord")
include("shared")
include("addons")
include("addons:signs")