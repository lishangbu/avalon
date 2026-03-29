package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.ItemCategory
import io.github.lishangbu.avalon.dataset.entity.ItemPocket
import io.github.lishangbu.avalon.dataset.entity.dto.ItemCategorySpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemCategoryRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var itemCategoryRepository: ItemCategoryRepository

    @Test
    fun shouldInsertItemCategorySuccessfully() {
        val itemCategory =
            ItemCategory {
                internalName = "unit-test-item-category"
                name = "单元测试类别"
                itemPocket =
                    ItemPocket {
                        id = 1L
                    }
            }

        val saved = itemCategoryRepository.save(itemCategory, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindItemCategoryById() {
        val itemCategory = requireNotNull(itemCategoryRepository.loadViewById(1L))

        assertEquals("1", itemCategory.id)
        assertEquals("stat-boosts", itemCategory.internalName)
        assertEquals("Stat boosts", itemCategory.name)
        assertEquals("7", itemCategory.itemPocket?.id)
    }

    @Test
    fun shouldUpdateItemCategoryById() {
        val itemCategory =
            itemCategoryRepository.save(
                ItemCategory {
                    internalName = "item-category-update"
                    name = "原始类别"
                    itemPocket =
                        ItemPocket {
                            id = 1L
                        }
                },
                SaveMode.INSERT_ONLY,
            )
        val id = itemCategory.id

        itemCategoryRepository.save(
            ItemCategory(itemCategory) {
                name = "更新后的类别"
            },
            SaveMode.UPSERT,
        )

        val updatedItemCategory = requireNotNull(itemCategoryRepository.findNullable(id))
        assertEquals("更新后的类别", updatedItemCategory.name)
    }

    @Test
    fun shouldDeleteItemCategoryById() {
        val itemCategory =
            itemCategoryRepository.save(
                ItemCategory {
                    internalName = "item-category-delete"
                    name = "待删除类别"
                    itemPocket =
                        ItemPocket {
                            id = 1L
                        }
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = itemCategory.id

        assertNotNull(itemCategoryRepository.findNullable(deleteRecordId))
        itemCategoryRepository.deleteById(deleteRecordId)
        assertNull(itemCategoryRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = ItemCategorySpecification(itemPocketId = "7")

        val results = itemCategoryRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.itemPocket?.id == "7" })
    }

    @Test
    fun shouldReturnAllItemCategoriesWhenNoCondition() {
        val results = itemCategoryRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "stat-boosts" })
    }
}
