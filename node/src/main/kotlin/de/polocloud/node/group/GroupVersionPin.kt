package de.polocloud.node.group

import de.polocloud.database.EntryIdentifier
import de.polocloud.database.RepositoryName

/**
 * Per-group memory of which platform build last came online successfully and which one is
 * currently quarantined for crash-looping, for whatever [Group.version] the group is
 * currently configured with (see [platformVersion]).
 *
 * This is its own table rather than columns added to [Group]: `Group` is an existing,
 * already-shipped table, and this project's DB layer (`polocloud-database`) has no
 * schema-migration step — `ensureTableExists` only checks that a table with the right name
 * exists, never that its columns match the current class, so a column an existing table
 * doesn't have throws on every read/write and gets silently swallowed into an empty
 * result/no-op (see `SqlExecutor`). A brand-new table has no such pre-existing rows to
 * conflict with. No constructor default values for the same reason [Group.static] documents
 * — a Kotlin default parameter makes the reflective row-mapping constructor lookup
 * (`SqlExecutor.resolveMeta()`) unreliable.
 */
@RepositoryName("group_version_pins")
data class GroupVersionPin(
    @EntryIdentifier val groupName: String,
    // The Group.version string this pin's good/bad build numbers were recorded against.
    // A pin only applies while the group is still configured for this exact version string —
    // an operator repointing the group at a different Minecraft version makes any earlier
    // build numbers meaningless (see PlatformVersionPinning.resolve).
    var platformVersion: String,
    // 0 means "no known-good build recorded yet".
    var goodBuild: Int,
    // 0 means "no build is currently quarantined".
    var badBuild: Int,
)
