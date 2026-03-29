package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.ItemPocket
import io.github.lishangbu.avalon.dataset.entity.dto.ItemPocketSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemPocketView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemPocketInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemPocketInput
import io.github.lishangbu.avalon.dataset.repository.ItemPocketRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ItemPocketServiceImplTest {
    private val repository = mock(ItemPocketRepository::class.java)
    private val service = ItemPocketServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = ItemPocketSpecification(id = "1", internalName = "misc")
        `when`(repository.listViews(specification)).thenReturn(listOf(itemPocketView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("misc", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<ItemPocket>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            itemPocketEntity(1L),
        )

        val result = service.save(SaveItemPocketInput("misc", "道具"))

        assertEquals("1", result.id)
        verify(repository).save(any<ItemPocket>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<ItemPocket>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            itemPocketEntity(1L),
        )

        val result = service.update(UpdateItemPocketInput("1", "misc", "道具"))

        assertEquals("1", result.id)
        verify(repository).save(any<ItemPocket>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun itemPocketEntity(id: Long): ItemPocket =
    ItemPocket {
        this.id = id
        internalName = "misc"
        name = "道具"
    }

private fun itemPocketView(id: Long): ItemPocketView = ItemPocketView(itemPocketEntity(id))
