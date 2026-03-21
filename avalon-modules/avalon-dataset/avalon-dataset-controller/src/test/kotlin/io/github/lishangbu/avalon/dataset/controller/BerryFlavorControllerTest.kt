package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.service.BerryFlavorService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class BerryFlavorControllerTest {
    @Test
    fun getBerryFlavorPage_delegatesToService() {
        val service = FakeBerryFlavorService()
        val controller = BerryFlavorController(service)
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<BerryFlavor> = Page(listOf(BerryFlavor()), 1, 1)
        service.pageResult = page

        val result = controller.getBerryFlavorPage(pageable, 1L, "spicy", "辣")

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals(1L, service.pageCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeBerryFlavorService()
        val controller = BerryFlavorController(service)
        val berryFlavor = BerryFlavor()
        service.saveResult = berryFlavor

        val result = controller.save(berryFlavor)

        assertSame(berryFlavor, result)
        assertSame(berryFlavor, service.saved)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeBerryFlavorService()
        val controller = BerryFlavorController(service)
        val berryFlavor = BerryFlavor()
        service.updateResult = berryFlavor

        val result = controller.update(berryFlavor)

        assertSame(berryFlavor, result)
        assertSame(berryFlavor, service.updated)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeBerryFlavorService()
        val controller = BerryFlavorController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeBerryFlavorService : BerryFlavorService {
        var pageCondition: BerryFlavor? = null
        var pageable: Pageable? = null
        var saved: BerryFlavor? = null
        var updated: BerryFlavor? = null
        var removedId: Long? = null

        var pageResult: Page<BerryFlavor> = Page(emptyList(), 0, 0)
        var saveResult: BerryFlavor = BerryFlavor()
        var updateResult: BerryFlavor = BerryFlavor()

        override fun getPageByCondition(
            berryFlavor: BerryFlavor,
            pageable: Pageable,
        ): Page<BerryFlavor> {
            pageCondition = berryFlavor
            this.pageable = pageable
            return pageResult
        }

        override fun save(berryFlavor: BerryFlavor): BerryFlavor {
            saved = berryFlavor
            return saveResult
        }

        override fun update(berryFlavor: BerryFlavor): BerryFlavor {
            updated = berryFlavor
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }
    }
}
