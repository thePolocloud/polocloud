package dev.httpmarco.polocloud.agent.platform

import dev.httpmarco.polocloud.platforms.PlatformPool
import dev.httpmarco.polocloud.shared.platform.Platform
import dev.httpmarco.polocloud.shared.platform.PlatformVersion
import dev.httpmarco.polocloud.shared.platform.SharedPlatformProvider
import dev.httpmarco.polocloud.v1.groups.GroupType

class PlatformStorageImpl : SharedPlatformProvider<Platform> {
    override fun findAll(): List<Platform> {
        return PlatformPool.platforms()
            .map {
                Platform(
                    it.name,
                    it.type,
                    it.versions.map { v -> PlatformVersion(v.version) })
            }
    }

    override fun find(name: String): Platform? {
        return PlatformPool.platforms()
            .filter { it.name == name }
            .map {
                Platform(
                    it.name,
                    it.type,
                    it.versions.map { v -> PlatformVersion(v.version) })
            }
            .firstOrNull()
    }

    override fun find(type: GroupType): List<Platform> {
        return PlatformPool.platforms()
            .filter { it.type == type }
            .map {
                Platform(
                    it.name,
                    it.type,
                    it.versions.map { v -> PlatformVersion(v.version) })
            }
    }
}