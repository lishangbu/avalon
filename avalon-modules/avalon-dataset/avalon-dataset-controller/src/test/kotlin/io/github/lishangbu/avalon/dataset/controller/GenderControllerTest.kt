package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.GenderSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.GenderView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveGenderInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateGenderInput
import io.github.lishangbu.avalon.dataset.service.GenderService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class GenderControllerTest {
    @Test
    fun listGenders_delegatesToService() {
        val service = FakeGenderService()
        val controller = GenderController(service)
        val list = listOf(GenderView("1", "female", "雌性"))
        service.listResult = list
        val specification = GenderSpecification(id = "1", internalName = "female", name = "雌性")

        val result = controller.listGenders(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeGenderService()
        val controller = GenderController(service)
        val command = SaveGenderInput("female", "雌性")
        service.saveResult = GenderView("1", "female", "雌性")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeGenderService()
        val controller = GenderController(service)
        val command = UpdateGenderInput("1", "female", "雌性")
        service.updateResult = GenderView("1", "female", "雌性")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeGenderService()
        val controller = GenderController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeGenderService : GenderService {
        var listCondition: GenderSpecification? = null
        var savedCommand: SaveGenderInput? = null
        var updatedCommand: UpdateGenderInput? = null
        var removedId: Long? = null

        var listResult: List<GenderView> = emptyList()
        lateinit var saveResult: GenderView
        lateinit var updateResult: GenderView

        override fun save(command: SaveGenderInput): GenderView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateGenderInput): GenderView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: GenderSpecification): List<GenderView> {
            listCondition = specification
            return listResult
        }
    }
}
