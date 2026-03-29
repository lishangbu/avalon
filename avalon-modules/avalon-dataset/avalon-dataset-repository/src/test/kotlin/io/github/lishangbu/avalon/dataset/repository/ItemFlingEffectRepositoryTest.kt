package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.ItemFlingEffect
import io.github.lishangbu.avalon.dataset.entity.dto.ItemFlingEffectSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemFlingEffectRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var itemFlingEffectRepository: ItemFlingEffectRepository

    @Test
    fun shouldInsertItemFlingEffectSuccessfully() {
        val itemFlingEffect =
            ItemFlingEffect {
                internalName = "unit-test-item-fling-effect"
                name = "单元测试投掷效果"
                effect = "测试效果"
            }

        val saved = itemFlingEffectRepository.save(itemFlingEffect, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindItemFlingEffectById() {
        val itemFlingEffect = requireNotNull(itemFlingEffectRepository.loadViewById(1L))

        assertEquals("1", itemFlingEffect.id)
        assertEquals("badly-poison", itemFlingEffect.internalName)
        assertEquals("Badly poisons the target.", itemFlingEffect.effect)
    }

    @Test
    fun shouldUpdateItemFlingEffectById() {
        val itemFlingEffect =
            itemFlingEffectRepository.save(
                ItemFlingEffect {
                    internalName = "item-fling-effect-update"
                    name = "原始投掷效果"
                    effect = "原始效果"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = itemFlingEffect.id

        itemFlingEffectRepository.save(
            ItemFlingEffect(itemFlingEffect) {
                effect = "更新后的效果"
            },
            SaveMode.UPSERT,
        )

        val updatedItemFlingEffect = requireNotNull(itemFlingEffectRepository.findNullable(id))
        assertEquals("更新后的效果", updatedItemFlingEffect.effect)
    }

    @Test
    fun shouldDeleteItemFlingEffectById() {
        val itemFlingEffect =
            itemFlingEffectRepository.save(
                ItemFlingEffect {
                    internalName = "item-fling-effect-delete"
                    name = "待删除投掷效果"
                    effect = "待删除效果"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = itemFlingEffect.id

        assertNotNull(itemFlingEffectRepository.findNullable(deleteRecordId))
        itemFlingEffectRepository.deleteById(deleteRecordId)
        assertNull(itemFlingEffectRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = ItemFlingEffectSpecification(internalName = "badly-poison")

        val results = itemFlingEffectRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.effect?.contains("poisons") == true })
    }

    @Test
    fun shouldReturnAllItemFlingEffectsWhenNoCondition() {
        val results = itemFlingEffectRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "badly-poison" })
    }
}
