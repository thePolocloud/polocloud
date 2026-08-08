plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.shadow)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(projects.api)
    compileOnly(libs.velocity.api)
    compileOnly(libs.waterfall.api)
    annotationProcessor(libs.velocity.api)
}

tasks.processResources {
    val tokens = mapOf("version" to project.version.toString())
    inputs.properties(tokens)
    filesMatching(listOf("velocity-plugin.json", "bungee.yml")) {
        expand(tokens)
    }
}

tasks.shadowJar {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

java {
    toolchain {
        // Match the polocloud api toolchain so the bundled bytecode stays consumable.
        // The resulting plugin therefore requires a matching JRE inside the proxy.
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    jvmToolchain(25)
}