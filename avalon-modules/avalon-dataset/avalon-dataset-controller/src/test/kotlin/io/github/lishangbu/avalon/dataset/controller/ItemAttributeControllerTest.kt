package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemAttributeInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemAttributeInput
import io.github.lishangbu.avalon.dataset.service.ItemAttributeService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ItemAttributeControllerTest {
    @Test
    fun listItemAttributes_delegatesToService() {
        val service = FakeItemAttributeService()
        val controller = ItemAttributeController(service)
        val list = listOf(ItemAttributeView("1", "countable", "Countable", "Has a count in the bag"))
        service.listResult = list
        val specification = ItemAttributeSpecification(id = "1", internalName = "countable", name = "Countable")

        val result = controller.listItemAttributes(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeItemAttributeService()
        val controller = ItemAttributeController(service)
        val command = SaveItemAttributeInput("countable", "Countable", "Has a count in the bag")
        service.saveResult = ItemAttributeView("1", "countable", "Countable", "Has a count in the bag")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeItemAttributeService()
        val controller = ItemAttributeController(service)
        val command = UpdateItemAttributeInput("1", "countable", "Countable", "Has a count in the bag")
        service.updateResult = ItemAttributeView("1", "countable", "Countable", "Has a count in the bag")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeItemAttributeService()
        val controller = ItemAttributeController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeItemAttributeService : ItemAttributeService {
        var listCondition: ItemAttributeSpecification? = null
        var savedCommand: SaveItemAttributeInput? = null
        var updatedCommand: UpdateItemAttributeInput? = null
        var removedId: Long? = null

        var listResult: List<ItemAttributeView> = emptyList()
        lateinit var saveResult: ItemAttributeView
        lateinit var updateResult: ItemAttributeView

        override fun save(command: SaveItemAttributeInput): ItemAttributeView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateItemAttributeInput): ItemAttributeView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: ItemAttributeSpecification): List<ItemAttributeView> {
            listCondition = specification
            return listResult
        }
    }
}
