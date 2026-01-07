subprojects {
    repositories {
        mavenCentral()

        group = "dev.httpmarco.polocloud"
        version = "3.0.0-pre.10-SNAPSHOT"


        maven {
            name = "polocloud-snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}