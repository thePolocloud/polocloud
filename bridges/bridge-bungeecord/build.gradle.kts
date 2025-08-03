repositories {
    maven {
        name = "bungeecord-repo"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
    maven {
        name = "minecraft-libraries"
        url = uri("https://libraries.minecraft.net/")
    }
}


dependencies {
    implementation(projects.sdk.sdkKotlin)
    implementation(projects.bridges.bridgeApi)

    compileOnly(libs.bungeecord)
}

