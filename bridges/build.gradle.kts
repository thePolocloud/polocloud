import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.2.0"
    id("com.gradleup.shadow") version "9.0.0-rc3"
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

