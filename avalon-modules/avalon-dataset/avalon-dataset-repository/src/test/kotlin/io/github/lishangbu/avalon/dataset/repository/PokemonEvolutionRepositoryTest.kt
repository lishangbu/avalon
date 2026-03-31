package io.github.lishangbu.avalon.dataset.repository

import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class PokemonEvolutionRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var evolutionChainRepository: EvolutionChainRepository

    @Resource
    private lateinit var pokemonEvolutionRepository: PokemonEvolutionRepository

    @Test
    fun shouldLoadEvolutionChainSeedData() {
        val evolutionChain = requireNotNull(evolutionChainRepository.findNullable(100L))

        assertEquals(100L, evolutionChain.id)
        assertEquals(232L, evolutionChainRepository.loadBabyTriggerItemId(100L))
    }

    @Test
    fun shouldLoadLinearEvolutionEdges() {
        val evolutions = pokemonEvolutionRepository.listByEvolutionChainId(1L)

        assertEquals(2, evolutions.size)
        assertEquals(16, evolutions.first().minLevel)
        assertEquals(32, evolutions.last().minLevel)
    }

    @Test
    fun shouldPreserveMultipleDetailsForSameEdge() {
        val evolutions = pokemonEvolutionRepository.listByEvolutionChainId(106L)

        assertEquals(3, evolutions.size)
        assertEquals(1, evolutions.first().branchSortOrder)
        assertEquals(1, evolutions.first().detailSortOrder)
        assertEquals(3, evolutions.last().detailSortOrder)
    }

    @Test
    fun shouldPreserveEdgesWithoutDetails() {
        val evolutions = pokemonEvolutionRepository.listByEvolutionChainId(442L)

        assertEquals(4, evolutions.size)
        val hydrapple = evolutions.last()
        assertEquals(1, hydrapple.branchSortOrder)
        assertEquals(1, hydrapple.detailSortOrder)
        assertEquals(null, hydrapple.minLevel)
        assertEquals(null, hydrapple.minHappiness)
    }

    @Test
    fun shouldLoadAlternativeLocationAndItemRules() {
        assertEquals(15, pokemonEvolutionRepository.listByEvolutionChainId(67L).size)
        assertEquals(1, pokemonEvolutionRepository.countByEvolutionChainIdAndLocationId(67L, 8L))
        assertEquals(1, pokemonEvolutionRepository.countByEvolutionChainIdAndItemId(67L, 85L))
    }
}
