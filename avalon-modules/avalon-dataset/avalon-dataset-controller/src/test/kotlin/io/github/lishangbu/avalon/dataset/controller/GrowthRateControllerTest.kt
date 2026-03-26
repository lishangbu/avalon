package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveGrowthRateInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateGrowthRateInput
import io.github.lishangbu.avalon.dataset.service.GrowthRateService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class GrowthRateControllerTest {
    @Test
    fun listGrowthRates_delegatesToService() {
        val service = FakeGrowthRateService()
        val controller = GrowthRateController(service)
        val list = listOf(GrowthRateView("1", "slow", "慢", "slow"))
        service.listResult = list
        val specification = GrowthRateSpecification(id = "1", internalName = "slow", name = "慢", description = "slow")

        val result = controller.listGrowthRates(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeGrowthRateService()
        val controller = GrowthRateController(service)
        val command = SaveGrowthRateInput("slow", "慢", "slow")
        service.saveResult = GrowthRateView("1", "slow", "慢", "slow")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeGrowthRateService()
        val controller = GrowthRateController(service)
        val command = UpdateGrowthRateInput("1", "slow", "慢", "slow")
        service.updateResult = GrowthRateView("1", "slow", "慢", "slow")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeGrowthRateService()
        val controller = GrowthRateController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeGrowthRateService : GrowthRateService {
        var listCondition: GrowthRateSpecification? = null
        var savedCommand: SaveGrowthRateInput? = null
        var updatedCommand: UpdateGrowthRateInput? = null
        var removedId: Long? = null

        var listResult: List<GrowthRateView> = emptyList()
        lateinit var saveResult: GrowthRateView
        lateinit var updateResult: GrowthRateView

        override fun save(command: SaveGrowthRateInput): GrowthRateView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateGrowthRateInput): GrowthRateView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: GrowthRateSpecification): List<GrowthRateView> {
            listCondition = specification
            return listResult
        }
    }
}
