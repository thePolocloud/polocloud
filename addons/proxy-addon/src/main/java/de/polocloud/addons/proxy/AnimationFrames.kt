package de.polocloud.addons.proxy

/** Picks the animation frame due at the current moment, cycling every [tickIntervalMillis] — driven purely by wall-clock time, so no ticking counter needs to be kept in sync between the MOTD (pull, per ping) and the tab list (push, per scheduled tick). */
object AnimationFrames {

    fun <T> current(frames: List<T>, tickIntervalMillis: Long): T? {
        if (frames.isEmpty()) return null
        if (tickIntervalMillis <= 0) return frames.first()

        val index = ((System.currentTimeMillis() / tickIntervalMillis) % frames.size).toInt()
        return frames[index]
    }
}
