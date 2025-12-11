plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("kapt") version "2.2.21"
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.polocloud.proto)
    implementation(libs.polocloud.shared)

    implementation(libs.bundles.terminal)
    kapt(libs.bundles.terminal)

    implementation(libs.bundles.runtime)
    implementation(libs.bundles.jline)

    implementation(libs.gson)
    implementation(libs.oshi)

    implementation(libs.bundles.confirationPool)
    implementation(projects.platforms)
    implementation(projects.common)
    implementation(projects.updater)

    // todo versions -> toml
    implementation("io.grpc:grpc-netty:1.77.0")
}

tasks.jar {
    archiveFileName.set("polocloud-agent-$version.jar")
    manifest {
        attributes("Main-Class" to "dev.httpmarco.polocloud.agent.AgentBootKt")
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}