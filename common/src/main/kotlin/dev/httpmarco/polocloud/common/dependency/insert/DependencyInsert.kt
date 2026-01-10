package dev.httpmarco.polocloud.common.dependency.insert

import dev.httpmarco.polocloud.common.dependency.Dependency

abstract class DependencyInsert<T> {

    abstract fun renderDependency(dependency: Dependency): T

    abstract fun connect(element: T)

    fun register(dependency: Dependency) = connect(renderDependency(dependency))

}