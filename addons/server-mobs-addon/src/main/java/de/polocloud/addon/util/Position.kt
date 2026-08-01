package de.polocloud.addon.util

import kotlinx.serialization.Serializable

/** An exact world position. Platform-agnostic in shape; only world-based platforms use it. */
@Serializable
data class Position(val x: Double, val y: Double, val z: Double, val world: String)