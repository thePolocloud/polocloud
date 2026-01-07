plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("dependencyPlugin") {
            id = "dev.httpmarco.polocloud.dependencies"
            implementationClass = "dev.httpmarco.polocloud.dependency.platform.PolocloudDependencyPlugin"
        }
    }
}

repositories {
    mavenCentral()
}
