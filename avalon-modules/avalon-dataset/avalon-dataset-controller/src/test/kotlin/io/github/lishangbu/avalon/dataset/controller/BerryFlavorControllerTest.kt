package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorSpecification
import io.github.lishangbu.avalon.dataset.service.BerryFlavorService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class BerryFlavorControllerTest {
    @Test
    fun listBerryFlavors_delegatesToService() {
        val service = FakeBerryFlavorService()
        val controller = BerryFlavorController(service)
        val list = listOf(BerryFlavor())
        service.listResult = list
        val specification = BerryFlavorSpecification(id = "1", internalName = "spicy", name = "辣")

        val result = controller.listBerryFlavors(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
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
        var listCondition: BerryFlavorSpecification? = null
        var saved: BerryFlavor? = null
        var updated: BerryFlavor? = null
        var removedId: Long? = null

        var listResult: List<BerryFlavor> = emptyList()
        var saveResult: BerryFlavor = BerryFlavor()
        var updateResult: BerryFlavor = BerryFlavor()

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

        override fun listByCondition(specification: BerryFlavorSpecification): List<BerryFlavor> {
            listCondition = specification
            return listResult
        }
    }
}
