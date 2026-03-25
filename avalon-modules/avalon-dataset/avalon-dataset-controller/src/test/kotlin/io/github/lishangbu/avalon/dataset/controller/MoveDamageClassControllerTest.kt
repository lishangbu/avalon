package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassSpecification
import io.github.lishangbu.avalon.dataset.service.MoveDamageClassService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class MoveDamageClassControllerTest {
    @Test
    fun getMoveDamageClassPage_delegatesToService() {
        val service = FakeMoveDamageClassService()
        val controller = MoveDamageClassController(service)
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<MoveDamageClass> = Page(listOf(MoveDamageClass()), 1, 1)
        service.pageResult = page
        val specification = MoveDamageClassSpecification(id = "1", internalName = "physical", name = "物理", description = "desc")

        val result = controller.getMoveDamageClassPage(pageable, specification)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("1", service.pageCondition!!.id)
    }

    @Test
    fun listMoveDamageClasses_delegatesToService() {
        val service = FakeMoveDamageClassService()
        val controller = MoveDamageClassController(service)
        val list = listOf(MoveDamageClass())
        service.listResult = list
        val specification = MoveDamageClassSpecification(id = "1", internalName = "physical", name = "物理", description = "desc")

        val result = controller.listMoveDamageClasses(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeMoveDamageClassService()
        val controller = MoveDamageClassController(service)
        val moveDamageClass = MoveDamageClass()
        service.saveResult = moveDamageClass

        val result = controller.save(moveDamageClass)

        assertSame(moveDamageClass, result)
        assertSame(moveDamageClass, service.saved)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeMoveDamageClassService()
        val controller = MoveDamageClassController(service)
        val moveDamageClass = MoveDamageClass()
        service.updateResult = moveDamageClass

        val result = controller.update(moveDamageClass)

        assertSame(moveDamageClass, result)
        assertSame(moveDamageClass, service.updated)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeMoveDamageClassService()
        val controller = MoveDamageClassController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeMoveDamageClassService : MoveDamageClassService {
        var pageCondition: MoveDamageClassSpecification? = null
        var pageable: Pageable? = null
        var listCondition: MoveDamageClassSpecification? = null
        var saved: MoveDamageClass? = null
        var updated: MoveDamageClass? = null
        var removedId: Long? = null

        var pageResult: Page<MoveDamageClass> = Page(emptyList(), 0, 0)
        var listResult: List<MoveDamageClass> = emptyList()
        var saveResult: MoveDamageClass = MoveDamageClass()
        var updateResult: MoveDamageClass = MoveDamageClass()

        override fun getPageByCondition(
            specification: MoveDamageClassSpecification,
            pageable: Pageable,
        ): Page<MoveDamageClass> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(moveDamageClass: MoveDamageClass): MoveDamageClass {
            saved = moveDamageClass
            return saveResult
        }

        override fun update(moveDamageClass: MoveDamageClass): MoveDamageClass {
            updated = moveDamageClass
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: MoveDamageClassSpecification): List<MoveDamageClass> {
            listCondition = specification
            return listResult
        }
    }
}
