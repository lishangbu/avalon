package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.ItemFlingEffectSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemFlingEffectView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveItemFlingEffectInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateItemFlingEffectInput
import io.github.lishangbu.avalon.dataset.service.ItemFlingEffectService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ItemFlingEffectControllerTest {
    @Test
    fun listItemFlingEffects_delegatesToService() {
        val service = FakeItemFlingEffectService()
        val controller = ItemFlingEffectController(service)
        val list = listOf(ItemFlingEffectView("1", "badly-poison", "badly-poison", "Badly poisons the target."))
        service.listResult = list
        val specification = ItemFlingEffectSpecification(id = "1", internalName = "badly-poison", name = "badly-poison")

        val result = controller.listItemFlingEffects(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeItemFlingEffectService()
        val controller = ItemFlingEffectController(service)
        val command = SaveItemFlingEffectInput("badly-poison", "badly-poison", "Badly poisons the target.")
        service.saveResult = ItemFlingEffectView("1", "badly-poison", "badly-poison", "Badly poisons the target.")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeItemFlingEffectService()
        val controller = ItemFlingEffectController(service)
        val command = UpdateItemFlingEffectInput("1", "badly-poison", "badly-poison", "Badly poisons the target.")
        service.updateResult = ItemFlingEffectView("1", "badly-poison", "badly-poison", "Badly poisons the target.")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeItemFlingEffectService()
        val controller = ItemFlingEffectController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeItemFlingEffectService : ItemFlingEffectService {
        var listCondition: ItemFlingEffectSpecification? = null
        var savedCommand: SaveItemFlingEffectInput? = null
        var updatedCommand: UpdateItemFlingEffectInput? = null
        var removedId: Long? = null

        var listResult: List<ItemFlingEffectView> = emptyList()
        lateinit var saveResult: ItemFlingEffectView
        lateinit var updateResult: ItemFlingEffectView

        override fun save(command: SaveItemFlingEffectInput): ItemFlingEffectView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateItemFlingEffectInput): ItemFlingEffectView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: ItemFlingEffectSpecification): List<ItemFlingEffectView> {
            listCondition = specification
            return listResult
        }
    }
}
