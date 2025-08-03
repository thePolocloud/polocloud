plugins {
    alias(libs.plugins.nexus.publish)
}

allprojects {
    apply(plugin = "maven-publish")

    group = "dev.httpmarco.polocloud"
    version = "3.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}


nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/releases/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))

            username.set(System.getenv("ossrhUsername") ?: "")
            password.set(System.getenv("ossrhPassword") ?: "")
        }
    }
    useStaging.set(!project.rootProject.version.toString().endsWith("-SNAPSHOT"))
}