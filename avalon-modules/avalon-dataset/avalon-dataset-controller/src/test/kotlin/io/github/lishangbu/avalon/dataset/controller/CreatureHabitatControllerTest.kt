package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureHabitatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureHabitatView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureHabitatInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureHabitatInput
import io.github.lishangbu.avalon.dataset.service.CreatureHabitatService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class CreatureHabitatControllerTest {
    @Test
    fun listCreatureHabitats_delegatesToService() {
        val service = FakeCreatureHabitatService()
        val controller = CreatureHabitatController(service)
        val list = listOf(CreatureHabitatView("1", "cave", "cave"))
        service.listResult = list
        val specification = CreatureHabitatSpecification(id = "1", internalName = "cave", name = "cave")

        val result = controller.listCreatureHabitats(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeCreatureHabitatService()
        val controller = CreatureHabitatController(service)
        val command = SaveCreatureHabitatInput("cave", "cave")
        service.saveResult = CreatureHabitatView("1", "cave", "cave")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeCreatureHabitatService()
        val controller = CreatureHabitatController(service)
        val command = UpdateCreatureHabitatInput("1", "cave", "cave")
        service.updateResult = CreatureHabitatView("1", "cave", "cave")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeCreatureHabitatService()
        val controller = CreatureHabitatController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeCreatureHabitatService : CreatureHabitatService {
        var listCondition: CreatureHabitatSpecification? = null
        var savedCommand: SaveCreatureHabitatInput? = null
        var updatedCommand: UpdateCreatureHabitatInput? = null
        var removedId: Long? = null

        var listResult: List<CreatureHabitatView> = emptyList()
        lateinit var saveResult: CreatureHabitatView
        lateinit var updateResult: CreatureHabitatView

        override fun save(command: SaveCreatureHabitatInput): CreatureHabitatView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateCreatureHabitatInput): CreatureHabitatView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: CreatureHabitatSpecification): List<CreatureHabitatView> {
            listCondition = specification
            return listResult
        }
    }
}
