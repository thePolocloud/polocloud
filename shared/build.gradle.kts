plugins {
    kotlin("jvm") version "2.2.0"
}

dependencies {
    api(projects.proto)
    implementation(projects.proto)
}

kotlin {
    jvmToolchain(21)
}