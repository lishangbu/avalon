package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureShapeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureShapeView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureShapeInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureShapeInput
import io.github.lishangbu.avalon.dataset.service.CreatureShapeService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class CreatureShapeControllerTest {
    @Test
    fun listCreatureShapes_delegatesToService() {
        val service = FakeCreatureShapeService()
        val controller = CreatureShapeController(service)
        val list = listOf(CreatureShapeView("1", "ball", "Ball"))
        service.listResult = list
        val specification = CreatureShapeSpecification(id = "1", internalName = "ball", name = "Ball")

        val result = controller.listCreatureShapes(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeCreatureShapeService()
        val controller = CreatureShapeController(service)
        val command = SaveCreatureShapeInput("ball", "Ball")
        service.saveResult = CreatureShapeView("1", "ball", "Ball")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeCreatureShapeService()
        val controller = CreatureShapeController(service)
        val command = UpdateCreatureShapeInput("1", "ball", "Ball")
        service.updateResult = CreatureShapeView("1", "ball", "Ball")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeCreatureShapeService()
        val controller = CreatureShapeController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeCreatureShapeService : CreatureShapeService {
        var listCondition: CreatureShapeSpecification? = null
        var savedCommand: SaveCreatureShapeInput? = null
        var updatedCommand: UpdateCreatureShapeInput? = null
        var removedId: Long? = null

        var listResult: List<CreatureShapeView> = emptyList()
        lateinit var saveResult: CreatureShapeView
        lateinit var updateResult: CreatureShapeView

        override fun save(command: SaveCreatureShapeInput): CreatureShapeView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateCreatureShapeInput): CreatureShapeView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: CreatureShapeSpecification): List<CreatureShapeView> {
            listCondition = specification
            return listResult
        }
    }
}
