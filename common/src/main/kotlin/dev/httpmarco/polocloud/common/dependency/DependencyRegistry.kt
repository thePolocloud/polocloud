package dev.httpmarco.polocloud.common.dependency

import dev.httpmarco.polocloud.common.dependency.insert.DependencyInsert
import dev.httpmarco.polocloud.common.dependency.scanning.DependencyScanner

class DependencyRegistry(val insert: DependencyInsert<*>) {

    private val registeredDependencies = mutableListOf<Dependency>()

    fun scan(scanner: DependencyScanner<*>) {
        this.registeredDependencies.addAll(scanner.doScanning().blobEntries)

        this.registeredDependencies.forEach {
            println("Registered dependency: ${it.groupId}:${it.artifactId}:${it.version}")
        }
    }

    fun bind() {
        this.insert.renderDependency()
    }
}