package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.SaveTypeInput
import io.github.lishangbu.avalon.dataset.entity.dto.TypeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.TypeView
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateTypeInput
import io.github.lishangbu.avalon.dataset.service.TypeService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class TypeControllerTest {
    @Test
    fun listTypes_delegatesToService() {
        val service = FakeTypeService()
        val controller = TypeController(service)
        val list = listOf(TypeView("1", "fire", "火"))
        service.listResult = list
        val specification = TypeSpecification(id = "1", internalName = "fire", name = "火")

        val result = controller.listTypes(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeTypeService()
        val controller = TypeController(service)
        val command = SaveTypeInput("fire", "火")
        service.saveResult = TypeView("1", "fire", "火")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeTypeService()
        val controller = TypeController(service)
        val command = UpdateTypeInput("1", "fire", "火")
        service.updateResult = TypeView("1", "fire", "火")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeTypeService()
        val controller = TypeController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeTypeService : TypeService {
        var listCondition: TypeSpecification? = null
        var savedCommand: SaveTypeInput? = null
        var updatedCommand: UpdateTypeInput? = null
        var removedId: Long? = null

        var listResult: List<TypeView> = emptyList()
        lateinit var saveResult: TypeView
        lateinit var updateResult: TypeView

        override fun save(command: SaveTypeInput): TypeView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateTypeInput): TypeView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: TypeSpecification): List<TypeView> {
            listCondition = specification
            return listResult
        }
    }
}
