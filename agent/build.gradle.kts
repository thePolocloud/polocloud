plugins {
    kotlin("jvm") version "2.1.20"
    id("com.gradleup.shadow") version "9.0.0-beta13"
}

group = "dev.httpmarco.polocloud"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(libs.bundles.proto)
    implementation(libs.grpc.netty)
    implementation(project(":proto"))

    implementation(libs.bundles.terminal)
    implementation(libs.bundles.runtime)
    implementation(libs.gson)
    implementation(libs.jline)
}

tasks.shadowJar {
    archiveFileName.set("polocloud-agent-$version.jar")
    manifest {
        attributes("Main-Class" to "dev.httpmarco.polocloud.agent.AgentBootKt");
        attributes("Premain-Class" to "dev.httpmarco.polocloud.agent.AgentBootKt")
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}