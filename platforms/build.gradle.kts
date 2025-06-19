plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "1.9.0"
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.gson)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}