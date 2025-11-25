plugins {
    kotlin("jvm") version "2.2.21"
}

dependencies {
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