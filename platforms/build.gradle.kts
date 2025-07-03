plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.gson)
    implementation(libs.bundles.confirationPool)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}

tasks.jar {
    archiveFileName.set("polocloud-platforms-$version.jar")
}