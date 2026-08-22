package de.polocloud.node.services.factory.template

import kotlinx.serialization.json.*

/**
 * Resolves a dot/bracket-notation JSON path against a root [JsonElement].
 *
 * Supports:
 * - Field access: `builds.latest`
 * - Numeric indices: `builds[-1]`, `builds[0]`
 * - String-key brackets: `downloads["server:default"]`
 *
 * @param root The JSON element to traverse.
 * @param path The path expression to resolve.
 * @return The resolved element, or null if any segment cannot be navigated.
 */
internal fun resolveElement(root: JsonElement, path: String): JsonElement? {
    if (path.isBlank()) return root
    var current: JsonElement = root
    for (token in tokenizePath(path)) {
        current = if (token.startsWith("[") && token.endsWith("]")) {
            val inner = token.drop(1).dropLast(1)
            val idx = inner.toIntOrNull()
            if (idx != null) {
                val arr = current as? JsonArray ?: return null
                arr.getOrNull(if (idx < 0) arr.size + idx else idx) ?: return null
            } else {
                (current as? JsonObject)?.get(inner.trim('"')) ?: return null
            }
        } else {
            (current as? JsonObject)?.get(token) ?: return null
        }
    }
    return current
}

/**
 * Resolves [versionPath] to a flat list of version strings from [root].
 *
 * Trailing numeric indices are stripped so the full array or object is evaluated:
 * - [JsonArray]  → each element as a string
 * - [JsonPrimitive] → single-element list
 * - [JsonObject] → treated as a family map; all nested arrays are flattened
 *
 * @param root        Root JSON element returned by the base API endpoint.
 * @param versionPath JSON path pointing to the version array or family map.
 * @return Flat list of all version strings, or empty if the path cannot be resolved.
 */
internal fun resolveAllVersions(root: JsonElement, versionPath: String): List<String> {
    val arrayPath = versionPath.replace(Regex("\\[-?\\d+]$"), "")
    val element = resolveElement(root, arrayPath) ?: return emptyList()
    return when (element) {
        is JsonArray -> element.mapNotNull { it.jsonPrimitive.contentOrNull }
        is JsonPrimitive -> listOf(element.content)
        is JsonObject -> element.values.flatMap { family ->
            (family as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        }
    }
}

/**
 * Splits a JSON path string into individual field and bracket tokens.
 *
 * Examples:
 * - `builds[-1].build` → `["builds", "[-1]", "build"]`
 * - `downloads["server:default"].url` → `["downloads", "[\"server:default\"]", "url"]`
 * - `[0].id` → `["[0]", "id"]`
 */
private fun tokenizePath(path: String): List<String> {
    val tokens = mutableListOf<String>()
    var i = 0
    val current = StringBuilder()
    while (i < path.length) {
        when {
            path[i] == '[' -> {
                if (current.isNotEmpty()) { tokens.add(current.toString()); current.clear() }
                val end = path.indexOf(']', i)
                tokens.add(path.substring(i, end + 1))
                i = end + 1
                if (i < path.length && path[i] == '.') i++
            }
            path[i] == '.' -> {
                if (current.isNotEmpty()) { tokens.add(current.toString()); current.clear() }
                i++
            }
            else -> { current.append(path[i]); i++ }
        }
    }
    if (current.isNotEmpty()) tokens.add(current.toString())
    return tokens
}
