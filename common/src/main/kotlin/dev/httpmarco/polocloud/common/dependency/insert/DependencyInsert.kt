package dev.httpmarco.polocloud.common.dependency.insert

interface DependencyInsert<T> {

    fun renderDependency() : T

    fun connect()

}