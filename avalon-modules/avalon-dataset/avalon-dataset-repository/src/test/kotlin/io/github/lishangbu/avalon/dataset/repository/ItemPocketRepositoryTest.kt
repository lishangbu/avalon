package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.ItemPocket
import io.github.lishangbu.avalon.dataset.entity.dto.ItemPocketSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemPocketRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var itemPocketRepository: ItemPocketRepository

    @Test
    fun shouldInsertItemPocketSuccessfully() {
        val itemPocket =
            ItemPocket {
                internalName = "unit-test-item-pocket"
                name = "单元测试口袋"
            }

        val saved = itemPocketRepository.save(itemPocket, SaveMode.INSERT_ONLY)

        assertTrue(saved.id > 0)
    }

    @Test
    fun shouldFindItemPocketById() {
        val itemPocket = requireNotNull(itemPocketRepository.loadViewById(1L))

        assertEquals("1", itemPocket.id)
        assertEquals("misc", itemPocket.internalName)
        assertEquals("道具", itemPocket.name)
    }

    @Test
    fun shouldUpdateItemPocketById() {
        val itemPocket =
            itemPocketRepository.save(
                ItemPocket {
                    internalName = "item-pocket-update"
                    name = "原始口袋"
                },
                SaveMode.INSERT_ONLY,
            )
        val id = itemPocket.id

        itemPocketRepository.save(
            ItemPocket(itemPocket) {
                name = "更新后的口袋"
            },
            SaveMode.UPSERT,
        )

        val updatedItemPocket = requireNotNull(itemPocketRepository.findNullable(id))
        assertEquals("更新后的口袋", updatedItemPocket.name)
    }

    @Test
    fun shouldDeleteItemPocketById() {
        val itemPocket =
            itemPocketRepository.save(
                ItemPocket {
                    internalName = "item-pocket-delete"
                    name = "待删除口袋"
                },
                SaveMode.INSERT_ONLY,
            )
        val deleteRecordId = itemPocket.id

        assertNotNull(itemPocketRepository.findNullable(deleteRecordId))
        itemPocketRepository.deleteById(deleteRecordId)
        assertNull(itemPocketRepository.findNullable(deleteRecordId))
    }

    @Test
    fun shouldSelectListWithDynamicCondition() {
        val condition = ItemPocketSpecification(internalName = "misc")

        val results = itemPocketRepository.listViews(condition)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "道具" })
    }

    @Test
    fun shouldReturnAllItemPocketsWhenNoCondition() {
        val results = itemPocketRepository.listViews(null)

        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.internalName == "misc" })
    }
}
