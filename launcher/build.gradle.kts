import java.security.MessageDigest
import org.w3c.dom.Document
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    id("java")
}

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_21.toString()
    targetCompatibility = JavaVersion.VERSION_21.toString()
    options.encoding = "UTF-8"
}

// Configure the shadowJar task (used to create a fat jar with dependencies)
tasks.jar {
    dependsOn("buildDependencies")

    from(
        includeLibs("common"),
        includeLibs("agent"),
        includeLibs("platforms"),
        includeLibs("updater"),
    )

    manifest {
        attributes(
            "Main-Class" to "dev.httpmarco.polocloud.launcher.PolocloudLauncher",
            "POLOCLOUD_VERSION" to version
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
tasks.register("buildDependencies") {
    group = "build"
    description =
        "Exports runtime dependencies of :agent as a semicolon-separated file with Maven Central URLs and SHA-256 checksums."

    evaluationDependsOn(":agent")

    doLast {
        val agentProject = project(":agent")
        val mavenCentralBase = "https://repo1.maven.org/maven2"
        val mavenCentralSnapshot = "https://central.sonatype.com/repository/maven-snapshots"
        val runtimeClasspath = agentProject.configurations.getByName("runtimeClasspath")

        val outputFile = file("src/main/resources/dependencies.blob")
        outputFile.printWriter().use { writer ->
            if (runtimeClasspath.isCanBeResolved) {
                runtimeClasspath.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
                    val group = artifact.moduleVersion.id.group
                    val name = artifact.name
                    val version = artifact.moduleVersion.id.version
                    val file = artifact.file
                    val fileName = file.name
                    val groupPath = group.replace(".", "/")

                    var url = "$mavenCentralBase/$groupPath/$name/$version/$fileName"

                    if (group == "dev.httpmarco.polocloud") {
                        if (name == "proto" || name == "shared") {
                            val fileUrl = getLatestSnapshotFile(
                                baseUrl = mavenCentralSnapshot,
                                group = group,
                                name = name,
                                version = version,
                                extension = "jar"
                            )

                            url = fileUrl
                        } else {
                            return@forEach
                        }
                    }


                    val sha256 = file.inputStream().use { input ->
                        val digest = MessageDigest.getInstance("SHA-256")
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            digest.update(buffer, 0, bytesRead)
                        }
                        digest.digest().joinToString("") { "%02x".format(it) }
                    }

                    writer.println("$group;$name;$version;$fileName;$url;$sha256")
                }
            }
        }
        println("✅ Exported agent dependencies to ${outputFile.absolutePath}")
    }
}

fun getLatestSnapshotFile(
    baseUrl: String,
    group: String,
    name: String,
    version: String,
    extension: String
): String {
    val groupPath = group.replace(".", "/")
    val metadataUrl = "$baseUrl/$groupPath/$name/$version/maven-metadata.xml"

    // maven-metadata.xml laden
    val xml: Document = DocumentBuilderFactory
        .newInstance()
        .newDocumentBuilder()
        .parse(URL(metadataUrl).openStream())

    xml.documentElement.normalize()

    // Die neueste Snapshot-Version extrahieren
    val snapshotVersions = xml.getElementsByTagName("snapshotVersion")
    var latestValue: String? = null
    var latestUpdated: Long = 0

    for (i in 0 until snapshotVersions.length) {
        val node = snapshotVersions.item(i)
        val child = node.childNodes

        var value: String? = null
        var updated: Long = 0

        for (j in 0 until child.length) {
            val c = child.item(j)
            when (c.nodeName) {
                "value" -> value = c.textContent
                "updated" -> updated = c.textContent.toLong()
            }
        }

        if (value != null && updated > latestUpdated) {
            latestUpdated = updated
            latestValue = value
        }
    }

    require(latestValue != null) {
        "Konnte keine Snapshot-Version in $metadataUrl finden"
    }

    val fileName = "$name-$latestValue.$extension"
    return "$baseUrl/$groupPath/$name/$version/$fileName"
}


tasks.register<Exec>("dockerBuild") {
    dependsOn(tasks.jar)
    val imageName = "polocloud:development"

    // Docker build
    commandLine(
        "docker", "build",
        "--build-arg", "POLOCLOUD_VERSION=$version",
        "-t", imageName,
        "-f", "../docker/Dockerfile",
        "."
    )
}



