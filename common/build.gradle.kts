plugins {
    kotlin("jvm") version "2.3.0"
}

dependencies {
    implementation("com.google.guava:guava:33.5.0-jre")
    testImplementation(kotlin("test"))
    api(libs.gson)
    compileOnly(libs.polocloud.proto)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

tasks.jar {
    archiveFileName.set("polocloud-common-$version.jar")
}