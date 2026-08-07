package de.polocloud.node.group.template

import de.polocloud.node.utils.isSafePathSegment
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Manages the `local/templates/<name>` folders copied into a service's work directory
 * on start.
 *
 * Every template is a plain folder: its contents are overlaid onto the service work
 * directory as-is, before the platform's pre-start tasks ([de.polocloud.node.services.factory.task.TaskExecutor])
 * run — tasks then patch specific keys in whatever files the templates (or the platform
 * itself, on first launch) left behind, rather than the other way around.
 *
 * Not to be confused with [de.polocloud.node.services.factory.template.PlatformTemplate],
 * an unrelated concept (how a platform's versions/tasks are declared) that happens to
 * share the word "template".
 */
object GroupTemplateService {

    /** Always applied to every service, regardless of group, ahead of the role/group ones. */
    const val GLOBAL = "GLOBAL"

    /** Applied to services of a PROXY-type platform, after [GLOBAL]. */
    const val GLOBAL_PROXY = "GLOBAL_PROXY"

    /** Applied to services of a non-proxy (server) platform, after [GLOBAL]. */
    const val GLOBAL_SERVER = "GLOBAL_SERVER"

    private val logger = LoggerFactory.getLogger(GroupTemplateService::class.java)
    private val root = File("local/templates")

    /** Creates the three global template folders if they don't exist yet. Safe to call repeatedly. */
    fun ensureGlobalTemplates() {
        ensure(GLOBAL)
        ensure(GLOBAL_PROXY)
        ensure(GLOBAL_SERVER)
    }

    /** Creates `local/templates/<name>` if it doesn't exist yet. Safe to call repeatedly. */
    fun ensure(name: String) {
        directoryOf(name).mkdirs()
    }

    /**
     * The folder backing template [name], regardless of whether it currently exists.
     *
     * @throws IllegalArgumentException if [name] isn't safe as a single path segment
     * under [root] (see [isSafePathSegment]) — otherwise a name like `../../etc` would
     * resolve outside `local/templates/` entirely, letting [copyInto] read (or [delete]
     * remove) arbitrary files on the node's filesystem.
     */
    fun directoryOf(name: String): File {
        require(isSafePathSegment(name)) { "Invalid template name '$name'." }
        return File(root, name)
    }

    /** Names of every template folder currently under [root], sorted alphabetically. */
    fun listAll(): List<String> =
        (root.listFiles { file -> file.isDirectory } ?: emptyArray()).map { it.name }.sorted()

    /** Deletes template [name]'s folder and its contents. No-op (returns `false`) if it didn't exist. */
    fun delete(name: String): Boolean = directoryOf(name).deleteRecursively()

    /**
     * Copies the contents of every named template into [targetDir], in [templates] order,
     * so a later template's files overwrite an earlier one's on conflict.
     *
     * A template with no folder (never created or removed since) is skipped with a
     * warning rather than failing the service start — an empty/missing template is a
     * valid, common state (e.g. right after a group is created).
     */
    fun copyInto(templates: List<String>, targetDir: File) {
        for (name in templates) {
            val source = runCatching { directoryOf(name) }
                .getOrElse {
                    logger.error("Refusing to apply template '{}': {}", name, it.message)
                    continue
                }
            if (!source.isDirectory) {
                logger.warn("Template '{}' has no folder at {} — skipping", name, source.path)
                continue
            }
            runCatching { source.copyRecursively(targetDir, overwrite = true) }
                .onFailure { logger.error("Failed to apply template '{}' to {}: {}", name, targetDir.path, it.message) }
        }
    }
}
