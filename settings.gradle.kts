plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "polocloudv3"
include("sdk")
include("sdk:java")
include("proto")
include("agent")
include("platforms")
include("launcher")