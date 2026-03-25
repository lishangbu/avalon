package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Type
import io.github.lishangbu.avalon.dataset.entity.dto.TypeSpecification
import io.github.lishangbu.avalon.dataset.service.TypeService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class TypeControllerTest {
    @Test
    fun listTypes_delegatesToService() {
        val service = FakeTypeService()
        val controller = TypeController(service)
        val list = listOf(Type())
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
        val type = Type()
        service.saveResult = type

        val result = controller.save(type)

        assertSame(type, result)
        assertSame(type, service.saved)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeTypeService()
        val controller = TypeController(service)
        val type = Type()
        service.updateResult = type

        val result = controller.update(type)

        assertSame(type, result)
        assertSame(type, service.updated)
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
        var saved: Type? = null
        var updated: Type? = null
        var removedId: Long? = null

        var listResult: List<Type> = emptyList()
        var saveResult: Type = Type()
        var updateResult: Type = Type()

        override fun save(type: Type): Type {
            saved = type
            return saveResult
        }

        override fun update(type: Type): Type {
            updated = type
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: TypeSpecification): List<Type> {
            listCondition = specification
            return listResult
        }
    }
}
