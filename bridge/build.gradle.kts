plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.shadow)
}

repositories {
    // velocity-api lives in the PaperMC repository
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // The bridge plugin ships ONLY the polocloud api (plus its transitive runtime).
    // Everything is bundled into the fat jar so it is self-contained inside the proxy,
    // which does not have the polocloud runtime on its classpath.
    implementation(projects.api)

    // Provided by Velocity at runtime — never bundled.
    // Waterfall/BungeeCord bridge support is not available yet, so waterfall-api is intentionally omitted.
    compileOnly(libs.velocity.api)

    // testing
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
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

    // The runner identifies embedded jars by these manifest attributes (see Expender).
    manifest {
        attributes(
            "artifactId" to "bridge",
            "groupId" to project.group.toString(),
            "version" to project.version.toString(),
        )
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}