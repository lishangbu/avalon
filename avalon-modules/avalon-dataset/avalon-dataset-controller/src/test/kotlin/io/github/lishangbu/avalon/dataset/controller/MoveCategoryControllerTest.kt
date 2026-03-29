package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.MoveCategorySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveCategoryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveCategoryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveCategoryInput
import io.github.lishangbu.avalon.dataset.service.MoveCategoryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class MoveCategoryControllerTest {
    @Test
    fun listMoveCategories_delegatesToService() {
        val service = FakeMoveCategoryService()
        val controller = MoveCategoryController(service)
        val list = listOf(MoveCategoryView("0", "damage", "damage", "Inflicts damage"))
        service.listResult = list
        val specification = MoveCategorySpecification(id = "0", internalName = "damage", name = "damage")

        val result = controller.listMoveCategories(specification)

        assertSame(list, result)
        assertEquals("0", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeMoveCategoryService()
        val controller = MoveCategoryController(service)
        val command = SaveMoveCategoryInput("damage", "damage", "Inflicts damage")
        service.saveResult = MoveCategoryView("0", "damage", "damage", "Inflicts damage")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeMoveCategoryService()
        val controller = MoveCategoryController(service)
        val command = UpdateMoveCategoryInput("0", "damage", "damage", "Inflicts damage")
        service.updateResult = MoveCategoryView("0", "damage", "damage", "Inflicts damage")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeMoveCategoryService()
        val controller = MoveCategoryController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeMoveCategoryService : MoveCategoryService {
        var listCondition: MoveCategorySpecification? = null
        var savedCommand: SaveMoveCategoryInput? = null
        var updatedCommand: UpdateMoveCategoryInput? = null
        var removedId: Long? = null

        var listResult: List<MoveCategoryView> = emptyList()
        lateinit var saveResult: MoveCategoryView
        lateinit var updateResult: MoveCategoryView

        override fun save(command: SaveMoveCategoryInput): MoveCategoryView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateMoveCategoryInput): MoveCategoryView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: MoveCategorySpecification): List<MoveCategoryView> {
            listCondition = specification
            return listResult
        }
    }
}
