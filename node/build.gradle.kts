import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.jline)
    implementation(project(":api"))
}


tasks.named("shadowJar", ShadowJar::class) {
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
    manifest {
        attributes["Main-Class"] = "dev.httpmarco.polocloud.node.launcher.NodeLauncher"
    }
}