import de.polocloud.gradle.plugin.polocloudRuntime

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.serialization")

    alias(libs.plugins.polocloud.gradle.plugin)
}

polocloud {
    mainClass = "de.polocloud.node.bootstrap.NodeLaunchBootstrapKt"
}

dependencies {
    //kotlin
    polocloudRuntime(libs.kotlinx.serialization.json)
    polocloudRuntime(libs.kotlinx.coroutines.core)
    polocloudRuntime(libs.kotlin.reflect)

    //logging
    kapt(libs.log4j.core)
    polocloudRuntime(libs.bundles.logging.full)

    // grpc
    polocloudRuntime(libs.bundles.grpc.node)

    //polocloud
    polocloudRuntime(libs.polocloud.i18n)

    //hashing
    polocloudRuntime(libs.argon2)

    //configuration
    polocloudRuntime(libs.snakeyaml)
    polocloudRuntime(libs.toml4j)

    //security
    polocloudRuntime(libs.bundles.tls)

    //system
    polocloudRuntime(libs.oshi)

    //database
    polocloudRuntime(libs.polocloud.database)

    // cli
    polocloudRuntime(libs.jline)

    compileOnly(projects.common)
    implementation(projects.proto)
    implementation(projects.shared)
    implementation(projects.updater)

    // testing
    testImplementation(projects.common)
    testImplementation(libs.bundles.testing)
    testImplementation(libs.kotlinx.coroutines.core)
    // The terminal commands reference the i18n helpers; needed on the test classpath
    // so command classes load when exercised in tests.
    testImplementation(libs.polocloud.i18n)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    systemProperty("PID", ProcessHandle.current().pid().toString())
    useJUnitPlatform()
}