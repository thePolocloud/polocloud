plugins {
    kotlin("jvm") version "2.2.0"
}

group = "dev.httpmarco.polocloud"
version = "3.0.0.BETA"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}