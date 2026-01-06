allprojects {
    apply(plugin = "maven-publish")

    group = "dev.httpmarco.polocloud"
    version = "3.0.0-pre.7-SNAPSHOT"

    repositories {
        mavenCentral()

        maven {
            name = "polocloud-snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}