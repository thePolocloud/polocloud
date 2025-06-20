plugins {
    kotlin("jvm") version "2.1.20"
    id("com.gradleup.shadow") version "9.0.0-beta13"
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.shadowJar {
    from(includeLibs("agent", "shadowJar"))

    manifest {
        attributes("Main-Class" to "dev.httpmarco.polocloud.launcher.PolocloudLauncherKt")
        attributes("polocloud-version" to version)
    }

    archiveFileName.set("polocloud-launcher.jar")
}


tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}

fun includeLibs(project: String, task: String = "jar"): Task {
    return project(":$project").tasks.getByPath(":$project:$task")
}