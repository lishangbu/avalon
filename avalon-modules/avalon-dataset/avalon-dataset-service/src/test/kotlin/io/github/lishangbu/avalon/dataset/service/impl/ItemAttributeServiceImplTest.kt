package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.ItemAttribute
import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemAttributeInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemAttributeInput
import io.github.lishangbu.avalon.dataset.repository.ItemAttributeRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ItemAttributeServiceImplTest {
    private val repository = mock(ItemAttributeRepository::class.java)
    private val service = ItemAttributeServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = ItemAttributeSpecification(id = "1", internalName = "countable")
        `when`(repository.listViews(specification)).thenReturn(listOf(itemAttributeView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("countable", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<ItemAttribute>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            itemAttributeEntity(1L),
        )

        val result = service.save(SaveItemAttributeInput("countable", "Countable", "Has a count in the bag"))

        assertEquals("1", result.id)
        verify(repository).save(any<ItemAttribute>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<ItemAttribute>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            itemAttributeEntity(1L),
        )

        val result = service.update(UpdateItemAttributeInput("1", "countable", "Countable", "Has a count in the bag"))

        assertEquals("1", result.id)
        verify(repository).save(any<ItemAttribute>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun itemAttributeEntity(id: Long): ItemAttribute =
    ItemAttribute {
        this.id = id
        internalName = "countable"
        name = "Countable"
        description = "Has a count in the bag"
    }

private fun itemAttributeView(id: Long): ItemAttributeView = ItemAttributeView(itemAttributeEntity(id))
