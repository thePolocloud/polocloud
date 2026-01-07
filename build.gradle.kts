
allprojects {
    group = "dev.httpmarco.polocloud"
    version = "3.0.0-pre.10-SNAPSHOT"

    repositories {
        mavenCentral()

        maven {
            name = "polocloud-snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}