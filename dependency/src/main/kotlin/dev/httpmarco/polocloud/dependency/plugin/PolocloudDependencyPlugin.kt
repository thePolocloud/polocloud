package dev.httpmarco.polocloud.dependency.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.tasks.bundling.Jar

class PolocloudDependencyPlugin : Plugin<Project> {

    companion object {
        private val runtimeDependencies = mutableListOf<String>()

        fun addDependency(notation: String) {
            runtimeDependencies.add(notation)
        }
    }

    override fun apply(project: Project) {
        project.afterEvaluate {
            project.tasks.withType(Jar::class.java).all {
                val txtFile = project.layout.buildDirectory.file("dependencies.blob").get().asFile
                txtFile.parentFile.mkdirs()

                val dependencyUrls = mutableListOf<String>()

                val primaryRepo = project.repositories.firstOrNull {
                    it is MavenArtifactRepository
                }?.let { it as MavenArtifactRepository }?.url?.toString()
                    ?: "https://repo.maven.apache.org/maven2"

                runtimeDependencies.forEach { notation ->
                    val parts = notation.split(":")
                    if (parts.size >= 3) {
                        val group = parts[0].replace(".", "/")
                        val name = parts[1]
                        val version = parts[2]

                        val url = "${primaryRepo.trimEnd('/')}/$group/$name/$version/$name-$version.jar"
                        dependencyUrls.add(url)
                    }
                }

                val content = dependencyUrls.joinToString("\n")
                txtFile.writeText(content)

                from(txtFile.parentFile) {
                    include(txtFile.name)
                    into("/")
                }
            }
        }
    }
}

fun DependencyHandler.polocloudRuntime(notation: Any) {
    val notationString = notation.toString()
    PolocloudDependencyPlugin.addDependency(notationString)
    add("implementation", notation)
}