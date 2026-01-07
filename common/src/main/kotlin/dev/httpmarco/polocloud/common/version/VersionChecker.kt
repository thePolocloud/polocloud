package dev.httpmarco.polocloud.common.version

/**
 * Utility for checking API version compatibility.
 *
 * Supports semantic versioning with operators:
 * - `>=3.0.0` - Greater than or equal to
 * - `>3.0.0` - Greater than
 * - `<=3.0.0` - Less than or equal to
 * - `<3.0.0` - Less than
 * - `=3.0.0` or `3.0.0` - Exact match
 * - `^3.0.0` - Compatible with (major version must match)
 * - `~3.0.0` - Approximately (major and minor must match)
 *
 * Supports pre-release versions: `3.0.0-pre.7-SNAPSHOT`
 */
object VersionChecker {

    private val VERSION_REGEX = Regex("""^([><=^~]*)(.+)$""")

    /**
     * Checks if the current version satisfies the required version constraint.
     *
     * @param requiredVersion The version constraint (e.g., ">=3.0.0-pre.7-SNAPSHOT")
     * @param currentVersion The current version
     * @return true if compatible, false otherwise
     */
    fun isCompatible(requiredVersion: String, currentVersion: String): Boolean {
        val (operator, version) = parseVersionConstraint(requiredVersion)

        return when (operator) {
            ">=" -> compareVersions(currentVersion, version) >= 0
            ">" -> compareVersions(currentVersion, version) > 0
            "<=" -> compareVersions(currentVersion, version) <= 0
            "<" -> compareVersions(currentVersion, version) < 0
            "=" -> compareVersions(currentVersion, version) == 0
            "^" -> isCaretCompatible(currentVersion, version)
            "~" -> isTildeCompatible(currentVersion, version)
            else -> compareVersions(currentVersion, version) == 0 // No operator = exact match
        }
    }

    /**
     * Parses a version constraint into operator and version.
     *
     * @return Pair of (operator, version)
     */
    private fun parseVersionConstraint(constraint: String): Pair<String, String> {
        val match = VERSION_REGEX.matchEntire(constraint.trim())
            ?: return "" to constraint.trim()

        val operator = match.groupValues[1]
        val version = match.groupValues[2]

        return operator to version
    }

    /**
     * Compares two semantic versions.
     *
     * @return negative if v1 < v2, zero if v1 == v2, positive if v1 > v2
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = parseVersion(v1)
        val parts2 = parseVersion(v2)

        // Compare major.minor.patch
        for (i in 0 until 3) {
            val cmp = parts1.numbers[i].compareTo(parts2.numbers[i])
            if (cmp != 0) return cmp
        }

        // If versions are equal, check pre-release
        return comparePreRelease(parts1.preRelease, parts2.preRelease)
    }

    /**
     * Caret compatibility (^): Major version must match.
     * ^3.0.0 allows 3.x.x but not 4.0.0
     */
    private fun isCaretCompatible(current: String, required: String): Boolean {
        val currentParts = parseVersion(current)
        val requiredParts = parseVersion(required)

        // Major version must match
        if (currentParts.numbers[0] != requiredParts.numbers[0]) return false

        // Current version must be >= required
        return compareVersions(current, required) >= 0
    }

    /**
     * Tilde compatibility (~): Major and minor version must match.
     * ~3.0.0 allows 3.0.x but not 3.1.0
     */
    private fun isTildeCompatible(current: String, required: String): Boolean {
        val currentParts = parseVersion(current)
        val requiredParts = parseVersion(required)

        // Major and minor must match
        if (currentParts.numbers[0] != requiredParts.numbers[0]) return false
        if (currentParts.numbers[1] != requiredParts.numbers[1]) return false

        // Current version must be >= required
        return compareVersions(current, required) >= 0
    }

    /**
     * Parses a version string into components.
     */
    private fun parseVersion(version: String): VersionParts {
        val parts = version.split("-", limit = 2)
        val versionNumbers = parts[0].split(".")
        val preRelease = if (parts.size > 1) parts[1] else null

        val numbers = IntArray(3) { idx ->
            versionNumbers.getOrNull(idx)?.toIntOrNull() ?: 0
        }

        return VersionParts(numbers, preRelease)
    }

    /**
     * Compares pre-release versions.
     * No pre-release (stable) > pre-release
     * Pre-release versions are compared alphabetically
     */
    private fun comparePreRelease(pre1: String?, pre2: String?): Int {
        return when {
            pre1 == null && pre2 == null -> 0
            pre1 == null -> 1  // Stable version is "greater" than pre-release
            pre2 == null -> -1
            else -> pre1.compareTo(pre2)
        }
    }
}