import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    alias(libs.plugins.shadow)
}

group = "dev.httpmarco.polocloud.node"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation(libs.guice)
    implementation(libs.gson)
    implementation(libs.osgan.netty)
    implementation(libs.jline)
    implementation(libs.annotations)

    implementation(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(libs.log4j2)
    implementation(libs.log4j2.simple)
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.named("shadowJar", ShadowJar::class) {
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
    manifest {
        attributes["Main-Class"] = "dev.httpmarco.polocloud.node.launcher.NodeLauncher"
    }
}