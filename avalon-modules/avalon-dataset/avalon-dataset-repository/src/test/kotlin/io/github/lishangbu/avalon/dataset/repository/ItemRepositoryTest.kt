package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Item
import io.github.lishangbu.avalon.dataset.entity.dto.ItemSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@Transactional
class ItemRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var itemRepository: ItemRepository

    @Test
    fun shouldQueryPageAndCrudItem() {
        val condition = ItemSpecification(internalName = "master-ball")

        val results = itemRepository.listViews(condition)
        val page = itemRepository.pageViews(condition, PageRequest.of(0, 10))

        assertFalse(results.isEmpty())
        assertEquals("1", results.first().id)
        assertEquals("master-ball", results.first().internalName)
        assertFalse(results.first().itemAttributes.isEmpty())
        assertTrue(page.totalRowCount >= 1)
        assertFalse(page.rows.isEmpty())

        val existing = requireNotNull(itemRepository.findNullable(1L))
        itemRepository.save(Item(existing) { name = "更新后的道具" }, SaveMode.UPSERT)

        val updated = requireNotNull(itemRepository.loadViewById(1L))
        assertEquals("更新后的道具", updated.name)

        itemRepository.deleteById(1L)
        assertNull(itemRepository.loadViewById(1L))
    }

    private fun assertTrue(value: Boolean) {
        assertEquals(true, value)
    }
}
