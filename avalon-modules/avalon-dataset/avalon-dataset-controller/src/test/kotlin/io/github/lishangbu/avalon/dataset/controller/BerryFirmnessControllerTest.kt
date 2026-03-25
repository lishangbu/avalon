package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessSpecification
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
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<BerryFirmness> = Page(listOf(BerryFirmness()), 1, 1)
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
        val list = listOf(BerryFirmness())
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
        val berryFirmness = BerryFirmness()
        service.saveResult = berryFirmness

        val result = controller.save(berryFirmness)

        assertSame(berryFirmness, result)
        assertSame(berryFirmness, service.saved)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeBerryFirmnessService()
        val controller = BerryFirmnessController(service)
        val berryFirmness = BerryFirmness()
        service.updateResult = berryFirmness

        val result = controller.update(berryFirmness)

        assertSame(berryFirmness, result)
        assertSame(berryFirmness, service.updated)
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
        var saved: BerryFirmness? = null
        var updated: BerryFirmness? = null
        var removedId: Long? = null

        var pageResult: Page<BerryFirmness> = Page(emptyList(), 0, 0)
        var listResult: List<BerryFirmness> = emptyList()
        var saveResult: BerryFirmness = BerryFirmness()
        var updateResult: BerryFirmness = BerryFirmness()

        override fun getPageByCondition(
            specification: BerryFirmnessSpecification,
            pageable: Pageable,
        ): Page<BerryFirmness> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(berryFirmness: BerryFirmness): BerryFirmness {
            saved = berryFirmness
            return saveResult
        }

        override fun update(berryFirmness: BerryFirmness): BerryFirmness {
            updated = berryFirmness
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: BerryFirmnessSpecification): List<BerryFirmness> {
            listCondition = specification
            return listResult
        }
    }
}
