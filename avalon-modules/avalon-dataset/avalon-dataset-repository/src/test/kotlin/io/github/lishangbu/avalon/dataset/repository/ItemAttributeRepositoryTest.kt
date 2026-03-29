package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.ItemAttribute
import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemAttributeRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var itemAttributeRepository: ItemAttributeRepository

    @Test
    fun shouldInsertItemAttributeSuccessfully() {
        val itemAttribute =
            ItemAttribute {
                internalName = "unit-test-item-attribute"
                name = "单元测试属性"
                description = "测试描述"
            }

        val saved = itemAttributeRepository.save(itemAttribute, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindItemAttributeById() {
        val itemAttribute = requireNotNull(itemAttributeRepository.loadViewById(1L))

        assertEquals("1", itemAttribute.id)
        assertEquals("countable", itemAttribute.internalName)
        assertEquals("Has a count in the bag", itemAttribute.description)
    }

    @Test
    fun shouldUpdateItemAttributeById() {
        val itemAttribute =
            itemAttributeRepository.save(
                ItemAttribute {
                    internalName = "item-attribute-update"
                    name = "原始属性"
                    description = "原始描述"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = itemAttribute.id

        itemAttributeRepository.save(
            ItemAttribute(itemAttribute) {
                description = "更新后的描述"
            },
            SaveMode.UPSERT,
        )

        val updatedItemAttribute = requireNotNull(itemAttributeRepository.findNullable(id))
        assertEquals("更新后的描述", updatedItemAttribute.description)
    }

    @Test
    fun shouldDeleteItemAttributeById() {
        val itemAttribute =
            itemAttributeRepository.save(
                ItemAttribute {
                    internalName = "item-attribute-delete"
                    name = "待删除属性"
                    description = "待删除描述"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = itemAttribute.id

        assertNotNull(itemAttributeRepository.findNullable(deleteRecordId))
        itemAttributeRepository.deleteById(deleteRecordId)
        assertNull(itemAttributeRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = ItemAttributeSpecification(internalName = "countable")

        val results = itemAttributeRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "Countable" })
    }

    @Test
    fun shouldReturnAllItemAttributesWhenNoCondition() {
        val results = itemAttributeRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "countable" })
    }
}
