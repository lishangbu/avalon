package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.ItemPocketSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemPocketView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemPocketInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemPocketInput
import io.github.lishangbu.avalon.dataset.service.ItemPocketService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ItemPocketControllerTest {
    @Test
    fun listItemPockets_delegatesToService() {
        val service = FakeItemPocketService()
        val controller = ItemPocketController(service)
        val list = listOf(ItemPocketView("1", "misc", "道具"))
        service.listResult = list
        val specification = ItemPocketSpecification(id = "1", internalName = "misc", name = "道具")

        val result = controller.listItemPockets(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeItemPocketService()
        val controller = ItemPocketController(service)
        val command = SaveItemPocketInput("misc", "道具")
        service.saveResult = ItemPocketView("1", "misc", "道具")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeItemPocketService()
        val controller = ItemPocketController(service)
        val command = UpdateItemPocketInput("1", "misc", "道具")
        service.updateResult = ItemPocketView("1", "misc", "道具")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeItemPocketService()
        val controller = ItemPocketController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeItemPocketService : ItemPocketService {
        var listCondition: ItemPocketSpecification? = null
        var savedCommand: SaveItemPocketInput? = null
        var updatedCommand: UpdateItemPocketInput? = null
        var removedId: Long? = null

        var listResult: List<ItemPocketView> = emptyList()
        lateinit var saveResult: ItemPocketView
        lateinit var updateResult: ItemPocketView

        override fun save(command: SaveItemPocketInput): ItemPocketView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateItemPocketInput): ItemPocketView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: ItemPocketSpecification): List<ItemPocketView> {
            listCondition = specification
            return listResult
        }
    }
}
