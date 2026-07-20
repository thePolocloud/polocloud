package de.polocloud.updater

import de.polocloud.common.version.PolocloudVersion
import de.polocloud.common.version.PolocloudVersionParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Checks GitHub releases for a newer PoloCloud version than the one currently running and
 * logs the result. Intentionally self-contained: a consumer (e.g. the node) only calls
 * [checkOnBootAsync] once after boot — the HTTP call, JSON parsing, version comparison and
 * logging all live here, so nothing else needs to know how the check works.
 */
object UpdateChecker {

    private val logger: Logger = LoggerFactory.getLogger("Updater")

    /**
     * Runs the check on a daemon background thread so it never delays boot. Never throws:
     * any failure (offline, GitHub unreachable, rate-limited, unparsable tag, ...) is
     * logged at DEBUG and otherwise swallowed — an update check is a nice-to-have, not
     * something that should ever affect node startup.
     */
    fun checkOnBootAsync(
        currentVersion: PolocloudVersion = PolocloudVersion.CURRENT,
        fetcher: ReleaseFetcher = GithubReleaseFetcher(),
    ) {
        Thread({
            runCatching { logger.info(evaluate(currentVersion, fetcher.fetchReleases())) }
                .onFailure { logger.debug("Update check failed: {}", it.message, it) }
        }, "update-checker").apply { isDaemon = true }.start()
    }

    /**
     * Pure evaluation step, split out from [checkOnBootAsync] so the version-comparison
     * logic is testable without a network call. [releases] is expected newest-first, as
     * GitHub's API returns them.
     */
    internal fun evaluate(currentVersion: PolocloudVersion, releases: List<GithubRelease>): String {
        val latestRelease = findLatestPublishedRelease(releases)
            ?: return "No GitHub releases found."

        val latestVersion = parseVersion(latestRelease.tagName)
            ?: return "Could not parse release tag '${latestRelease.tagName}' as a PoloCloud version."

        return if (isUpdateAvailable(currentVersion, latestVersion)) {
            formatUpdateAvailableMessage(currentVersion, latestVersion, latestRelease)
        } else {
            formatUpToDateMessage(currentVersion)
        }
    }

    /** Newest non-draft, non-prerelease entry — [releases] is expected newest-first, as GitHub's API returns them. */
    private fun findLatestPublishedRelease(releases: List<GithubRelease>): GithubRelease? =
        releases.firstOrNull { !it.draft && !it.prerelease }

    /** Parses a release tag (e.g. `v3.1.0`) into a [PolocloudVersion], or null if it isn't one. */
    private fun parseVersion(tagName: String): PolocloudVersion? =
        runCatching { PolocloudVersionParser.parse(tagName.removePrefix("v").removePrefix("V")) }.getOrNull()

    /**
     * isSameRelease first: a running build differs from its own released tag in
     * channel/build metadata (e.g. channel=RELEASE, build=<CI run number> vs. the
     * locally-built default build="local"), which the full isNewerThan ordering does
     * not ignore — only major/minor/patch actually matter for "is this up to date".
     */
    private fun isUpdateAvailable(currentVersion: PolocloudVersion, latestVersion: PolocloudVersion): Boolean =
        !latestVersion.isSameRelease(currentVersion) && latestVersion.isNewerThan(currentVersion)

    private fun formatUpdateAvailableMessage(
        currentVersion: PolocloudVersion,
        latestVersion: PolocloudVersion,
        latestRelease: GithubRelease,
    ): String = "A new PoloCloud version is available: ${latestVersion.toDisplayString()} " +
        "(currently running ${currentVersion.toDisplayString()}). Download: ${latestRelease.htmlUrl}"

    private fun formatUpToDateMessage(currentVersion: PolocloudVersion): String =
        "PoloCloud is up to date (${currentVersion.toDisplayString()})."
}