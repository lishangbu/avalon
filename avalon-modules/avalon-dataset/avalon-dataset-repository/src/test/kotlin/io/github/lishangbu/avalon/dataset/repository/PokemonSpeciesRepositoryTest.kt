package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.PokemonColor
import io.github.lishangbu.avalon.dataset.entity.PokemonHabitat
import io.github.lishangbu.avalon.dataset.entity.PokemonShape
import io.github.lishangbu.avalon.dataset.entity.PokemonSpecies
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpeciesSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@Transactional
class PokemonSpeciesRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var pokemonSpeciesRepository: PokemonSpeciesRepository

    @Test
    fun shouldQueryPageAndCrudPokemonSpecies() {
        val condition = PokemonSpeciesSpecification(internalName = "bulbasaur", growthRateId = "4", pokemonColorId = "5")

        val results = pokemonSpeciesRepository.listViews(condition)
        val page = pokemonSpeciesRepository.pageViews(condition, PageRequest.of(0, 10))

        assertFalse(results.isEmpty())
        assertEquals("1", results.first().id)
        assertEquals("bulbasaur", results.first().internalName)
        assertEquals("medium-slow", results.first().growthRate?.internalName)
        assertEquals("绿色", results.first().pokemonColor?.name)
        assertTrue(page.totalRowCount >= 1)
        assertFalse(page.rows.isEmpty())

        val created =
            pokemonSpeciesRepository.save(
                PokemonSpecies {
                    internalName = "test-species"
                    name = "测试种族"
                    sortingOrder = 9999
                    genderRate = 1
                    captureRate = 45
                    baseHappiness = 70
                    baby = false
                    legendary = false
                    mythical = false
                    hatchCounter = 20
                    hasGenderDifferences = false
                    formsSwitchable = false
                    evolvesFromSpeciesId = null
                    evolutionChainId = 1L
                    growthRate =
                        GrowthRate {
                            id = 4L
                        }
                    pokemonColor =
                        PokemonColor {
                            id = 5L
                        }
                    pokemonHabitat =
                        PokemonHabitat {
                            id = 3L
                        }
                    pokemonShape =
                        PokemonShape {
                            id = 8L
                        }
                },
                SaveMode.INSERT_ONLY,
            )

        val createdView = requireNotNull(pokemonSpeciesRepository.loadViewById(created.id))
        assertEquals("test-species", createdView.internalName)
        assertEquals("4", createdView.growthRate?.id)

        val existing = requireNotNull(pokemonSpeciesRepository.findNullable(created.id))
        pokemonSpeciesRepository.save(PokemonSpecies(existing) { name = "更新后的测试种族" }, SaveMode.UPSERT)

        val updated = requireNotNull(pokemonSpeciesRepository.loadViewById(created.id))
        assertEquals("更新后的测试种族", updated.name)

        pokemonSpeciesRepository.deleteById(created.id)
        assertNull(pokemonSpeciesRepository.loadViewById(created.id))
    }
}
