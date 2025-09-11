import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.2.20"
    id("com.gradleup.shadow") version "9.1.0"
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")

    tasks.withType<ShadowJar> {
        archiveFileName.set("polocloud-${project.name}-$version.jar")
    }

    kotlin {
        jvmToolchain(21)
    }
}

