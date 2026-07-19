import de.polocloud.gradle.plugin.polocloudRuntime

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.polocloud.gradle.plugin)
}

dependencies {
    compileOnly(projects.common)
    compileOnly(libs.slf4j.api)

    polocloudRuntime(libs.kotlinx.serialization.json)

    testImplementation(libs.bundles.testing)
    testImplementation(projects.common)
    testImplementation(libs.slf4j.api)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}