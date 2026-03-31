package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureColorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureColorView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureColorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureColorInput
import io.github.lishangbu.avalon.dataset.service.CreatureColorService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class CreatureColorControllerTest {
    @Test
    fun listCreatureColors_delegatesToService() {
        val service = FakeCreatureColorService()
        val controller = CreatureColorController(service)
        val list = listOf(CreatureColorView("1", "black", "黑色"))
        service.listResult = list
        val specification = CreatureColorSpecification(id = "1", internalName = "black", name = "黑色")

        val result = controller.listCreatureColors(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeCreatureColorService()
        val controller = CreatureColorController(service)
        val command = SaveCreatureColorInput("black", "黑色")
        service.saveResult = CreatureColorView("1", "black", "黑色")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeCreatureColorService()
        val controller = CreatureColorController(service)
        val command = UpdateCreatureColorInput("1", "black", "黑色")
        service.updateResult = CreatureColorView("1", "black", "黑色")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeCreatureColorService()
        val controller = CreatureColorController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeCreatureColorService : CreatureColorService {
        var listCondition: CreatureColorSpecification? = null
        var savedCommand: SaveCreatureColorInput? = null
        var updatedCommand: UpdateCreatureColorInput? = null
        var removedId: Long? = null

        var listResult: List<CreatureColorView> = emptyList()
        lateinit var saveResult: CreatureColorView
        lateinit var updateResult: CreatureColorView

        override fun save(command: SaveCreatureColorInput): CreatureColorView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateCreatureColorInput): CreatureColorView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: CreatureColorSpecification): List<CreatureColorView> {
            listCondition = specification
            return listResult
        }
    }
}
