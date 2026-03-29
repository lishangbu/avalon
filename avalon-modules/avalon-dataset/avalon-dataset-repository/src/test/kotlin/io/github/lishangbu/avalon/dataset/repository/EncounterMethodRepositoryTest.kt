package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.EncounterMethod
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterMethodSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EncounterMethodRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var encounterMethodRepository: EncounterMethodRepository

    @Test
    fun shouldInsertEncounterMethodSuccessfully() {
        val encounterMethod =
            EncounterMethod {
                internalName = "unit-test-encounter-method"
                name = "单元测试遭遇方式"
                sortingOrder = 99
            }

        val saved = encounterMethodRepository.save(encounterMethod, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindEncounterMethodById() {
        val encounterMethod = requireNotNull(encounterMethodRepository.loadViewById(1L))

        assertEquals("1", encounterMethod.id)
        assertEquals("walk", encounterMethod.internalName)
        assertEquals("Walking in tall grass or a cave", encounterMethod.name)
        assertEquals(1, encounterMethod.sortingOrder)
    }

    @Test
    fun shouldUpdateEncounterMethodById() {
        val encounterMethod =
            encounterMethodRepository.save(
                EncounterMethod {
                    internalName = "encounter-method-update"
                    name = "原始遭遇方式"
                    sortingOrder = 20
                },
                SaveMode.INSERT_ONLY,
            )
        val id = encounterMethod.id

        encounterMethodRepository.save(
            EncounterMethod(encounterMethod) {
                name = "更新后的遭遇方式"
                sortingOrder = 21
            },
            SaveMode.UPSERT,
        )

        val updatedEncounterMethod = requireNotNull(encounterMethodRepository.findNullable(id))
        assertEquals("更新后的遭遇方式", updatedEncounterMethod.name)
        assertEquals(21, updatedEncounterMethod.sortingOrder)
    }

    @Test
    fun shouldDeleteEncounterMethodById() {
        val encounterMethod =
            encounterMethodRepository.save(
                EncounterMethod {
                    internalName = "encounter-method-delete"
                    name = "待删除遭遇方式"
                    sortingOrder = 88
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = encounterMethod.id

        assertNotNull(encounterMethodRepository.findNullable(deleteRecordId))
        encounterMethodRepository.deleteById(deleteRecordId)
        assertNull(encounterMethodRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = EncounterMethodSpecification(internalName = "walk", sortingOrder = 1)

        val results = encounterMethodRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "Walking in tall grass or a cave" })
    }

    @Test
    fun shouldReturnAllEncounterMethodsWhenNoCondition() {
        val results = encounterMethodRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "walk" })
    }
}
