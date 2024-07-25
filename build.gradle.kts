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

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}


tasks.named("shadowJar", ShadowJar::class) {
    mergeServiceFiles()
    archiveFileName.set("${project.name}.jar")
    manifest {
        attributes["Main-Class"] = "dev.httpmarco.polocloud.node.launcher.NodeLauncher"
    }
}