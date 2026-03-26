package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryFlavorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryFlavorInput
import io.github.lishangbu.avalon.dataset.service.BerryFlavorService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class BerryFlavorControllerTest {
    @Test
    fun listBerryFlavors_delegatesToService() {
        val service = FakeBerryFlavorService()
        val controller = BerryFlavorController(service)
        val list = listOf(BerryFlavorView("1", "spicy", "辣"))
        service.listResult = list
        val specification = BerryFlavorSpecification(id = "1", internalName = "spicy", name = "辣")

        val result = controller.listBerryFlavors(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeBerryFlavorService()
        val controller = BerryFlavorController(service)
        val command = SaveBerryFlavorInput("spicy", "辣")
        service.saveResult = BerryFlavorView("1", "spicy", "辣")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeBerryFlavorService()
        val controller = BerryFlavorController(service)
        val command = UpdateBerryFlavorInput("1", "spicy", "辣")
        service.updateResult = BerryFlavorView("1", "spicy", "辣")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeBerryFlavorService()
        val controller = BerryFlavorController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeBerryFlavorService : BerryFlavorService {
        var listCondition: BerryFlavorSpecification? = null
        var savedCommand: SaveBerryFlavorInput? = null
        var updatedCommand: UpdateBerryFlavorInput? = null
        var removedId: Long? = null

        var listResult: List<BerryFlavorView> = emptyList()
        lateinit var saveResult: BerryFlavorView
        lateinit var updateResult: BerryFlavorView

        override fun save(command: SaveBerryFlavorInput): BerryFlavorView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateBerryFlavorInput): BerryFlavorView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: BerryFlavorSpecification): List<BerryFlavorView> {
            listCondition = specification
            return listResult
        }
    }
}
