package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Item
import io.github.lishangbu.avalon.dataset.entity.ItemAttribute
import io.github.lishangbu.avalon.dataset.entity.ItemFlingEffect
import io.github.lishangbu.avalon.dataset.entity.dto.ItemSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemInput
import io.github.lishangbu.avalon.dataset.service.ItemService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class ItemControllerTest {
    @Test
    fun getItemPage_delegatesToService() {
        val service = FakeItemService()
        val controller = ItemController(service)
        val pageable = PageRequest.of(0, 5)
        val page: Page<ItemView> = Page(listOf(itemView()), 1, 1)
        service.pageResult = page
        val specification =
            ItemSpecification(
                id = "1",
                internalName = "master-ball",
                name = "大师球",
                cost = 0,
                flingPower = 30,
                shortEffect = "catch",
                effect = "捕捉",
                text = "球",
                itemFlingEffectId = "3",
            )

        val result = controller.getItemPage(pageable = pageable, specification = specification)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("1", service.pageCondition!!.id)
        assertEquals("3", service.pageCondition!!.itemFlingEffectId)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeItemService()
        val controller = ItemController(service)
        val command =
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
            )
        service.saveResult = itemView()

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeItemService()
        val controller = ItemController(service)
        val command =
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
            )
        service.updateResult = itemView()

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeItemService()
        val controller = ItemController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeItemService : ItemService {
        var pageCondition: ItemSpecification? = null
        var pageable: Pageable? = null
        var savedCommand: SaveItemInput? = null
        var updatedCommand: UpdateItemInput? = null
        var removedId: Long? = null

        var pageResult: Page<ItemView> = Page(emptyList(), 0, 0)
        lateinit var saveResult: ItemView
        lateinit var updateResult: ItemView

        override fun getPageByCondition(
            specification: ItemSpecification,
            pageable: Pageable,
        ): Page<ItemView> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(command: SaveItemInput): ItemView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateItemInput): ItemView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }
    }
}

private fun itemView(): ItemView =
    ItemView(
        Item {
            id = 1L
            internalName = "master-ball"
            name = "大师球"
            cost = 0
            flingPower = 30
            shortEffect = "Catches a wild Pokémon every time."
            effect = "必定捕捉成功"
            text = "性能最好的球。"
            itemFlingEffect =
                ItemFlingEffect {
                    id = 3L
                    internalName = "berry-effect"
                    name = "Immediately activates the berry’s effect on the target."
                }
            itemAttributes =
                listOf(
                    ItemAttribute {
                        id = 1L
                        internalName = "countable"
                        name = "Countable"
                    },
                    ItemAttribute {
                        id = 2L
                        internalName = "consumable"
                        name = "Consumable"
                    },
                )
        },
    )
