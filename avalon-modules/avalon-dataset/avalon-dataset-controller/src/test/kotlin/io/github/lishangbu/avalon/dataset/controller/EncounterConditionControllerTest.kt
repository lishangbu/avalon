package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterConditionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterConditionInput
import io.github.lishangbu.avalon.dataset.service.EncounterConditionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class EncounterConditionControllerTest {
    @Test
    fun listEncounterConditions_delegatesToService() {
        val service = FakeEncounterConditionService()
        val controller = EncounterConditionController(service)
        val list = listOf(EncounterConditionView("1", "swarm", "Swarm"))
        service.listResult = list
        val specification = EncounterConditionSpecification(id = "1", internalName = "swarm", name = "Swarm")

        val result = controller.listEncounterConditions(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeEncounterConditionService()
        val controller = EncounterConditionController(service)
        val command = SaveEncounterConditionInput("swarm", "Swarm")
        service.saveResult = EncounterConditionView("1", "swarm", "Swarm")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeEncounterConditionService()
        val controller = EncounterConditionController(service)
        val command = UpdateEncounterConditionInput("1", "swarm", "Swarm")
        service.updateResult = EncounterConditionView("1", "swarm", "Swarm")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeEncounterConditionService()
        val controller = EncounterConditionController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeEncounterConditionService : EncounterConditionService {
        var listCondition: EncounterConditionSpecification? = null
        var savedCommand: SaveEncounterConditionInput? = null
        var updatedCommand: UpdateEncounterConditionInput? = null
        var removedId: Long? = null

        var listResult: List<EncounterConditionView> = emptyList()
        lateinit var saveResult: EncounterConditionView
        lateinit var updateResult: EncounterConditionView

        override fun save(command: SaveEncounterConditionInput): EncounterConditionView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateEncounterConditionInput): EncounterConditionView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: EncounterConditionSpecification): List<EncounterConditionView> {
            listCondition = specification
            return listResult
        }
    }
}
