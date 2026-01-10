package dev.httpmarco.polocloud.dependency.plugin

import dev.httpmarco.polocloud.dependency.plugin.dependency.Dependency
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.attributes
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Gradle plugin that embeds a `dependencies.blob` file into the produced JAR.
 *
 * The blob contains runtime dependency metadata (group, artifact, version, download URL, checksum)
 * which can later be consumed by Polocloud at runtime to resolve and verify dependencies.
 */
class PolocloudDependencyPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val extension = project.extensions.create(
            "polocloud",
            PolocloudDependencyExtension::class.java
        )

        project.afterEvaluate {
            project.tasks.withType(Jar::class.java).configureEach {

                manifest {
                    extension.mainClass?.let { main ->
                        attributes("Main-Class" to main)
                    }

                    attributes(
                        "groupId" to project.group.toString(),
                        "artifactId" to project.name,
                        "version" to project.version.toString()
                    )
                }

                doFirst {
                    val blobFile = project.layout.buildDirectory
                        .file("dependencies.blob")
                        .get()
                        .asFile

                    blobFile.parentFile.mkdirs()

                    val primaryRepo = project.repositories
                        .filterIsInstance<MavenArtifactRepository>()
                        .firstOrNull()
                        ?.url
                        ?.toString()
                        ?: "https://repo.maven.apache.org/maven2"

                    val dependencies = extension.projects.mapNotNull { notation ->
                        parseDependency(notation, primaryRepo)
                    }

                    blobFile.writeText(
                        dependencies.joinToString("\n") { it.toNotation() },
                        StandardCharsets.UTF_8
                    )
                }

                from(project.layout.buildDirectory) {
                    include("dependencies.blob")
                    into("/")
                }
            }
        }
    }

    /**
     * Parses a Gradle dependency notation (`group:artifact:version`)
     * and converts it into a [Dependency] object.
     */
    private fun parseDependency(
        notation: String,
        repositoryUrl: String
    ): Dependency? {
        val parts = notation.split(":")
        if (parts.size < 3) return null

        val groupId = parts[0]
        val artifactId = parts[1]
        val version = parts[2]

        val path = groupId.replace(".", "/")
        val jarUrl =
            "${repositoryUrl.trimEnd('/')}/$path/$artifactId/$version/$artifactId-$version.jar"

        return Dependency(
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            url = jarUrl,
            checksum = fetchChecksum(jarUrl)
        )
    }
}

/**
 * Attempts to fetch a checksum for the given artifact URL.
 *
 * Prefers SHA-256 and falls back to SHA-1 if unavailable.
 *
 * @param jarUrl the base URL of the JAR artifact
 * @return the checksum value as a hexadecimal string
 * @throws IllegalStateException if no checksum could be resolved
 */
fun fetchChecksum(jarUrl: String): String {

    fun load(url: String): String = URL(url).readText().trim().split(" ")[0]

    return runCatching {
        load("$jarUrl.sha256")
    }.getOrElse {
        load("$jarUrl.sha1")
    }
}

/**
 * Adds a runtime dependency that will be embedded into the `dependencies.blob`
 * and also registers it as an `implementation` dependency.
 *
 * @param notation dependency notation (`group:artifact:version`)
 */
fun Project.polocloudRuntime(notation: Any) {
    extensions
        .getByType(PolocloudDependencyExtension::class.java)
        .projects
        .add(notation.toString())

    dependencies.add("implementation", notation)
}