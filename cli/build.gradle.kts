import dev.httpmarco.polocloud.dependency.plugin.polocloudRuntime

plugins {
    kotlin("jvm") version "2.3.0"
    id("dev.httpmarco.polocloud.dependencies")
}

dependencies {
    polocloudRuntime("com.google.code.gson:gson:2.10.1")
}
