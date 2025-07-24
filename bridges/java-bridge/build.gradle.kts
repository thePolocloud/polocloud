plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-rc2"
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)

    implementation(project(":sdk:java"))
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_21.toString()
    targetCompatibility = JavaVersion.VERSION_21.toString()
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveFileName.set("polocloud-java-bridge-$version.jar")
}