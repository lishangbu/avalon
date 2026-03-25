package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Berry
import io.github.lishangbu.avalon.dataset.entity.dto.BerrySpecification
import io.github.lishangbu.avalon.dataset.service.BerryService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class BerryControllerTest {
    @Test
    fun getBerryPage_delegatesToService() {
        val service = FakeBerryService()
        val controller = BerryController(service)
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<Berry> = Page(listOf(Berry()), 1, 1)
        service.pageResult = page
        val specification =
            BerrySpecification(
                id = "1",
                internalName = "cheri",
                name = "樱子",
                growthTime = 2,
                maxHarvest = 3,
                bulk = 4,
                smoothness = 5,
                soilDryness = 6,
                berryFirmnessId = "7",
                naturalGiftTypeId = "8",
                naturalGiftPower = 9,
            )

        val result = controller.getBerryPage(pageable = pageable, specification = specification)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("1", service.pageCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeBerryService()
        val controller = BerryController(service)
        val berry = Berry()
        service.saveResult = berry

        val result = controller.save(berry)

        assertSame(berry, result)
        assertSame(berry, service.savedBerry)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeBerryService()
        val controller = BerryController(service)
        val berry = Berry()
        service.updateResult = berry

        val result = controller.update(berry)

        assertSame(berry, result)
        assertSame(berry, service.updatedBerry)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeBerryService()
        val controller = BerryController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeBerryService : BerryService {
        var pageCondition: BerrySpecification? = null
        var pageable: Pageable? = null
        var savedBerry: Berry? = null
        var updatedBerry: Berry? = null
        var removedId: Long? = null

        var pageResult: Page<Berry> = Page(emptyList(), 0, 0)
        var saveResult: Berry = Berry()
        var updateResult: Berry = Berry()

        override fun getPageByCondition(
            specification: BerrySpecification,
            pageable: Pageable,
        ): Page<Berry> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(berry: Berry): Berry {
            savedBerry = berry
            return saveResult
        }

        override fun update(berry: Berry): Berry {
            updatedBerry = berry
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }
    }
}
