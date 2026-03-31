package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Location
import io.github.lishangbu.avalon.dataset.entity.LocationArea
import io.github.lishangbu.avalon.dataset.entity.dto.LocationAreaSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.LocationAreaView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveLocationAreaInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateLocationAreaInput
import io.github.lishangbu.avalon.dataset.service.LocationAreaService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class LocationAreaControllerTest {
    @Test
    fun getLocationAreaPage_delegatesToService() {
        val service = FakeLocationAreaService()
        val controller = LocationAreaController(service)
        val pageable = PageRequest.of(0, 5)
        val page: Page<LocationAreaView> = Page(listOf(locationAreaView(1L)), 1, 1)
        service.pageResult = page
        val specification = LocationAreaSpecification(id = "1", internalName = "canalave-city-area", locationId = "1")

        val result = controller.getLocationAreaPage(pageable = pageable, specification = specification)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("1", service.pageCondition!!.locationId)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeLocationAreaService()
        val controller = LocationAreaController(service)
        val command = SaveLocationAreaInput(1, "canalave-city-area", "Canalave City", "1")
        service.saveResult = locationAreaView(1L)

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeLocationAreaService()
        val controller = LocationAreaController(service)
        val command = UpdateLocationAreaInput("1", 1, "canalave-city-area", "Canalave City", "1")
        service.updateResult = locationAreaView(1L)

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeLocationAreaService()
        val controller = LocationAreaController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeLocationAreaService : LocationAreaService {
        var pageCondition: LocationAreaSpecification? = null
        var pageable: Pageable? = null
        var savedCommand: SaveLocationAreaInput? = null
        var updatedCommand: UpdateLocationAreaInput? = null
        var removedId: Long? = null

        var pageResult: Page<LocationAreaView> = Page(emptyList(), 0, 0)
        lateinit var saveResult: LocationAreaView
        lateinit var updateResult: LocationAreaView

        override fun getPageByCondition(
            specification: LocationAreaSpecification,
            pageable: Pageable,
        ): Page<LocationAreaView> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(command: SaveLocationAreaInput): LocationAreaView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateLocationAreaInput): LocationAreaView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }
    }
}

private fun locationAreaView(id: Long): LocationAreaView =
    LocationAreaView(
        LocationArea {
            this.id = id
            gameIndex = 1
            internalName = "canalave-city-area"
            name = "Canalave City"
            location =
                Location {
                    this.id = 1L
                    internalName = "canalave-city"
                    name = "Canalave City"
                }
        },
    )
