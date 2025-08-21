import org.gradle.kotlin.dsl.projects

plugins {
    id("com.gradleup.shadow") version "9.0.2"
}


repositories {
    maven {
        name = "waterdog-repo"
        url = uri("https://repo.waterdog.dev/main")
    }
    maven {
        name = "opencollab-snapshots"
        url = uri("https://repo.opencollab.dev/maven-snapshots/")
    }
    maven {
        name = "opencollab-releases"
        url = uri("https://repo.opencollab.dev/maven-releases/")
    }
}

dependencies {
    compileOnly(libs.waterdog)
    implementation(projects.sdk.sdkJava)
    implementation(projects.bridges.bridgeApi)
}

tasks.processResources {
    filesMatching(listOf("plugin.yml")) {
        expand("version" to version)
    }
}