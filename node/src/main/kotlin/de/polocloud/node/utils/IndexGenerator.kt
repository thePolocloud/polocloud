package de.polocloud.node.utils

import de.polocloud.node.cluster.node.NodeRepository

object IndexGenerator {

    fun generateNode(): Int {
        val usedIndexes = NodeRepository.findAll()
            .map { it.nodeIndex }
            .toSet()

        return generate(usedIndexes)
    }


    private fun generate(usedIndexes: Iterable<Int>): Int {
        var index = 1
        while (index in usedIndexes) {
            index++
        }

        return index
    }
}