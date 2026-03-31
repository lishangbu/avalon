package io.github.lishangbu.avalon.dataset.repository

import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class CreatureEvolutionRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var evolutionChainRepository: EvolutionChainRepository

    @Resource
    private lateinit var creatureEvolutionRepository: CreatureEvolutionRepository

    @Test
    fun shouldLoadEvolutionChainSeedData() {
        val evolutionChain = requireNotNull(evolutionChainRepository.findNullable(100L))

        assertEquals(100L, evolutionChain.id)
        assertEquals(232L, evolutionChainRepository.loadBabyTriggerItemId(100L))
    }

    @Test
    fun shouldLoadLinearEvolutionEdges() {
        val evolutions = creatureEvolutionRepository.listByEvolutionChainId(1L)

        assertEquals(2, evolutions.size)
        assertEquals(16, evolutions.first().minLevel)
        assertEquals(32, evolutions.last().minLevel)
    }

    @Test
    fun shouldPreserveMultipleDetailsForSameEdge() {
        val evolutions = creatureEvolutionRepository.listByEvolutionChainId(106L)

        assertEquals(3, evolutions.size)
        assertEquals(1, evolutions.first().branchSortOrder)
        assertEquals(1, evolutions.first().detailSortOrder)
        assertEquals(3, evolutions.last().detailSortOrder)
    }

    @Test
    fun shouldPreserveEdgesWithoutDetails() {
        val evolutions = creatureEvolutionRepository.listByEvolutionChainId(442L)

        assertEquals(4, evolutions.size)
        val hydrapple = evolutions.last()
        assertEquals(1, hydrapple.branchSortOrder)
        assertEquals(1, hydrapple.detailSortOrder)
        assertEquals(null, hydrapple.minLevel)
        assertEquals(null, hydrapple.minHappiness)
    }

    @Test
    fun shouldLoadAlternativeLocationAndItemRules() {
        assertEquals(15, creatureEvolutionRepository.listByEvolutionChainId(67L).size)
        assertEquals(1, creatureEvolutionRepository.countByEvolutionChainIdAndLocationId(67L, 8L))
        assertEquals(1, creatureEvolutionRepository.countByEvolutionChainIdAndItemId(67L, 85L))
    }
}
