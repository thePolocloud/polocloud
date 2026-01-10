package dev.httpmarco.polocloud.common.dependency.scanning

import dev.httpmarco.polocloud.common.dependency.Dependency

interface DependencyScanner<T> {

    fun scanDependencies(): List<T>

    fun mapToDependency(dependency: T): Dependency

}