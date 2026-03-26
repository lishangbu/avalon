package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveDamageClassInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveDamageClassInput
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
        val pageable = PageRequest.of(0, 5)
        val page: Page<MoveDamageClassView> = Page(listOf(MoveDamageClassView("1", "physical", "物理", "desc")), 1, 1)
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
        val list = listOf(MoveDamageClassView("1", "physical", "物理", "desc"))
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
        val command = SaveMoveDamageClassInput("physical", "物理", "desc")
        service.saveResult = MoveDamageClassView("1", "physical", "物理", "desc")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeMoveDamageClassService()
        val controller = MoveDamageClassController(service)
        val command = UpdateMoveDamageClassInput("1", "physical", "物理", "desc")
        service.updateResult = MoveDamageClassView("1", "physical", "物理", "desc")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
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
        var savedCommand: SaveMoveDamageClassInput? = null
        var updatedCommand: UpdateMoveDamageClassInput? = null
        var removedId: Long? = null

        var pageResult: Page<MoveDamageClassView> = Page(emptyList(), 0, 0)
        var listResult: List<MoveDamageClassView> = emptyList()
        lateinit var saveResult: MoveDamageClassView
        lateinit var updateResult: MoveDamageClassView

        override fun getPageByCondition(
            specification: MoveDamageClassSpecification,
            pageable: Pageable,
        ): Page<MoveDamageClassView> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(command: SaveMoveDamageClassInput): MoveDamageClassView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateMoveDamageClassInput): MoveDamageClassView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: MoveDamageClassSpecification): List<MoveDamageClassView> {
            listCondition = specification
            return listResult
        }
    }
}
