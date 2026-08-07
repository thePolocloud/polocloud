plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.serialization")

    alias(libs.plugins.polocloud.gradle.plugin)
}

dependencies {
    // exposed to SDK consumers: proto stubs/messages, common (Address, mTLS, channel factory),
    // shared (cluster event contracts + codec)
    api(projects.proto)
    api(projects.common)
    api(projects.shared)

    // BouncyCastle is required at runtime by the inherited CertificateStorage
    implementation(libs.bundles.tls)

    // testing
    testImplementation(libs.bundles.testing)
    testImplementation(libs.kotlinx.coroutines.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

// bridge/build.gradle.kts pins its own toolchain to 25 specifically to match this
// module's bytecode ("Match the polocloud api toolchain so the bundled bytecode stays
// consumable") — but without this module pinning the same version explicitly, it instead
// compiled at whatever JDK happened to invoke Gradle, silently breaking that contract
// (and the build) on any machine/CI whose default JDK isn't 25.
kotlin {
    jvmToolchain(25)
}
