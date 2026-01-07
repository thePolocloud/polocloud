package dev.httpmarco.polocloud.common.version

data class VersionParts(
    val numbers: IntArray,
    val preRelease: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VersionParts) return false
        if (!numbers.contentEquals(other.numbers)) return false
        if (preRelease != other.preRelease) return false
        return true
    }

    override fun hashCode(): Int {
        var result = numbers.contentHashCode()
        result = 31 * result + (preRelease?.hashCode() ?: 0)
        return result
    }
}