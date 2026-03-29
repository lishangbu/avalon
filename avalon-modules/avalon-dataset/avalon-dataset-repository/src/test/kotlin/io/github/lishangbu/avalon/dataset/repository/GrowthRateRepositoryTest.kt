package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

/** 成长速率仓储测试 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class GrowthRateRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var growthRateRepository: GrowthRateRepository

    @Test
    fun shouldInsertGrowthRateSuccessfully() {
        val growthRate =
            GrowthRate {
                internalName = "unit-test-growth-rate"
                name = "单元测试成长速率"
                description = "unit test growth rate"
            }

        val saved = growthRateRepository.save(growthRate, SaveMode.INSERT_ONLY)

        Assertions.assertNotNull(saved.id)
        Assertions.assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindGrowthRateById() {
        val slow = requireNotNull(growthRateRepository.findNullable(1L))

        Assertions.assertEquals(1L, slow.id)
        Assertions.assertEquals("slow", slow.internalName)
        Assertions.assertEquals("慢", slow.name)
        Assertions.assertEquals("slow", slow.description)
    }

    @Test
    fun shouldUpdateGrowthRateById() {
        val growthRate =
            growthRateRepository.save(
                GrowthRate {
                    internalName = "growth-rate-update"
                    name = "原始成长速率"
                    description = "original growth rate"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = growthRate.id

        growthRateRepository.save(
            GrowthRate(growthRate) {
                name = "更新后的成长速率"
                description = "updated growth rate"
            },
            SaveMode.UPSERT,
        )

        val updatedGrowthRate = requireNotNull(growthRateRepository.findNullable(id))
        Assertions.assertEquals("更新后的成长速率", updatedGrowthRate.name)
        Assertions.assertEquals("updated growth rate", updatedGrowthRate.description)
    }

    @Test
    fun shouldDeleteGrowthRateById() {
        val growthRate =
            growthRateRepository.save(
                GrowthRate {
                    internalName = "growth-rate-delete"
                    name = "待删除成长速率"
                    description = "delete growth rate"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = growthRate.id

        Assertions.assertNotNull(growthRateRepository.findNullable(deleteRecordId))
        growthRateRepository.deleteById(deleteRecordId)
        Assertions.assertNull(growthRateRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = GrowthRateSpecification(internalName = "medium")

        val results = growthRateRepository.findAll(condition)

        Assertions.assertNotNull(results)
        Assertions.assertTrue(results.any { it.internalName == "medium" })
    }

    @Test
    fun shouldReturnAllGrowthRatesWhenNoCondition() {
        val results = growthRateRepository.findAll()

        Assertions.assertNotNull(results)
        Assertions.assertTrue(results.size >= 3)
        Assertions.assertTrue(results.any { it.internalName == "medium" })
    }
}
