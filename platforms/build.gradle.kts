plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.2.0"
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