package de.polocloud.node.module

/**
 * Why a module (or a jar that never even became a module) failed to load/enable, as
 * recorded in [ModuleManager.failures]. [source] tells a caller whether [ModuleManager.failures]'s
 * key for this entry is a module name ([Source.MODULE] — the descriptor parsed fine) or a
 * raw jar file name ([Source.JAR] — the failure happened before a module name was known,
 * e.g. the descriptor itself failed to parse, or it collided with an already-loaded name).
 */
data class ModuleFailure(
    val reason: String,
    val source: Source,
) {
    enum class Source { MODULE, JAR }
}
