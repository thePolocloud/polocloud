plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    apply(from = rootProject.file("gradle/version.gradle.kts"))

    group = "de.polocloud"
    // version is now set by gradle/version.gradle.kts — do NOT set it here

    repositories {
        // NOTE: intentionally not using mavenCentral()/trailing-slash URLs here.
        // The polocloud-gradle-plugin's MavenResolver builds artifact URLs as
        // "$repo/$groupPath/...", so a trailing slash on the repo URL produces a
        // double slash that 404s against Maven Central's CDN, silently marking
        // every polocloudRuntime dependency as unresolvable ("unknown") in
        // dependencies.index — which is skipped at runtime, causing
        // NoClassDefFoundError for anything not in the hardcoded bootstrap set.
        maven {
            url = uri("https://repo.maven.apache.org/maven2")
        }

        maven {
            name = "polocloud-snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots")
        }
    }

    // The polocloud-gradle-plugin's GenerateDependencyIndexTask resolves each
    // polocloudRuntime dependency's transitive tree independently and appends every
    // result to dependencies.index without deduplicating — so a library pulled in
    // transitively by two different polocloudRuntime entries (e.g. grpc-api via both
    // the grpc and tls bundles) ends up listed twice, sometimes at two different
    // resolved versions (e.g. kotlin-stdlib, log4j-core, guava). DependencyRegistry's
    // download step only dedupes by the full group:artifact:version key, so two
    // different versions of the same library both get downloaded and registered onto
    // the runtime classpath. Post-process the generated index here, keeping only the
    // highest version per group:artifact — same "newest wins" resolution Gradle
    // itself would apply to a normal dependency graph.
    plugins.withId("de.polocloud.gradle.plugin") {
        tasks.named("generateDependencyIndex").configure {
            doLast {
                dedupeDependencyIndex(outputs.files.singleFile)
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

/**
 * Project-wide test suite: runs the `test` task of every module in one go.
 *
 * Usage: `./gradlew allTests`. This is the cross-module entry point — JUnit's own
 * `@Suite` only aggregates tests within a single module's classpath, so Gradle is
 * the right layer to run everything at once.
 */
tasks.register("allTests") {
    group = "verification"
    description = "Runs the test task of every module (project-wide test suite)."
    dependsOn(subprojects.map { "${it.path}:test" })
}