package dev.httpmarco.polocloud.common.dependency.scanning

import dev.httpmarco.polocloud.common.dependency.Dependency
import dev.httpmarco.polocloud.common.dependency.DependencyBlob

interface DependencyScanner<T> {

    fun scanDependencies(): List<T>

    fun mapToDependency(dependency: T): Dependency

    fun doScanning(): DependencyBlob {
        return DependencyBlob(
            scanDependencies().map { mapToDependency(it) }
        )
    }
}