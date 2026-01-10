package dev.httpmarco.polocloud.cli

import dev.httpmarco.polocloud.common.dependency.Dependency
import dev.httpmarco.polocloud.common.dependency.DependencyRegistry
import dev.httpmarco.polocloud.common.dependency.scanning.LocalBlobScanner

fun main() {

    val dependencyRegistry = DependencyRegistry(LocalBlobScanner())


    println((Dependency::class as Any).javaClass.classLoader)

}