package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.PokemonShape
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonShapeSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PokemonShapeRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var pokemonShapeRepository: PokemonShapeRepository

    @Test
    fun shouldInsertPokemonShapeSuccessfully() {
        val pokemonShape =
            PokemonShape {
                internalName = "unit-test-pokemon-shape"
                name = "单元测试形状"
            }

        val saved = pokemonShapeRepository.save(pokemonShape, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindPokemonShapeById() {
        val pokemonShape = requireNotNull(pokemonShapeRepository.loadViewById(1L))

        assertEquals("1", pokemonShape.id)
        assertEquals("ball", pokemonShape.internalName)
        assertEquals("Ball", pokemonShape.name)
    }

    @Test
    fun shouldUpdatePokemonShapeById() {
        val pokemonShape =
            pokemonShapeRepository.save(
                PokemonShape {
                    internalName = "pokemon-shape-update"
                    name = "原始形状"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = pokemonShape.id

        pokemonShapeRepository.save(
            PokemonShape(pokemonShape) {
                name = "更新后的形状"
            },
            SaveMode.UPSERT,
        )

        val updatedPokemonShape = requireNotNull(pokemonShapeRepository.findNullable(id))
        assertEquals("更新后的形状", updatedPokemonShape.name)
    }

    @Test
    fun shouldDeletePokemonShapeById() {
        val pokemonShape =
            pokemonShapeRepository.save(
                PokemonShape {
                    internalName = "pokemon-shape-delete"
                    name = "待删除形状"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = pokemonShape.id

        assertNotNull(pokemonShapeRepository.findNullable(deleteRecordId))
        pokemonShapeRepository.deleteById(deleteRecordId)
        assertNull(pokemonShapeRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = PokemonShapeSpecification(internalName = "ball")

        val results = pokemonShapeRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "Ball" })
    }

    @Test
    fun shouldReturnAllPokemonShapesWhenNoCondition() {
        val results = pokemonShapeRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "ball" })
    }
}
