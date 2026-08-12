package de.polocloud.node.player

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseKey
import java.util.UUID

object CloudPlayerRepository {

    private val playerDatabaseKey = DatabaseKey(CloudPlayer::class)

    fun save(player: CloudPlayer) = DatabaseAccess.executor().save(playerDatabaseKey, player)

    fun delete(player: CloudPlayer) = DatabaseAccess.executor().delete(playerDatabaseKey, player)

    fun findAll() = DatabaseAccess.executor().findAll(playerDatabaseKey)

    fun findById(id: UUID) = DatabaseAccess.executor().findById(playerDatabaseKey, id)

    // Filtered in-memory (not via a DB Eq filter) so name matching is reliably
    // case-insensitive regardless of the backing database's collation settings —
    // mirrors NodeArgument/ServiceArgument's own name-lookup style.
    fun findByName(name: String) = findAll().firstOrNull { it.name.equals(name, ignoreCase = true) }
}
