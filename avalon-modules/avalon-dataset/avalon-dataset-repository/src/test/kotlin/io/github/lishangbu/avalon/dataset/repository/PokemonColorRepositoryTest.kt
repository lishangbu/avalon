package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.PokemonColor
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonColorSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PokemonColorRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var pokemonColorRepository: PokemonColorRepository

    @Test
    fun shouldInsertPokemonColorSuccessfully() {
        val pokemonColor =
            PokemonColor {
                internalName = "unit-test-pokemon-color"
                name = "单元测试颜色"
            }

        val saved = pokemonColorRepository.save(pokemonColor, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindPokemonColorById() {
        val pokemonColor = requireNotNull(pokemonColorRepository.loadViewById(1L))

        assertEquals("1", pokemonColor.id)
        assertEquals("black", pokemonColor.internalName)
        assertEquals("黑色", pokemonColor.name)
    }

    @Test
    fun shouldUpdatePokemonColorById() {
        val pokemonColor =
            pokemonColorRepository.save(
                PokemonColor {
                    internalName = "pokemon-color-update"
                    name = "原始颜色"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = pokemonColor.id

        pokemonColorRepository.save(
            PokemonColor(pokemonColor) {
                name = "更新后的颜色"
            },
            SaveMode.UPSERT,
        )

        val updatedPokemonColor = requireNotNull(pokemonColorRepository.findNullable(id))
        assertEquals("更新后的颜色", updatedPokemonColor.name)
    }

    @Test
    fun shouldDeletePokemonColorById() {
        val pokemonColor =
            pokemonColorRepository.save(
                PokemonColor {
                    internalName = "pokemon-color-delete"
                    name = "待删除颜色"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = pokemonColor.id

        assertNotNull(pokemonColorRepository.findNullable(deleteRecordId))
        pokemonColorRepository.deleteById(deleteRecordId)
        assertNull(pokemonColorRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = PokemonColorSpecification(internalName = "black")

        val results = pokemonColorRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "黑色" })
    }

    @Test
    fun shouldReturnAllPokemonColorsWhenNoCondition() {
        val results = pokemonColorRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "black" })
    }
}
