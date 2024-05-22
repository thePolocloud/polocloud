dependencies {
    implementation(project(":runner"))
    compileOnly(libs.osgan.netty)
}

tasks.jar {
    archiveFileName.set("api.jar")
}