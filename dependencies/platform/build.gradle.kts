plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("dependencyPlatformPlugin") {
            id = "dev.httpmarco.polocloud.dependency-platform"
            implementationClass = "dev.httpmarco.polocloud.dependency.platform.PolocloudDependencyPlatformPlugin"
        }
    }
}
