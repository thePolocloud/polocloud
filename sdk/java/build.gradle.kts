plugins {
    kotlin("jvm") version "2.2.0"
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.bundles.proto)
    implementation(libs.grpc.netty)
    implementation(project(":proto"))
}

tasks.named("compileJava") {
    dependsOn(":proto:classes")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(23)
}