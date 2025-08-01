import groovy.json.JsonOutput
import java.io.File
import java.security.MessageDigest

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-rc3"
}

dependencies {
    implementation(libs.gson)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_21.toString()
    targetCompatibility = JavaVersion.VERSION_21.toString()
    options.encoding = "UTF-8"
}

// Configure the shadowJar task (used to create a fat jar with dependencies)
tasks.shadowJar {
    from(
        includeLibs("common"),
        includeLibs("agent"),
        includeLibs("platforms"),
        includeLibs("proto"),

        includeLibs(":bridges:bridge-velocity", "shadowJar"),
        includeLibs(":bridges:bridge-bungeecord", "shadowJar")
    )

    manifest {
        attributes(
            "Main-Class" to "dev.httpmarco.polocloud.launcher.PolocloudLauncher",
            "polocloud-version" to version
        )
    }
    archiveFileName.set("polocloud-launcher.jar")
}

/**
 * Returns the output task of a specific subproject and task name.
 * By default, it retrieves the 'jar' task.
 */
fun includeLibs(project: String, task: String = "jar"): Task {
    return project(":$project").tasks.getByPath(":$project:$task")
}

/**
 * Exports all runtime dependencies from the :agent project into a JSON file,
 * including guessed Maven Central URLs.
 */

tasks.register("exportAgentDependenciesWithUrls") {
    group = "custom"
    description =
        "Exports only runtime dependencies of the :agent project as JSON with guessed Maven Central URLs and SHA-256 checksums."

    // Ensure the agent project is evaluated
    evaluationDependsOn(":agent")

    doLast {
        val agentProject = project(":agent")
        val dependenciesList = mutableListOf<Map<String, String>>()

        val mavenCentralBase = "https://repo1.maven.org/maven2"

        val runtimeClasspath = agentProject.configurations.getByName("runtimeClasspath")

        if (runtimeClasspath.isCanBeResolved) {
            runtimeClasspath.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
                val group = artifact.moduleVersion.id.group
                val name = artifact.name
                val version = artifact.moduleVersion.id.version
                val file = artifact.file
                val fileName = file.name
                val groupPath = group.replace(".", "/")
                val url = "$mavenCentralBase/$groupPath/$name/$version/$fileName"

                if (group == "dev.httpmarco.polocloud") return@forEach

                val sha256 = file.inputStream().use { input ->
                    val digest = MessageDigest.getInstance("SHA-256")
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                    digest.digest().joinToString("") { "%02x".format( it) }
                }

                dependenciesList.add(
                    mapOf(
                        "group" to group,
                        "name" to name,
                        "version" to version,
                        "file" to fileName,
                        "url" to url,
                        "sha256" to sha256
                    )
                )
            }
        }

        val json = JsonOutput.prettyPrint(JsonOutput.toJson(dependenciesList))
        val outputFile = File("${projectDir}/src/main/resources", "dependencies.json")
        outputFile.writeText(json)

        println("✅ Exported agent dependencies with SHA-256 to ${outputFile.absolutePath}")
    }
}
