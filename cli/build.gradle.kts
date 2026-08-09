import de.polocloud.gradle.plugin.polocloudRuntime

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")

    alias(libs.plugins.polocloud.gradle.plugin)
}

polocloud {
    mainClass = "de.polocloud.cli.CliBootKt"
}

dependencies {
    polocloudRuntime(libs.jline)
    polocloudRuntime(libs.slf4j.api)
    polocloudRuntime(libs.log4j.api)
    polocloudRuntime(libs.log4j.core)
    polocloudRuntime(libs.log4j.slf4j)

    polocloudRuntime(libs.polocloud.i18n)
    polocloudRuntime(libs.kotlinx.serialization.json)
    polocloudRuntime(libs.kotlinx.coroutines.core)
    polocloudRuntime(libs.kotlin.reflect)
    polocloudRuntime(libs.bcprov)
    polocloudRuntime(libs.bcpkix)
    polocloudRuntime(libs.bctls)

    polocloudRuntime(libs.grpc.api)
    polocloudRuntime(libs.grpc.stub)
    polocloudRuntime(libs.grpc.kotlin.stub)
    polocloudRuntime(libs.grpc.services)
    polocloudRuntime(libs.protobuf.kotlin)
    polocloudRuntime(libs.grpc.netty.shaded)

    compileOnly(projects.common)
    implementation(projects.proto)

    // testing — common is compileOnly for main, so it must be re-declared for tests
    // that exercise CLI types depending on shared Address/serialization helpers.
    testImplementation(projects.common)
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}