package dev.httpmarco.polocloud.dependency.plugin

abstract class PolocloudDependencyExtension {

    var mainClass: String? = null
    val projects = mutableListOf<String>()

    fun include(vararg paths: String) {
        projects.addAll(paths)
    }
}