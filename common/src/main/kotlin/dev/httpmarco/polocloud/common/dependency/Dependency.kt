package dev.httpmarco.polocloud.common.dependency

data class Dependency(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val url: String,
    val checksum: String
) {

}