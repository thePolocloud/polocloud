package de.polocloud.node.utils

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PathHandlingTest {

    @Test
    fun `accepts ordinary names`() {
        assertTrue(isSafePathSegment("lobby"))
        assertTrue(isSafePathSegment("lobby-1"))
        assertTrue(isSafePathSegment("GLOBAL_PROXY"))
        assertTrue(isSafePathSegment("my.group.v2"))
    }

    @Test
    fun `accepts a name containing a space`() {
        assertTrue(isSafePathSegment("my group"))
    }

    @Test
    fun `rejects blank names`() {
        assertFalse(isSafePathSegment(""))
        assertFalse(isSafePathSegment("   "))
    }

    @Test
    fun `rejects bare relative-directory references`() {
        assertFalse(isSafePathSegment("."))
        assertFalse(isSafePathSegment(".."))
    }

    @Test
    fun `rejects any path separator, including a traversal attempt`() {
        assertFalse(isSafePathSegment("../secrets"))
        assertFalse(isSafePathSegment("a/../../b"))
        assertFalse(isSafePathSegment("foo/bar"))
        assertFalse(isSafePathSegment("foo\\bar"))
        assertFalse(isSafePathSegment("/etc/passwd"))
    }

    @Test
    fun `rejects an embedded null byte`() {
        assertFalse(isSafePathSegment("foo" + Char(0) + "bar"))
    }
}
