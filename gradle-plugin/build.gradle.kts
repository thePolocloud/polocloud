plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("polocloud-plugin") {
            id = "dev.httpmarco.polocloud"
            implementationClass = "dev.httpmarco.polocloud.dependency.plugin.PolocloudDependencyPlugin"
        }
    }
}

repositories {
    mavenCentral()
}
