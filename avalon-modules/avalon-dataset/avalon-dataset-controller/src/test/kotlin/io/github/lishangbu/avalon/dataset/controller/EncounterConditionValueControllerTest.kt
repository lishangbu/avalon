package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionValueSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionValueView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterConditionValueInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterConditionValueInput
import io.github.lishangbu.avalon.dataset.service.EncounterConditionValueService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class EncounterConditionValueControllerTest {
    @Test
    fun listEncounterConditionValues_delegatesToService() {
        val service = FakeEncounterConditionValueService()
        val controller = EncounterConditionValueController(service)
        val list = listOf(encounterConditionValueView())
        service.listResult = list
        val specification = EncounterConditionValueSpecification(id = "1", internalName = "swarm-yes", encounterConditionId = "1")

        val result = controller.listEncounterConditionValues(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeEncounterConditionValueService()
        val controller = EncounterConditionValueController(service)
        val command =
            SaveEncounterConditionValueInput(
                internalName = "swarm-yes",
                name = "During a swarm",
                encounterConditionId = "1",
            )
        service.saveResult = encounterConditionValueView()

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeEncounterConditionValueService()
        val controller = EncounterConditionValueController(service)
        val command =
            UpdateEncounterConditionValueInput(
                id = "1",
                internalName = "swarm-yes",
                name = "During a swarm",
                encounterConditionId = "1",
            )
        service.updateResult = encounterConditionValueView()

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeEncounterConditionValueService()
        val controller = EncounterConditionValueController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeEncounterConditionValueService : EncounterConditionValueService {
        var listCondition: EncounterConditionValueSpecification? = null
        var savedCommand: SaveEncounterConditionValueInput? = null
        var updatedCommand: UpdateEncounterConditionValueInput? = null
        var removedId: Long? = null

        var listResult: List<EncounterConditionValueView> = emptyList()
        lateinit var saveResult: EncounterConditionValueView
        lateinit var updateResult: EncounterConditionValueView

        override fun save(command: SaveEncounterConditionValueInput): EncounterConditionValueView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateEncounterConditionValueInput): EncounterConditionValueView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: EncounterConditionValueSpecification): List<EncounterConditionValueView> {
            listCondition = specification
            return listResult
        }
    }
}

private fun encounterConditionValueView(): EncounterConditionValueView =
    EncounterConditionValueView(
        id = "1",
        internalName = "swarm-yes",
        name = "During a swarm",
        encounterCondition =
            EncounterConditionValueView.TargetOf_encounterCondition(
                id = "1",
                internalName = "swarm",
                name = "Swarm",
            ),
    )
