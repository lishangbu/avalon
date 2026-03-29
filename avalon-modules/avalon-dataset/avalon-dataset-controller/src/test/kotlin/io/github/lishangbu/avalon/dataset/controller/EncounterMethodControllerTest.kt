package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.EncounterMethodSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterMethodView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterMethodInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterMethodInput
import io.github.lishangbu.avalon.dataset.service.EncounterMethodService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class EncounterMethodControllerTest {
    @Test
    fun listEncounterMethods_delegatesToService() {
        val service = FakeEncounterMethodService()
        val controller = EncounterMethodController(service)
        val list = listOf(EncounterMethodView("1", "walk", "Walking in tall grass or a cave", 1))
        service.listResult = list
        val specification = EncounterMethodSpecification(id = "1", internalName = "walk", sortingOrder = 1)

        val result = controller.listEncounterMethods(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeEncounterMethodService()
        val controller = EncounterMethodController(service)
        val command = SaveEncounterMethodInput("walk", "Walking in tall grass or a cave", 1)
        service.saveResult = EncounterMethodView("1", "walk", "Walking in tall grass or a cave", 1)

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeEncounterMethodService()
        val controller = EncounterMethodController(service)
        val command = UpdateEncounterMethodInput("1", "walk", "Walking in tall grass or a cave", 1)
        service.updateResult = EncounterMethodView("1", "walk", "Walking in tall grass or a cave", 1)

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeEncounterMethodService()
        val controller = EncounterMethodController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeEncounterMethodService : EncounterMethodService {
        var listCondition: EncounterMethodSpecification? = null
        var savedCommand: SaveEncounterMethodInput? = null
        var updatedCommand: UpdateEncounterMethodInput? = null
        var removedId: Long? = null

        var listResult: List<EncounterMethodView> = emptyList()
        lateinit var saveResult: EncounterMethodView
        lateinit var updateResult: EncounterMethodView

        override fun save(command: SaveEncounterMethodInput): EncounterMethodView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateEncounterMethodInput): EncounterMethodView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: EncounterMethodSpecification): List<EncounterMethodView> {
            listCondition = specification
            return listResult
        }
    }
}
