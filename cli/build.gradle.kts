import dev.httpmarco.polocloud.dependency.plugin.polocloudRuntime

plugins {
    kotlin("jvm") version "2.3.0"
    id("dev.httpmarco.polocloud")
}

dependencies {
    polocloudRuntime(libs.jline)

    compileOnly(projects.common)
}

polocloud {
    mainClass = "dev.httpmarco.polocloud.cli.PolocloudCliKt"
}
