package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.ItemCategory
import io.github.lishangbu.avalon.dataset.entity.ItemPocket
import io.github.lishangbu.avalon.dataset.entity.dto.ItemCategorySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemCategoryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemCategoryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemCategoryInput
import io.github.lishangbu.avalon.dataset.repository.ItemCategoryRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ItemCategoryServiceImplTest {
    private val repository = mock(ItemCategoryRepository::class.java)
    private val service = ItemCategoryServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = ItemCategorySpecification(id = "1", internalName = "stat-boosts")
        `when`(repository.listViews(specification)).thenReturn(listOf(itemCategoryView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("7", result.first().itemPocket?.id)
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<ItemCategory>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            itemCategorySavedEntity(1L),
        )
        `when`(repository.loadViewById(1L)).thenReturn(itemCategoryView(1L))

        val result = service.save(SaveItemCategoryInput("stat-boosts", "Stat boosts", "7"))

        assertEquals("1", result.id)
        assertEquals("道具", result.itemPocket?.name)
        verify(repository).save(any<ItemCategory>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.save(any<ItemCategory>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            itemCategorySavedEntity(1L),
        )
        `when`(repository.loadViewById(1L)).thenReturn(itemCategoryView(1L))

        val result = service.update(UpdateItemCategoryInput("1", "stat-boosts", "Stat boosts", "7"))

        assertEquals("1", result.id)
        assertEquals("misc", result.itemPocket?.internalName)
        verify(repository).save(any<ItemCategory>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun itemCategorySavedEntity(id: Long): ItemCategory =
    ItemCategory {
        this.id = id
        internalName = "stat-boosts"
        name = "Stat boosts"
        itemPocket =
            ItemPocket {
                this.id = 7L
            }
    }

private fun itemCategoryWithAssociation(id: Long): ItemCategory =
    ItemCategory {
        this.id = id
        internalName = "stat-boosts"
        name = "Stat boosts"
        itemPocket =
            ItemPocket {
                this.id = 7L
                internalName = "misc"
                name = "道具"
            }
    }

private fun itemCategoryView(id: Long): ItemCategoryView = ItemCategoryView(itemCategoryWithAssociation(id))
