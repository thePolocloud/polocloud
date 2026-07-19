package de.polocloud.updater

import de.polocloud.common.version.PolocloudReleaseChannel
import de.polocloud.common.version.PolocloudVersion
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateCheckerTest {

    private fun version(major: Int, minor: Int, patch: Int) =
        PolocloudVersion(major, minor, patch, PolocloudReleaseChannel.RELEASE, "1")

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
    fun `handles no releases gracefully`() {
        val message = UpdateChecker.evaluate(version(3, 0, 1), emptyList())

        assertTrue(message.contains("No GitHub releases"))
    }

    @Test
    fun `handles an unparsable tag gracefully`() {
        val message = UpdateChecker.evaluate(version(3, 0, 1), listOf(release("not-a-version")))

        assertTrue(message.contains("Could not parse"))
    }
}