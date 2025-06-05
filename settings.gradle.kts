plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "polocloudv3"
include("sdk")
include("sdk:java")
include("proto")
include("agent")
include("platforms")