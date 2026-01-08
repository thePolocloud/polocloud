plugins {
    id("java")
    id("dev.httpmarco.polocloud")
}

polocloud {
    mainClass = "dev.httpmarco.polocloud.runner.PolocloudRuntimeLauncher"
}

tasks.named<Jar>("jar") {
    val cliJar = project(":cli").tasks.named<Jar>("jar").get().archiveFile.get().asFile
    from(cliJar) {
        into(".cache")
    }
}