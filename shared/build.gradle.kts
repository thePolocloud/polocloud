plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.serialization")

    alias(libs.plugins.polocloud.gradle.plugin)
}

dependencies {
    // Event payloads are @Serializable and (de)serialized via the shared EventCodec.
    // Exposed so consumers (api, node) get the serialization runtime transitively.
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

// See common/build.gradle.kts — same reasoning: this module is one of api's
// dependencies, so it must match api's pinned toolchain (25) too.
kotlin {
    jvmToolchain(25)
}
