package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Location
import io.github.lishangbu.avalon.dataset.entity.Region
import io.github.lishangbu.avalon.dataset.entity.dto.LocationSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.LocationView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveLocationInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateLocationInput
import io.github.lishangbu.avalon.dataset.service.LocationService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class LocationControllerTest {
    @Test
    fun getLocationPage_delegatesToService() {
        val service = FakeLocationService()
        val controller = LocationController(service)
        val pageable = PageRequest.of(0, 5)
        val page: Page<LocationView> = Page(listOf(locationView(1L)), 1, 1)
        service.pageResult = page
        val specification = LocationSpecification(id = "1", internalName = "canalave-city", regionId = "4")

        val result = controller.getLocationPage(pageable = pageable, specification = specification)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("4", service.pageCondition!!.regionId)
    }

    @Test
    fun listLocations_delegatesToService() {
        val service = FakeLocationService()
        val controller = LocationController(service)
        val list = listOf(locationView(1L))
        service.listResult = list
        val specification = LocationSpecification(regionId = "4")

        val result = controller.listLocations(specification)

        assertSame(list, result)
        assertSame(specification, service.listCondition)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeLocationService()
        val controller = LocationController(service)
        val command = SaveLocationInput("canalave-city", "Canalave City", "4")
        service.saveResult = locationView(1L)

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeLocationService()
        val controller = LocationController(service)
        val command = UpdateLocationInput("1", "canalave-city", "Canalave City", "4")
        service.updateResult = locationView(1L)

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeLocationService()
        val controller = LocationController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeLocationService : LocationService {
        var pageCondition: LocationSpecification? = null
        var listCondition: LocationSpecification? = null
        var pageable: Pageable? = null
        var savedCommand: SaveLocationInput? = null
        var updatedCommand: UpdateLocationInput? = null
        var removedId: Long? = null

        var pageResult: Page<LocationView> = Page(emptyList(), 0, 0)
        var listResult: List<LocationView> = emptyList()
        lateinit var saveResult: LocationView
        lateinit var updateResult: LocationView

        override fun getPageByCondition(
            specification: LocationSpecification,
            pageable: Pageable,
        ): Page<LocationView> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(command: SaveLocationInput): LocationView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateLocationInput): LocationView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: LocationSpecification): List<LocationView> {
            listCondition = specification
            return listResult
        }
    }
}

private fun locationView(id: Long): LocationView =
    LocationView(
        Location {
            this.id = id
            internalName = "canalave-city"
            name = "Canalave City"
            region =
                Region {
                    this.id = 4L
                    internalName = "sinnoh"
                    name = "Sinnoh"
                }
        },
    )
