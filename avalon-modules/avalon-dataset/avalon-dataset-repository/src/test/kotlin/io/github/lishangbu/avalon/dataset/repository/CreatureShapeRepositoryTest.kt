package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.CreatureShape
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureShapeSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreatureShapeRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var creatureShapeRepository: CreatureShapeRepository

    @Test
    fun shouldInsertCreatureShapeSuccessfully() {
        val creatureShape =
            CreatureShape {
                internalName = "unit-test-pokemon-shape"
                name = "单元测试形状"
            }

        val saved = creatureShapeRepository.save(creatureShape, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindCreatureShapeById() {
        val creatureShape = requireNotNull(creatureShapeRepository.loadViewById(1L))

        assertEquals("1", creatureShape.id)
        assertEquals("ball", creatureShape.internalName)
        assertEquals("Ball", creatureShape.name)
    }

    @Test
    fun shouldUpdateCreatureShapeById() {
        val creatureShape =
            creatureShapeRepository.save(
                CreatureShape {
                    internalName = "pokemon-shape-update"
                    name = "原始形状"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = creatureShape.id

        creatureShapeRepository.save(
            CreatureShape(creatureShape) {
                name = "更新后的形状"
            },
            SaveMode.UPSERT,
        )

        val updatedCreatureShape = requireNotNull(creatureShapeRepository.findNullable(id))
        assertEquals("更新后的形状", updatedCreatureShape.name)
    }

    @Test
    fun shouldDeleteCreatureShapeById() {
        val creatureShape =
            creatureShapeRepository.save(
                CreatureShape {
                    internalName = "pokemon-shape-delete"
                    name = "待删除形状"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = creatureShape.id

        assertNotNull(creatureShapeRepository.findNullable(deleteRecordId))
        creatureShapeRepository.deleteById(deleteRecordId)
        assertNull(creatureShapeRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = CreatureShapeSpecification(internalName = "ball")

        val results = creatureShapeRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "Ball" })
    }

    @Test
    fun shouldReturnAllCreatureShapesWhenNoCondition() {
        val results = creatureShapeRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "ball" })
    }
}
