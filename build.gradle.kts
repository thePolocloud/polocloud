plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.maven.publish) apply false
}

allprojects {
    apply(from = rootProject.file("gradle/version.gradle.kts"))

    group = "de.polocloud"

    repositories {
        maven {
            url = uri("https://repo.maven.apache.org/maven2")
        }

        maven {
            name = "polocloud-snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots")
        }
    }

    plugins.withId("de.polocloud.gradle.plugin") {
        tasks.named("generateDependencyIndex").configure {
            doLast {
                dedupeDependencyIndex(outputs.files.singleFile)
            }
        }
    }
}

val publishedModules = setOf("api", "proto", "common", "shared")

configure(subprojects.filter { it.name in publishedModules }) {
    apply(plugin = "com.vanniktech.maven.publish")

    extensions.configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
        publishToMavenCentral()
        if (providers.gradleProperty("signingInMemoryKey").isPresent) {
            signAllPublications()
        }

        coordinates("de.polocloud", "polocloud-${project.name}", project.version.toString())

        pom {
            name.set("PoloCloud ${project.name}")
            description.set("PoloCloud ${project.name} module")
            url.set("https://github.com/thePolocloud/polocloud")
            inceptionYear.set("2024")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("httpmarco")
                    name.set("Mirco Lindenau")
                    url.set("https://github.com/httpmarco")
                }
            }
            scm {
                url.set("https://github.com/thePolocloud/polocloud")
                connection.set("scm:git:git://github.com/thePolocloud/polocloud.git")
                developerConnection.set("scm:git:ssh://git@github.com/thePolocloud/polocloud.git")
            }
        }
    }
}

/** Rewrites [file] (`group;artifact;version;url;checksum` lines) keeping the highest version per group:artifact. */
fun dedupeDependencyIndex(file: java.io.File) {
    if (!file.exists()) return

    val bestByCoordinate = LinkedHashMap<String, List<String>>()
    file.readLines()
        .filter { it.isNotBlank() }
        .forEach { line ->
            val parts = line.split(";")
            val coordinate = "${parts[0]};${parts[1]}"
            val existing = bestByCoordinate[coordinate]
            if (existing == null || compareVersions(parts[2], existing[2]) > 0) {
                bestByCoordinate[coordinate] = parts
            }
        }

    file.writeText(bestByCoordinate.values.joinToString("\n") { it.joinToString(";") } + "\n")
}

/** Compares dotted/hyphenated version strings numerically segment by segment, falling back to lexical comparison for non-numeric segments. */
fun compareVersions(a: String, b: String): Int {
    val partsA = a.split(Regex("[.\\-+]"))
    val partsB = b.split(Regex("[.\\-+]"))
    val length = maxOf(partsA.size, partsB.size)

    for (i in 0 until length) {
        val segmentA = partsA.getOrNull(i) ?: ""
        val segmentB = partsB.getOrNull(i) ?: ""
        val numericA = segmentA.toLongOrNull()
        val numericB = segmentB.toLongOrNull()

        val comparison = if (numericA != null && numericB != null) {
            numericA.compareTo(numericB)
        } else {
            segmentA.compareTo(segmentB)
        }
        if (comparison != 0) return comparison
    }
    return 0
}


tasks.register("allTests") {
    group = "verification"
    description = "Runs the test task of every module (project-wide test suite)."
    dependsOn(subprojects.map { "${it.path}:test" })
}