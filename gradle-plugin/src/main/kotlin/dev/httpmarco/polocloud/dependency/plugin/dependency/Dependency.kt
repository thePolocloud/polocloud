package dev.httpmarco.polocloud.dependency.plugin.dependency

data class Dependency(val groupId: String, val artifactId: String, val version: String, val url: String, val sha256: String) {

    fun toNotation(): String {
        return "$groupId;$artifactId;$version;$url;$sha256"
    }
}
