plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.0.0"
}

dependencies {
    api(projects.shared)
    api(projects.common)
    api(libs.grpc.netty)
    api(libs.polocloud.proto)

    compileOnly(libs.gson)
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_21.toString()
    targetCompatibility = JavaVersion.VERSION_21.toString()
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveFileName = "sdk-java-3.0.0-pre.6.2-SNAPSHOT.jar"

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    relocate("com.google.protobuf", "dev.httpmarco.polocloud.sdk.java.relocated.protobuf")
}

tasks.jar {
    dependsOn(tasks.shadowJar)

    // Jar selbst soll nichts erzeugen
    enabled = false
}

artifacts {
    archives(tasks.shadowJar)
}