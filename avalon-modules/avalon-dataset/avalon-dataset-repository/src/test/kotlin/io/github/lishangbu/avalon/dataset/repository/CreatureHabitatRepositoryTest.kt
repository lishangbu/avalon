package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.CreatureHabitat
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureHabitatSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreatureHabitatRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var creatureHabitatRepository: CreatureHabitatRepository

    @Test
    fun shouldInsertCreatureHabitatSuccessfully() {
        val creatureHabitat =
            CreatureHabitat {
                internalName = "unit-test-pokemon-habitat"
                name = "单元测试栖息地"
            }

        val saved = creatureHabitatRepository.save(creatureHabitat, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindCreatureHabitatById() {
        val creatureHabitat = requireNotNull(creatureHabitatRepository.loadViewById(1L))

        assertEquals("1", creatureHabitat.id)
        assertEquals("cave", creatureHabitat.internalName)
        assertEquals("cave", creatureHabitat.name)
    }

    @Test
    fun shouldUpdateCreatureHabitatById() {
        val creatureHabitat =
            creatureHabitatRepository.save(
                CreatureHabitat {
                    internalName = "pokemon-habitat-update"
                    name = "原始栖息地"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = creatureHabitat.id

        creatureHabitatRepository.save(
            CreatureHabitat(creatureHabitat) {
                name = "更新后的栖息地"
            },
            SaveMode.UPSERT,
        )

        val updatedCreatureHabitat = requireNotNull(creatureHabitatRepository.findNullable(id))
        assertEquals("更新后的栖息地", updatedCreatureHabitat.name)
    }

    @Test
    fun shouldDeleteCreatureHabitatById() {
        val creatureHabitat =
            creatureHabitatRepository.save(
                CreatureHabitat {
                    internalName = "pokemon-habitat-delete"
                    name = "待删除栖息地"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = creatureHabitat.id

        assertNotNull(creatureHabitatRepository.findNullable(deleteRecordId))
        creatureHabitatRepository.deleteById(deleteRecordId)
        assertNull(creatureHabitatRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = CreatureHabitatSpecification(internalName = "cave")

        val results = creatureHabitatRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "cave" })
    }

    @Test
    fun shouldReturnAllCreatureHabitatsWhenNoCondition() {
        val results = creatureHabitatRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "cave" })
    }
}
