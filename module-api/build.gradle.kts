plugins {
    id("org.jetbrains.kotlin.jvm")

    alias(libs.plugins.polocloud.gradle.plugin)
}

dependencies {
    // exposed to module authors: the full node-facing SDK (group/service/event services)
    // plus everything api itself re-exports (proto, common, shared)
    api(projects.api)

    // PolocloudModule exposes a ready-to-use Logger — module authors shouldn't need to
    // add this themselves just to call module.logger.info(...)
    api(libs.slf4j.api)

    // ModuleConfig's YAML (de)serialization — same library the node itself uses for its
    // own config/task files, kept as `api` so module authors get it for free too.
    api(libs.snakeyaml)

    // testing
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

// matches api/build.gradle.kts — module.jar bytecode has to stay consumable by the same
// toolchain the rest of the SDK chain (bridge, api) is pinned to.
kotlin {
    jvmToolchain(25)
}
