package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionTriggerSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionTriggerView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEvolutionTriggerInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEvolutionTriggerInput
import io.github.lishangbu.avalon.dataset.service.EvolutionTriggerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class EvolutionTriggerControllerTest {
    @Test
    fun listEvolutionTriggers_delegatesToService() {
        val service = FakeEvolutionTriggerService()
        val controller = EvolutionTriggerController(service)
        val list = listOf(EvolutionTriggerView("1", "level-up", "Level up"))
        service.listResult = list
        val specification = EvolutionTriggerSpecification(id = "1", internalName = "level-up", name = "Level up")

        val result = controller.listEvolutionTriggers(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeEvolutionTriggerService()
        val controller = EvolutionTriggerController(service)
        val command = SaveEvolutionTriggerInput("level-up", "Level up")
        service.saveResult = EvolutionTriggerView("1", "level-up", "Level up")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeEvolutionTriggerService()
        val controller = EvolutionTriggerController(service)
        val command = UpdateEvolutionTriggerInput("1", "level-up", "Level up")
        service.updateResult = EvolutionTriggerView("1", "level-up", "Level up")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeEvolutionTriggerService()
        val controller = EvolutionTriggerController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeEvolutionTriggerService : EvolutionTriggerService {
        var listCondition: EvolutionTriggerSpecification? = null
        var savedCommand: SaveEvolutionTriggerInput? = null
        var updatedCommand: UpdateEvolutionTriggerInput? = null
        var removedId: Long? = null

        var listResult: List<EvolutionTriggerView> = emptyList()
        lateinit var saveResult: EvolutionTriggerView
        lateinit var updateResult: EvolutionTriggerView

        override fun save(command: SaveEvolutionTriggerInput): EvolutionTriggerView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateEvolutionTriggerInput): EvolutionTriggerView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: EvolutionTriggerSpecification): List<EvolutionTriggerView> {
            listCondition = specification
            return listResult
        }
    }
}
