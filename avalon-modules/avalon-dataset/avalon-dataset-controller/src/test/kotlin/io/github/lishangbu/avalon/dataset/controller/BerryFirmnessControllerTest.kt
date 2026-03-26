package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryFirmnessInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryFirmnessInput
import io.github.lishangbu.avalon.dataset.service.BerryFirmnessService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class BerryFirmnessControllerTest {
    @Test
    fun getBerryFirmnessPage_delegatesToService() {
        val service = FakeBerryFirmnessService()
        val controller = BerryFirmnessController(service)
        val pageable = PageRequest.of(0, 5)
        val page: Page<BerryFirmnessView> = Page(listOf(BerryFirmnessView("1", "hard", "硬")), 1, 1)
        service.pageResult = page
        val specification = BerryFirmnessSpecification(id = "1", internalName = "hard", name = "硬")

        val result = controller.getBerryFirmnessPage(pageable, specification)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("1", service.pageCondition!!.id)
    }

    @Test
    fun listBerryFirmnesses_delegatesToService() {
        val service = FakeBerryFirmnessService()
        val controller = BerryFirmnessController(service)
        val list = listOf(BerryFirmnessView("1", "hard", "硬"))
        service.listResult = list
        val specification = BerryFirmnessSpecification(id = "1", internalName = "hard", name = "硬")

        val result = controller.listBerryFirmnesses(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeBerryFirmnessService()
        val controller = BerryFirmnessController(service)
        val command = SaveBerryFirmnessInput("hard", "硬")
        service.saveResult = BerryFirmnessView("1", "hard", "硬")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeBerryFirmnessService()
        val controller = BerryFirmnessController(service)
        val command = UpdateBerryFirmnessInput("1", "hard", "硬")
        service.updateResult = BerryFirmnessView("1", "hard", "硬")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeBerryFirmnessService()
        val controller = BerryFirmnessController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeBerryFirmnessService : BerryFirmnessService {
        var pageCondition: BerryFirmnessSpecification? = null
        var pageable: Pageable? = null
        var listCondition: BerryFirmnessSpecification? = null
        var savedCommand: SaveBerryFirmnessInput? = null
        var updatedCommand: UpdateBerryFirmnessInput? = null
        var removedId: Long? = null

        var pageResult: Page<BerryFirmnessView> = Page(emptyList(), 0, 0)
        var listResult: List<BerryFirmnessView> = emptyList()
        lateinit var saveResult: BerryFirmnessView
        lateinit var updateResult: BerryFirmnessView

        override fun getPageByCondition(
            specification: BerryFirmnessSpecification,
            pageable: Pageable,
        ): Page<BerryFirmnessView> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(command: SaveBerryFirmnessInput): BerryFirmnessView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateBerryFirmnessInput): BerryFirmnessView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: BerryFirmnessSpecification): List<BerryFirmnessView> {
            listCondition = specification
            return listResult
        }
    }
}
