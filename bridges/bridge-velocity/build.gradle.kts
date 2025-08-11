import org.gradle.kotlin.dsl.projects
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow") version "9.0.1"
}


repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly(libs.velocity)

    implementation(libs.bstats.velocity)
    implementation(projects.sdk.sdkJava)
    implementation(projects.bridges.bridgeApi)
}


tasks.withType<ShadowJar> {
    relocate("org.bstats", "dev.httpmarco.polocloud.libs.bstats")
}

tasks.processResources {
    filesMatching(listOf("velocity-plugin.json")) {
        expand("version" to version)
    }
}