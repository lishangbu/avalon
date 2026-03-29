package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.PokemonHabitat
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonHabitatSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PokemonHabitatRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var pokemonHabitatRepository: PokemonHabitatRepository

    @Test
    fun shouldInsertPokemonHabitatSuccessfully() {
        val pokemonHabitat =
            PokemonHabitat {
                internalName = "unit-test-pokemon-habitat"
                name = "单元测试栖息地"
            }

        val saved = pokemonHabitatRepository.save(pokemonHabitat, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindPokemonHabitatById() {
        val pokemonHabitat = requireNotNull(pokemonHabitatRepository.loadViewById(1L))

        assertEquals("1", pokemonHabitat.id)
        assertEquals("cave", pokemonHabitat.internalName)
        assertEquals("cave", pokemonHabitat.name)
    }

    @Test
    fun shouldUpdatePokemonHabitatById() {
        val pokemonHabitat =
            pokemonHabitatRepository.save(
                PokemonHabitat {
                    internalName = "pokemon-habitat-update"
                    name = "原始栖息地"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = pokemonHabitat.id

        pokemonHabitatRepository.save(
            PokemonHabitat(pokemonHabitat) {
                name = "更新后的栖息地"
            },
            SaveMode.UPSERT,
        )

        val updatedPokemonHabitat = requireNotNull(pokemonHabitatRepository.findNullable(id))
        assertEquals("更新后的栖息地", updatedPokemonHabitat.name)
    }

    @Test
    fun shouldDeletePokemonHabitatById() {
        val pokemonHabitat =
            pokemonHabitatRepository.save(
                PokemonHabitat {
                    internalName = "pokemon-habitat-delete"
                    name = "待删除栖息地"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = pokemonHabitat.id

        assertNotNull(pokemonHabitatRepository.findNullable(deleteRecordId))
        pokemonHabitatRepository.deleteById(deleteRecordId)
        assertNull(pokemonHabitatRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = PokemonHabitatSpecification(internalName = "cave")

        val results = pokemonHabitatRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "cave" })
    }

    @Test
    fun shouldReturnAllPokemonHabitatsWhenNoCondition() {
        val results = pokemonHabitatRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "cave" })
    }
}
