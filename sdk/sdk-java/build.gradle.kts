plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.0.1"
}

dependencies {
    api(projects.shared)
    implementation(projects.common)

    compileOnly(libs.gson)
    implementation(libs.bundles.proto)
    implementation(libs.grpc.netty)
    api(projects.proto)
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_21.toString()
    targetCompatibility = JavaVersion.VERSION_21.toString()
    options.encoding = "UTF-8"
}

tasks.jar {
    dependsOn(tasks.shadowJar)
}

extensions.configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

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