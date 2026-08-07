package de.polocloud.node.utils

import java.nio.file.Path
import kotlin.io.path.Path

private const val ROOT_DIR_PROPERTY = "rootDir"

fun rootDir() : Path {
    return Path(System.getProperty(ROOT_DIR_PROPERTY))
}

fun rootDir(path: Path)  {
    System.setProperty(ROOT_DIR_PROPERTY, path.toString())
}

/**
 * Whether [name] is safe to use as a single filesystem path segment (e.g. `File(root, name)`)
 * without risking path traversal outside its intended parent directory.
 *
 * Used for operator/API-supplied names that end up as directory names — group names
 * ([de.polocloud.node.group.GroupService.create], which are also implicitly used as a
 * template name — see `defaultTemplatesFor`) and template names
 * ([de.polocloud.node.group.template.GroupTemplateService.directoryOf], reachable from a
 * launched service's own identity via the `CopyTemplate` SDK call). A name containing a
 * path separator, or consisting only of `.`/`..`, would otherwise let `File(root, name)`
 * resolve to an arbitrary location on disk instead of a child of `root`.
 */
fun isSafePathSegment(name: String): Boolean {
    if (name.isBlank() || name == "." || name == "..") return false
    return name.none { it == '/' || it == '\\' || it.code == 0 }
}
