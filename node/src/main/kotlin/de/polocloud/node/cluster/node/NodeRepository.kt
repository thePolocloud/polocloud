package de.polocloud.node.cluster.node

import de.polocloud.database.DatabaseAccess
import de.polocloud.database.DatabaseKey
import de.polocloud.database.filtering.Eq
import de.polocloud.proto.NodeState
import java.util.UUID

object NodeRepository {

    private val nodeDatabaseKey = DatabaseKey(NodeData::class)

    fun find(id: UUID) = DatabaseAccess.executor().findById(nodeDatabaseKey, id)

    fun find(state: NodeState) = DatabaseAccess.executor().find(nodeDatabaseKey, Eq("state", state))

    fun save(node: NodeData) = DatabaseAccess.executor().save(nodeDatabaseKey, node)

    fun delete(node: NodeData) = DatabaseAccess.executor().delete(nodeDatabaseKey, node)

    fun findAll() = DatabaseAccess.executor().findAll(nodeDatabaseKey)

    fun count() = DatabaseAccess.executor().count(nodeDatabaseKey)

}