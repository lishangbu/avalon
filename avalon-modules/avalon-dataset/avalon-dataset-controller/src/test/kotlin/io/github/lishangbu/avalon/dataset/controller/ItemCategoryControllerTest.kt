package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.ItemCategory
import io.github.lishangbu.avalon.dataset.entity.ItemPocket
import io.github.lishangbu.avalon.dataset.entity.dto.ItemCategorySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemCategoryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemCategoryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemCategoryInput
import io.github.lishangbu.avalon.dataset.service.ItemCategoryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ItemCategoryControllerTest {
    @Test
    fun listItemCategories_delegatesToService() {
        val service = FakeItemCategoryService()
        val controller = ItemCategoryController(service)
        val list = listOf(itemCategoryView())
        service.listResult = list
        val specification = ItemCategorySpecification(id = "1", internalName = "stat-boosts", itemPocketId = "7")

        val result = controller.listItemCategories(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeItemCategoryService()
        val controller = ItemCategoryController(service)
        val command = SaveItemCategoryInput("stat-boosts", "Stat boosts", "7")
        service.saveResult = itemCategoryView()

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeItemCategoryService()
        val controller = ItemCategoryController(service)
        val command = UpdateItemCategoryInput("1", "stat-boosts", "Stat boosts", "7")
        service.updateResult = itemCategoryView()

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeItemCategoryService()
        val controller = ItemCategoryController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeItemCategoryService : ItemCategoryService {
        var listCondition: ItemCategorySpecification? = null
        var savedCommand: SaveItemCategoryInput? = null
        var updatedCommand: UpdateItemCategoryInput? = null
        var removedId: Long? = null

        var listResult: List<ItemCategoryView> = emptyList()
        lateinit var saveResult: ItemCategoryView
        lateinit var updateResult: ItemCategoryView

        override fun save(command: SaveItemCategoryInput): ItemCategoryView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateItemCategoryInput): ItemCategoryView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: ItemCategorySpecification): List<ItemCategoryView> {
            listCondition = specification
            return listResult
        }
    }

    private fun itemCategoryView(): ItemCategoryView =
        ItemCategoryView(
            ItemCategory {
                id = 1L
                internalName = "stat-boosts"
                name = "Stat boosts"
                itemPocket =
                    ItemPocket {
                        id = 7L
                        internalName = "misc"
                        name = "道具"
                    }
            },
        )
}
