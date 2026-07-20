package de.polocloud.updater

import de.polocloud.common.version.PolocloudReleaseChannel
import de.polocloud.common.version.PolocloudVersion
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateCheckerTest {

    private fun version(major: Int, minor: Int, patch: Int, channel: PolocloudReleaseChannel = PolocloudReleaseChannel.RELEASE) =
        PolocloudVersion(major, minor, patch, channel, "1")

    private fun release(tag: String, draft: Boolean = false) =
        GithubRelease(tagName = tag, htmlUrl = "https://example.com/$tag", draft = draft)

    @Test
    fun `reports a newer version when the latest release tag is ahead`() {
        val message = UpdateChecker.evaluate(version(3, 0, 1), listOf(release("v3.0.2")))

        assertTrue(message.contains("new PoloCloud version"))
        assertTrue(message.contains("3.0.2"))
    }

    @Test
    fun `reports up to date when the latest release matches the running version`() {
        val message = UpdateChecker.evaluate(version(3, 0, 2), listOf(release("v3.0.2")))

        assertTrue(message.contains("up to date"))
    }

    @Test
    fun `reports up to date when the running version is ahead of the latest release`() {
        val message = UpdateChecker.evaluate(version(3, 1, 0), listOf(release("v3.0.2")))

        assertTrue(message.contains("up to date"))
    }

    @Test
    fun `skips draft releases and falls through to the newest published one`() {
        val message = UpdateChecker.evaluate(
            version(3, 0, 1),
            listOf(release("v9.9.9", draft = true), release("v3.0.2")),
        )

        assertTrue(message.contains("3.0.2"))
        assertTrue(!message.contains("9.9.9"))
    }

    @Test
    fun `picks the highest version regardless of list order`() {
        // GitHub returns releases newest-first, but evaluate() must not depend on that —
        // it compares every candidate rather than trusting position 0.
        val message = UpdateChecker.evaluate(
            version(3, 0, 1),
            listOf(release("v3.0.2"), release("v3.0.4"), release("v3.0.3")),
        )

        assertTrue(message.contains("3.0.4"))
    }

    @Test
    fun `does not offer a DEV rolling build to a node running a stable RELEASE channel`() {
        // Reflects the real CI setup: every push to master publishes an auto-incrementing
        // v3.0.x-dev.<build> GitHub pre-release. A RELEASE-channel node must not be nagged
        // about those; it should only ever be offered an equal-or-more-stable release.
        val message = UpdateChecker.evaluate(
            version(3, 0, 1, PolocloudReleaseChannel.RELEASE),
            listOf(release("v9.9.9-dev.42")),
        )

        assertTrue(message.contains("No usable PoloCloud release found"))
        assertTrue(!message.contains("9.9.9"))
    }

    @Test
    fun `offers a same-channel DEV update to a node already running DEV`() {
        val message = UpdateChecker.evaluate(
            version(3, 0, 1, PolocloudReleaseChannel.DEV),
            listOf(release("v3.0.2-dev.42")),
        )

        assertTrue(message.contains("new PoloCloud version"))
        assertTrue(message.contains("3.0.2"))
    }

    @Test
    fun `handles no releases gracefully`() {
        val message = UpdateChecker.evaluate(version(3, 0, 1), emptyList())

        assertTrue(message.contains("No GitHub releases"))
    }

    @Test
    fun `handles a release list with no usable tag gracefully`() {
        val message = UpdateChecker.evaluate(version(3, 0, 1), listOf(release("not-a-version")))

        assertTrue(message.contains("No usable PoloCloud release found"))
    }
}