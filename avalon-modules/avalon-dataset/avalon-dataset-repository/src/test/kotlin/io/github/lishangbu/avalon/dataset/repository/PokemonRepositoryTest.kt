package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Pokemon
import io.github.lishangbu.avalon.dataset.entity.PokemonSpecies
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpecification
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
class PokemonRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var pokemonRepository: PokemonRepository

    @Test
    fun shouldQueryPageAndCrudPokemon() {
        val condition = PokemonSpecification(internalName = "bulbasaur", pokemonSpeciesId = "1")

        val results = pokemonRepository.listViews(condition)
        val page = pokemonRepository.pageViews(condition, PageRequest.of(0, 10))

        assertFalse(results.isEmpty())
        assertEquals("1", results.first().id)
        assertEquals("bulbasaur", results.first().internalName)
        assertEquals("妙蛙种子", results.first().pokemonSpecies?.name)
        assertTrue(page.totalRowCount >= 1)
        assertFalse(page.rows.isEmpty())

        val created =
            pokemonRepository.save(
                Pokemon {
                    internalName = "testmon"
                    name = "测试宝可梦"
                    height = 10
                    weight = 100
                    baseExperience = 200
                    sortingOrder = 9999
                    pokemonSpecies =
                        PokemonSpecies {
                            id = 1L
                        }
                },
                SaveMode.INSERT_ONLY,
            )

        val createdView = requireNotNull(pokemonRepository.loadViewById(created.id))
        assertEquals("testmon", createdView.internalName)
        assertEquals("1", createdView.pokemonSpecies?.id)

        val existing = requireNotNull(pokemonRepository.findNullable(created.id))
        pokemonRepository.save(Pokemon(existing) { name = "更新后的测试宝可梦" }, SaveMode.UPSERT)

        val updated = requireNotNull(pokemonRepository.loadViewById(created.id))
        assertEquals("更新后的测试宝可梦", updated.name)

        pokemonRepository.deleteById(created.id)
        assertNull(pokemonRepository.loadViewById(created.id))
    }
}
