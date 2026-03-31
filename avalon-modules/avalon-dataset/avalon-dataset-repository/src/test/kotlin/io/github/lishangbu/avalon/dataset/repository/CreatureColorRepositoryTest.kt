package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.CreatureColor
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureColorSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreatureColorRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var creatureColorRepository: CreatureColorRepository

    @Test
    fun shouldInsertCreatureColorSuccessfully() {
        val creatureColor =
            CreatureColor {
                internalName = "unit-test-pokemon-color"
                name = "单元测试颜色"
            }

        val saved = creatureColorRepository.save(creatureColor, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindCreatureColorById() {
        val creatureColor = requireNotNull(creatureColorRepository.loadViewById(1L))

        assertEquals("1", creatureColor.id)
        assertEquals("black", creatureColor.internalName)
        assertEquals("黑色", creatureColor.name)
    }

    @Test
    fun shouldUpdateCreatureColorById() {
        val creatureColor =
            creatureColorRepository.save(
                CreatureColor {
                    internalName = "pokemon-color-update"
                    name = "原始颜色"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = creatureColor.id

        creatureColorRepository.save(
            CreatureColor(creatureColor) {
                name = "更新后的颜色"
            },
            SaveMode.UPSERT,
        )

        val updatedCreatureColor = requireNotNull(creatureColorRepository.findNullable(id))
        assertEquals("更新后的颜色", updatedCreatureColor.name)
    }

    @Test
    fun shouldDeleteCreatureColorById() {
        val creatureColor =
            creatureColorRepository.save(
                CreatureColor {
                    internalName = "pokemon-color-delete"
                    name = "待删除颜色"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = creatureColor.id

        assertNotNull(creatureColorRepository.findNullable(deleteRecordId))
        creatureColorRepository.deleteById(deleteRecordId)
        assertNull(creatureColorRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = CreatureColorSpecification(internalName = "black")

        val results = creatureColorRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "黑色" })
    }

    @Test
    fun shouldReturnAllCreatureColorsWhenNoCondition() {
        val results = creatureColorRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "black" })
    }
}
