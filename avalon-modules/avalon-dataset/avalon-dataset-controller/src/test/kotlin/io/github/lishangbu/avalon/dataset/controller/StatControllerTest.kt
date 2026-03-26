package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.SaveStatInput
import io.github.lishangbu.avalon.dataset.entity.dto.StatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.StatView
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateStatInput
import io.github.lishangbu.avalon.dataset.service.StatService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class StatControllerTest {
    @Test
    fun listStats_delegatesToService() {
        val service = FakeStatService()
        val controller = StatController(service)
        val list = listOf(StatView("1", "hp", "生命", 1, false, "2", "physical", "物理"))
        service.listResult = list
        val specification = StatSpecification(id = "1", internalName = "hp", name = "生命", gameIndex = 1, battleOnly = false, moveDamageClassId = "2")

        val result = controller.listStats(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeStatService()
        val controller = StatController(service)
        val command = SaveStatInput("hp", "生命", 1, false, "2")
        service.saveResult = StatView("1", "hp", "生命", 1, false, "2", "physical", "物理")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.saved)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeStatService()
        val controller = StatController(service)
        val command = UpdateStatInput("1", "hp", "生命", 1, false, "2")
        service.updateResult = StatView("1", "hp", "生命", 1, false, "2", "physical", "物理")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updated)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeStatService()
        val controller = StatController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeStatService : StatService {
        var listCondition: StatSpecification? = null
        var saved: SaveStatInput? = null
        var updated: UpdateStatInput? = null
        var removedId: Long? = null

        var listResult: List<StatView> = emptyList()
        lateinit var saveResult: StatView
        lateinit var updateResult: StatView

        override fun save(command: SaveStatInput): StatView {
            saved = command
            return saveResult
        }

        override fun update(command: UpdateStatInput): StatView {
            updated = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: StatSpecification): List<StatView> {
            listCondition = specification
            return listResult
        }
    }
}
