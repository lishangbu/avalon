package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Type
import io.github.lishangbu.avalon.dataset.service.TypeService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class TypeControllerTest {
    @Test
    fun getTypePage_delegatesToService() {
        val service = FakeTypeService()
        val controller = TypeController(service)
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<Type> = Page(listOf(Type()), 1, 1)
        service.pageResult = page

        val result = controller.getTypePage(pageable, 1L, "fire", "火")

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals(1L, service.pageCondition!!.id)
    }

    @Test
    fun listTypes_delegatesToService() {
        val service = FakeTypeService()
        val controller = TypeController(service)
        val list = listOf(Type())
        service.listResult = list

        val result = controller.listTypes(1L, "fire", "火")

        assertSame(list, result)
        assertEquals(1L, service.listCondition!!.id)
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
        var pageCondition: Type? = null
        var pageable: Pageable? = null
        var listCondition: Type? = null
        var saved: Type? = null
        var updated: Type? = null
        var removedId: Long? = null

        var pageResult: Page<Type> = Page(emptyList(), 0, 0)
        var listResult: List<Type> = emptyList()
        var saveResult: Type = Type()
        var updateResult: Type = Type()

        override fun getPageByCondition(
            type: Type,
            pageable: Pageable,
        ): Page<Type> {
            pageCondition = type
            this.pageable = pageable
            return pageResult
        }

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

        override fun listByCondition(type: Type): List<Type> {
            listCondition = type
            return listResult
        }
    }
}
