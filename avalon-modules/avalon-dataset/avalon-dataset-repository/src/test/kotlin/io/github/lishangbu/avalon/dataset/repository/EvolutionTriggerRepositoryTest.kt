package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.EvolutionTrigger
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionTriggerSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EvolutionTriggerRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var evolutionTriggerRepository: EvolutionTriggerRepository

    @Test
    fun shouldInsertEvolutionTriggerSuccessfully() {
        val evolutionTrigger =
            EvolutionTrigger {
                internalName = "unit-test-evolution-trigger"
                name = "单元测试进化触发方式"
            }

        val saved = evolutionTriggerRepository.save(evolutionTrigger, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindEvolutionTriggerById() {
        val evolutionTrigger = requireNotNull(evolutionTriggerRepository.loadViewById(1L))

        assertEquals("1", evolutionTrigger.id)
        assertEquals("level-up", evolutionTrigger.internalName)
        assertEquals("Level up", evolutionTrigger.name)
    }

    @Test
    fun shouldUpdateEvolutionTriggerById() {
        val evolutionTrigger =
            evolutionTriggerRepository.save(
                EvolutionTrigger {
                    internalName = "evolution-trigger-update"
                    name = "原始进化触发方式"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = evolutionTrigger.id

        evolutionTriggerRepository.save(
            EvolutionTrigger(evolutionTrigger) {
                name = "更新后的进化触发方式"
            },
            SaveMode.UPSERT,
        )

        val updatedEvolutionTrigger = requireNotNull(evolutionTriggerRepository.findNullable(id))
        assertEquals("更新后的进化触发方式", updatedEvolutionTrigger.name)
    }

    @Test
    fun shouldDeleteEvolutionTriggerById() {
        val evolutionTrigger =
            evolutionTriggerRepository.save(
                EvolutionTrigger {
                    internalName = "evolution-trigger-delete"
                    name = "待删除进化触发方式"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = evolutionTrigger.id

        assertNotNull(evolutionTriggerRepository.findNullable(deleteRecordId))
        evolutionTriggerRepository.deleteById(deleteRecordId)
        assertNull(evolutionTriggerRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = EvolutionTriggerSpecification(internalName = "level-up")

        val results = evolutionTriggerRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "Level up" })
    }

    @Test
    fun shouldReturnAllEvolutionTriggersWhenNoCondition() {
        val results = evolutionTriggerRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "level-up" })
    }
}
