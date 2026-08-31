package de.polocloud.node.group

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseKey

object GroupVersionPinRepository {

    private val groupVersionPinDatabaseKey = DatabaseKey(GroupVersionPin::class)

    fun find(groupName: String): GroupVersionPin? =
        DatabaseAccess.executor().findById(groupVersionPinDatabaseKey, groupName)

    fun save(pin: GroupVersionPin) = DatabaseAccess.executor().save(groupVersionPinDatabaseKey, pin)

    fun delete(groupName: String) {
        find(groupName)?.let { DatabaseAccess.executor().delete(groupVersionPinDatabaseKey, it) }
    }
}
