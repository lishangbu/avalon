package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Region
import io.github.lishangbu.avalon.dataset.entity.dto.RegionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.RegionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveRegionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateRegionInput
import io.github.lishangbu.avalon.dataset.service.RegionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class RegionControllerTest {
    @Test
    fun listRegions_delegatesToService() {
        val service = FakeRegionService()
        val controller = RegionController(service)
        val list = listOf(regionView(1L))
        service.listResult = list
        val specification = RegionSpecification(id = "1", internalName = "kanto", name = "Kanto")

        val result = controller.listRegions(specification)

        assertSame(list, result)
        assertSame(specification, service.listCondition)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeRegionService()
        val controller = RegionController(service)
        val command = SaveRegionInput("kanto", "Kanto")
        service.saveResult = regionView(1L)

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeRegionService()
        val controller = RegionController(service)
        val command = UpdateRegionInput("1", "kanto", "Kanto")
        service.updateResult = regionView(1L)

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeRegionService()
        val controller = RegionController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeRegionService : RegionService {
        var listCondition: RegionSpecification? = null
        var savedCommand: SaveRegionInput? = null
        var updatedCommand: UpdateRegionInput? = null
        var removedId: Long? = null

        var listResult: List<RegionView> = emptyList()
        lateinit var saveResult: RegionView
        lateinit var updateResult: RegionView

        override fun save(command: SaveRegionInput): RegionView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateRegionInput): RegionView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: RegionSpecification): List<RegionView> {
            listCondition = specification
            return listResult
        }
    }
}

private fun regionView(id: Long): RegionView =
    RegionView(
        Region {
            this.id = id
            internalName = "kanto"
            name = "Kanto"
        },
    )
