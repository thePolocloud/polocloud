plugins {
    java
    alias(libs.plugins.polocloud.gradle.plugin)
}

polocloud {
    mainClass = "de.polocloud.runner.PolocloudRuntimeLauncher"
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Enable-Native-Access" to "ALL-UNNAMED",

            "kotlin-version"  to libs.versions.kotlin.jvm.get(),
            "log4j-version"   to libs.versions.log4j.get(),
            "slf4j-version"   to libs.versions.slf4j.get(),
        )
    }

    val subprojects = listOf(
        "common",
        "cli",
        "node",
        "proto",
        "shared",
        "updater",
    )

    dependsOn(subprojects.map { ":$it:jar" })

    subprojects.forEach { path ->
        val jarTask = project(":$path").tasks.named<Jar>("jar")

        from(jarTask.flatMap { it.archiveFile }) {
            into(".cache/dependencies")
        }
    }

    // The bridge ships as a self-contained fat jar (shadowJar). It is embedded here so the
    // node can drop it into a proxy's plugins/ folder on start (see FactoryService).
    val bridgeShadowJar = project(":bridge").tasks.named("shadowJar")
    dependsOn(bridgeShadowJar)
    from(bridgeShadowJar.map { (it as Jar).archiveFile }) {
        into(".cache/dependencies")
    }
}


java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    options.encoding = "UTF-8"
}