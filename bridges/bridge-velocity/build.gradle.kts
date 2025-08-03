import org.gradle.kotlin.dsl.projects

plugins {
    kotlin("kapt")
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly(libs.velocity)
    kapt(libs.velocity)

    implementation(projects.sdk.sdkKotlin)
    implementation(projects.bridges.bridgeApi)
}