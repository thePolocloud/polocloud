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
 *
 * Stability comes exclusively from the tag itself (parsed into a [PolocloudVersion] and its
 * [de.polocloud.common.version.PolocloudReleaseChannel]) — GitHub's own `draft`/`prerelease`
 * flags are metadata about how a release was published, not about our channel model, and are
 * not reliable stand-ins for it (e.g. every automated master build is published with GitHub's
 * `prerelease` flag set, regardless of its actual channel).
 */
object UpdateChecker {

    private val logger: Logger = LoggerFactory.getLogger("Updater")

    /**
     * Runs the check on a daemon background thread so it never delays boot. Never throws:
     * any failure (offline, GitHub unreachable, rate-limited, no usable release, ...) is
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
     * logic is testable without a network call.
     */
    internal fun evaluate(currentVersion: PolocloudVersion, releases: List<GithubRelease>): String {
        if (releases.isEmpty()) return "No GitHub releases found."

        val latest = findLatestEligibleVersion(currentVersion, releases)
            ?: return "No usable PoloCloud release found among ${releases.size} GitHub release(s)."

        return if (isUpdateAvailable(currentVersion, latest.version)) {
            formatUpdateAvailableMessage(currentVersion, latest.version, latest.release)
        } else {
            formatUpToDateMessage(currentVersion)
        }
    }

    private class EligibleRelease(val version: PolocloudVersion, val release: GithubRelease)

    /**
     * The highest parsed version among non-draft releases whose channel is at least as
     * stable as [currentVersion]'s — e.g. a node running [RELEASE][de.polocloud.common.version.PolocloudReleaseChannel.RELEASE]
     * is never offered a [DEV][de.polocloud.common.version.PolocloudReleaseChannel.DEV]
     * rolling build, but a node already running a pre-release channel is offered updates
     * within that channel or more stable ones. Unparsable tags and drafts are skipped
     * rather than aborting the whole check. Doesn't assume any particular list order —
     * every candidate is parsed and compared, not just the first one.
     */
    private fun findLatestEligibleVersion(currentVersion: PolocloudVersion, releases: List<GithubRelease>): EligibleRelease? =
        releases.asSequence()
            .filterNot { it.draft }
            .mapNotNull { release -> parseVersion(release.tagName)?.let { EligibleRelease(it, release) } }
            .filter { it.version.channel.priority >= currentVersion.channel.priority }
            .maxByOrNull { it.version }

    /** Parses a release tag (e.g. `v3.1.0` or `v3.0.5-dev.42`) into a [PolocloudVersion], or null if it isn't one. */
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