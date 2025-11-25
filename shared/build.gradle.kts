plugins {
    kotlin("jvm") version "2.2.21"
}

dependencies {
    compileOnly(libs.polocloud.proto)
    compileOnly(libs.gson)
}

kotlin {
    jvmToolchain(21)
}