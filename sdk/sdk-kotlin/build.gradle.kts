plugins {
    kotlin("jvm") version "2.2.0"
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.bundles.proto)
    implementation(libs.grpc.netty)
    compileOnly(libs.gson)

    api(project(":proto"))
    implementation(project(":proto"))

    api(projects.shared)
    implementation(projects.shared)

    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
}

extensions.configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["kotlin"])

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

sourceSets["main"].java.srcDirs("../../proto/build/generated/sources/proto")

tasks.named("compileJava") {
    dependsOn(":proto:classes")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveFileName.set("polocloud-java-sdk-$version.jar")
}

kotlin {
    jvmToolchain(21)
}