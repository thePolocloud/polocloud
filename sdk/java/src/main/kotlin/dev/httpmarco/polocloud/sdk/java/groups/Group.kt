package dev.httpmarco.polocloud.sdk.java.groups

import dev.httpmarco.polocloud.v1.proto.GroupProvider

class Group(private val snapshot : GroupProvider.GroupSnapshot) {

    fun name(): String = snapshot.name

    fun minimumMemory() = snapshot.minimumMemory

    fun maximumMemory() = snapshot.maximumMemory

    fun minimumOnline() = snapshot.minimumOnline

    fun maximumOnline() = snapshot.maximumOnline

}