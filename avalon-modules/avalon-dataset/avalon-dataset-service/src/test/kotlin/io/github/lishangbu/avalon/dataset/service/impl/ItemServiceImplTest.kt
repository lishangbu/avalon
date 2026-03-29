package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Item
import io.github.lishangbu.avalon.dataset.entity.ItemAttribute
import io.github.lishangbu.avalon.dataset.entity.ItemFlingEffect
import io.github.lishangbu.avalon.dataset.entity.dto.ItemSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemInput
import io.github.lishangbu.avalon.dataset.repository.ItemRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class ItemServiceImplTest {
    private val repository = mock(ItemRepository::class.java)
    private val service = ItemServiceImpl(repository)

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = ItemSpecification(id = "1", internalName = "master-ball")
        val pageable = PageRequest.of(0, 5)
        `when`(repository.pageViews(specification, pageable)).thenReturn(Page(listOf(itemView(1L)), 1, 1))

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals("1", result.rows.first().id)
        assertEquals("大师球", result.rows.first().name)
        assertEquals(
            "Countable",
            result.rows
                .first()
                .itemAttributes
                .first()
                .name,
        )
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<Item>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            itemSavedEntity(1L),
        )
        `when`(repository.loadViewById(1L)).thenReturn(itemView(1L))

        val result =
            service.save(
                SaveItemInput(
                    internalName = "master-ball",
                    name = "大师球",
                    cost = 0,
                    flingPower = 30,
                    shortEffect = "Catches a wild Pokémon every time.",
                    effect = "必定捕捉成功",
                    text = "性能最好的球。",
                    itemFlingEffectId = "3",
                    itemAttributeIds = listOf("1", "2"),
                ),
            )

        assertEquals("1", result.id)
        assertEquals("berry-effect", result.itemFlingEffect?.internalName)
        assertEquals(2, result.itemAttributes.size)
        verify(repository).save(any<Item>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.save(any<Item>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            itemSavedEntity(1L),
        )
        `when`(repository.loadViewById(1L)).thenReturn(itemView(1L))

        val result =
            service.update(
                UpdateItemInput(
                    id = "1",
                    internalName = "master-ball",
                    name = "大师球",
                    cost = 0,
                    flingPower = 30,
                    shortEffect = "Catches a wild Pokémon every time.",
                    effect = "必定捕捉成功",
                    text = "性能最好的球。",
                    itemFlingEffectId = "3",
                    itemAttributeIds = listOf("1", "2"),
                ),
            )

        assertEquals("1", result.id)
        assertEquals("Immediately activates the berry’s effect on the target.", result.itemFlingEffect?.name)
        assertEquals(setOf("1", "2"), result.itemAttributes.map { it.id }.toSet())
        verify(repository).save(any<Item>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun itemSavedEntity(id: Long): Item =
    Item {
        this.id = id
        internalName = "master-ball"
        name = "大师球"
        cost = 0
        flingPower = 30
        shortEffect = "Catches a wild Pokémon every time."
        effect = "必定捕捉成功"
        text = "性能最好的球。"
        itemFlingEffect =
            ItemFlingEffect {
                this.id = 3L
            }
        itemAttributes =
            listOf(
                ItemAttribute {
                    this.id = 1L
                },
                ItemAttribute {
                    this.id = 2L
                },
            )
    }

private fun itemWithAssociations(id: Long): Item =
    Item {
        this.id = id
        internalName = "master-ball"
        name = "大师球"
        cost = 0
        flingPower = 30
        shortEffect = "Catches a wild Pokémon every time."
        effect = "必定捕捉成功"
        text = "性能最好的球。"
        itemFlingEffect =
            ItemFlingEffect {
                this.id = 3L
                internalName = "berry-effect"
                name = "Immediately activates the berry’s effect on the target."
            }
        itemAttributes =
            listOf(
                ItemAttribute {
                    this.id = 1L
                    internalName = "countable"
                    name = "Countable"
                },
                ItemAttribute {
                    this.id = 2L
                    internalName = "consumable"
                    name = "Consumable"
                },
            )
    }

private fun itemView(id: Long): ItemView = ItemView(itemWithAssociations(id))
