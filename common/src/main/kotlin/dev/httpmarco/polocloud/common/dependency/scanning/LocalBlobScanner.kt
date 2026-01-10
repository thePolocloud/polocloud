package dev.httpmarco.polocloud.common.dependency.scanning

import dev.httpmarco.polocloud.common.dependency.Dependency

class LocalBlobScanner : DependencyScanner<String> {
    override fun scanDependencies(): List<String> {
        TODO("Not yet implemented")
    }

    override fun mapToDependency(dependency: String): Dependency {
        val parts = dependency.split(":")

        return Dependency(
            groupId =  parts[0],
            artifactId = parts[1],
            version = parts[2],
            url = parts[3],
            checksum =  parts[4]
        )
    }
}