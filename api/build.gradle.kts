plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.serialization")

    alias(libs.plugins.polocloud.gradle.plugin)
}

dependencies {
    api(projects.proto)
    api(projects.common)
    api(projects.shared)
    implementation(libs.bundles.tls)
    implementation(libs.kotlinx.coroutines.jdk8)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.kotlinx.coroutines.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(25)
}
