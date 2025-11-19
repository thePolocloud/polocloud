plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.0.0"
}

dependencies {
    api(projects.shared)
    api(projects.common)
    api(libs.grpc.netty)
    api(projects.proto)

    compileOnly(libs.gson)
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_21.toString()
    targetCompatibility = JavaVersion.VERSION_21.toString()
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveFileName = "sdk-java-3.0.0-pre.6.1-SNAPSHOT.jar"

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    relocate("com.google.protobuf", "dev.httpmarco.polocloud.sdk.java.relocated.protobuf")
}

tasks.jar {
    dependsOn(tasks.shadowJar)

    // Jar selbst soll nichts erzeugen
    enabled = false
}

artifacts {
    archives(tasks.shadowJar)
}

extensions.configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.shadowJar.get()) {
                classifier = null
            }

            pom {
                name.set(project.name)
                url.set("https://github.com/httpmarco/polocloud")
                description.set("PoloCloud is the simplest and easiest Cloud for Minecraft")
                licenses {
                    license {
                        name.set("Apache License")
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
                    url.set("https://github.com/httpmarco/polocloud")
                    connection.set("https://github.com/httpmarco/polocloud.git")
                }
            }
        }
    }
}
