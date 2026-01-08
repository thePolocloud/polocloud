plugins {
    id("java")
    id("dev.httpmarco.polocloud")
}

polocloud {
    mainClass = "dev.httpmarco.polocloud.runner.PolocloudRuntimeLauncher"
}

tasks.jar {
    from(project(":cli").tasks.getByPath(":cli:jar"))
}