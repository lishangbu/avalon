package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.BerrySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryInput
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
        val pageable = PageRequest.of(0, 5)
        val page: Page<BerryView> = Page(listOf(berryView()), 1, 1)
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
        val command = SaveBerryInput("cheri", "樱子", 2, 3, 4, 5, 6, "7", "8", 9)
        service.saveResult = berryView()

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeBerryService()
        val controller = BerryController(service)
        val command = UpdateBerryInput("1", "cheri", "樱子", 2, 3, 4, 5, 6, "7", "8", 9)
        service.updateResult = berryView()

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
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
        var savedCommand: SaveBerryInput? = null
        var updatedCommand: UpdateBerryInput? = null
        var removedId: Long? = null

        var pageResult: Page<BerryView> = Page(emptyList(), 0, 0)
        lateinit var saveResult: BerryView
        lateinit var updateResult: BerryView

        override fun getPageByCondition(
            specification: BerrySpecification,
            pageable: Pageable,
        ): Page<BerryView> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(command: SaveBerryInput): BerryView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateBerryInput): BerryView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }
    }
}

private fun berryView(): BerryView = BerryView("1", "cheri", "樱子", 2, 3, 4, 5, 6, "7", "8", 9, "hard", "硬", "fire", "火")
