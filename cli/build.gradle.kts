import dev.httpmarco.polocloud.dependency.plugin.polocloudRuntime

plugins {
    kotlin("jvm") version "2.3.0"
    id("dev.httpmarco.polocloud")
}

dependencies {
    polocloudRuntime("com.google.code.gson:gson:2.10.1")
}

polocloud {
    mainClass = "dev.httpmarco.polocloud.cli.PolocloudCliKt"
}
