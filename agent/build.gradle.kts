plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("kapt") version "2.3.0"
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
    implementation("io.grpc:grpc-netty:1.78.0")
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


publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.named("jar")) {
                classifier = null
            }

            pom {
                description.set("PoloCloud gRPC API with bundled dependencies")
                url.set("https://github.com/thePolocloud/polocloud")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        name.set("Mirco Lindenau")
                        email.set("mirco.lindenau@gmx.de")
                    }
                }
                scm {
                    url.set("https://github.com/thePolocloud/polocloud")
                    connection.set("scm:git:https://github.com/thePolocloud/polocloud.git")
                    developerConnection.set("scm:git:https://github.com/thePolocloud/polocloud.git")
                }
            }
        }
    }
}
