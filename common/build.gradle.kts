import de.polocloud.gradle.plugin.polocloudRuntime

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.gradle.git.properties)
    alias(libs.plugins.polocloud.gradle.plugin)
}

dependencies {
    compileOnly(libs.bundles.grpc)
    compileOnly(libs.bundles.logging)
    compileOnly(libs.polocloud.i18n)

    polocloudRuntime(libs.kotlinx.serialization.json)

    compileOnly(libs.bundles.tls)
    runtimeOnly(libs.bundles.tls)

    polocloudRuntime(libs.oshi)

    //database
    polocloudRuntime(libs.polocloud.database)

    // testing — the command framework logs via slf4j and the i18n helpers, so both
    // must be on the test runtime classpath (they are compileOnly for main).
    testImplementation(libs.bundles.testing)
    testImplementation(libs.bundles.logging)
    testImplementation(libs.polocloud.i18n)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

gitProperties {
    extProperty = "gitProps"
    keys = listOf(
        "git.commit.id",
        "git.commit.id.abbrev"
    )
}

tasks.processResources {
    dependsOn(tasks.generateGitProperties)
}

tasks.withType<Jar>().configureEach {
    if (name == "sourcesJar") {
        dependsOn(tasks.generateGitProperties)
    }
}

kotlin {
    jvmToolchain(25)
}