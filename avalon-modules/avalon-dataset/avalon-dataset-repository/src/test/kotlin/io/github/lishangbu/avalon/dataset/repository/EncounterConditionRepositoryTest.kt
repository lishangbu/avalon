package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.EncounterCondition
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EncounterConditionRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var encounterConditionRepository: EncounterConditionRepository

    @Test
    fun shouldInsertEncounterConditionSuccessfully() {
        val encounterCondition =
            EncounterCondition {
                internalName = "unit-test-encounter-condition"
                name = "单元测试遭遇条件"
            }

        val saved = encounterConditionRepository.save(encounterCondition, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindEncounterConditionById() {
        val encounterCondition = requireNotNull(encounterConditionRepository.loadViewById(1L))

        assertEquals("1", encounterCondition.id)
        assertEquals("swarm", encounterCondition.internalName)
        assertEquals("Swarm", encounterCondition.name)
    }

    @Test
    fun shouldUpdateEncounterConditionById() {
        val encounterCondition =
            encounterConditionRepository.save(
                EncounterCondition {
                    internalName = "encounter-condition-update"
                    name = "原始遭遇条件"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = encounterCondition.id

        encounterConditionRepository.save(
            EncounterCondition(encounterCondition) {
                name = "更新后的遭遇条件"
            },
            SaveMode.UPSERT,
        )

        val updatedEncounterCondition = requireNotNull(encounterConditionRepository.findNullable(id))
        assertEquals("更新后的遭遇条件", updatedEncounterCondition.name)
    }

    @Test
    fun shouldDeleteEncounterConditionById() {
        val encounterCondition =
            encounterConditionRepository.save(
                EncounterCondition {
                    internalName = "encounter-condition-delete"
                    name = "待删除遭遇条件"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = encounterCondition.id

        assertNotNull(encounterConditionRepository.findNullable(deleteRecordId))
        encounterConditionRepository.deleteById(deleteRecordId)
        assertNull(encounterConditionRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = EncounterConditionSpecification(internalName = "swarm")

        val results = encounterConditionRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "Swarm" })
    }

    @Test
    fun shouldReturnAllEncounterConditionsWhenNoCondition() {
        val results = encounterConditionRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "swarm" })
    }
}
