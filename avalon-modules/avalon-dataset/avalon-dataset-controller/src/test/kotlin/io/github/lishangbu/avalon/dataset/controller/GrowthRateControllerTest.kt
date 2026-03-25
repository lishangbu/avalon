package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateSpecification
import io.github.lishangbu.avalon.dataset.service.GrowthRateService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class GrowthRateControllerTest {
    @Test
    fun listGrowthRates_delegatesToService() {
        val service = FakeGrowthRateService()
        val controller = GrowthRateController(service)
        val list = listOf(GrowthRate {})
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
        val growthRate = GrowthRate {}
        service.saveResult = growthRate

        val result = controller.save(growthRate)

        assertSame(growthRate, result)
        assertSame(growthRate, service.saved)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeGrowthRateService()
        val controller = GrowthRateController(service)
        val growthRate = GrowthRate {}
        service.updateResult = growthRate

        val result = controller.update(growthRate)

        assertSame(growthRate, result)
        assertSame(growthRate, service.updated)
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
        var saved: GrowthRate? = null
        var updated: GrowthRate? = null
        var removedId: Long? = null

        var listResult: List<GrowthRate> = emptyList()
        var saveResult: GrowthRate = GrowthRate {}
        var updateResult: GrowthRate = GrowthRate {}

        override fun save(growthRate: GrowthRate): GrowthRate {
            saved = growthRate
            return saveResult
        }

        override fun update(growthRate: GrowthRate): GrowthRate {
            updated = growthRate
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: GrowthRateSpecification): List<GrowthRate> {
            listCondition = specification
            return listResult
        }
    }
}
