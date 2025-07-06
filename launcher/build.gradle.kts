plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-rc1"
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.shadowJar {
    from(includeLibs("common"), includeLibs("agent", "shadowJar"), includeLibs("platforms"))

    manifest {
        attributes("Main-Class" to "dev.httpmarco.polocloud.launcher.PolocloudLauncher")
        attributes("polocloud-version" to version)
    }

    archiveFileName.set("polocloud-launcher.jar")
}


tasks.test {
    useJUnitPlatform()
}

fun includeLibs(project: String, task: String = "jar"): Task {
    return project(":$project").tasks.getByPath(":$project:$task")
}