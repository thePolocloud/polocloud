dependencies {
    implementation(project(":runner"))
    implementation(libs.osgan.netty)
    implementation(project(":api"))
}

tasks.jar {
    archiveFileName.set("instance.jar")
}