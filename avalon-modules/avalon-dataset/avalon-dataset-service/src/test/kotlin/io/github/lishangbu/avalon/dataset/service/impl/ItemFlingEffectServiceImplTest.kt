package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.ItemFlingEffect
import io.github.lishangbu.avalon.dataset.entity.dto.ItemFlingEffectSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemFlingEffectView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemFlingEffectInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemFlingEffectInput
import io.github.lishangbu.avalon.dataset.repository.ItemFlingEffectRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ItemFlingEffectServiceImplTest {
    private val repository = mock(ItemFlingEffectRepository::class.java)
    private val service = ItemFlingEffectServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = ItemFlingEffectSpecification(id = "1", internalName = "badly-poison")
        `when`(repository.listViews(specification)).thenReturn(listOf(itemFlingEffectView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("badly-poison", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<ItemFlingEffect>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            itemFlingEffectEntity(1L),
        )

        val result = service.save(SaveItemFlingEffectInput("badly-poison", "badly-poison", "Badly poisons the target."))

        assertEquals("1", result.id)
        verify(repository).save(any<ItemFlingEffect>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<ItemFlingEffect>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            itemFlingEffectEntity(1L),
        )

        val result = service.update(UpdateItemFlingEffectInput("1", "badly-poison", "badly-poison", "Badly poisons the target."))

        assertEquals("1", result.id)
        verify(repository).save(any<ItemFlingEffect>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun itemFlingEffectEntity(id: Long): ItemFlingEffect =
    ItemFlingEffect {
        this.id = id
        internalName = "badly-poison"
        name = "badly-poison"
        effect = "Badly poisons the target."
    }

private fun itemFlingEffectView(id: Long): ItemFlingEffectView = ItemFlingEffectView(itemFlingEffectEntity(id))
