plugins {
    id("java")
    id("dev.httpmarco.polocloud")
}

polocloud {
    mainClass = "dev.httpmarco.polocloud.runner.PolocloudRuntimeLauncher"
}

// TODO use gradle plugin here
tasks.named<Jar>("jar") {
    dependsOn(":cli:jar")

    val cliJar = project(":cli").tasks.named<Jar>("jar").get().archiveFile.get().asFile

    from(cliJar) {
        into(".cache")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}
