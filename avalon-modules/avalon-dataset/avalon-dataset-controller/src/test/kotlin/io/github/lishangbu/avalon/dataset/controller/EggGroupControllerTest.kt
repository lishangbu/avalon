package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEggGroupInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEggGroupInput
import io.github.lishangbu.avalon.dataset.service.EggGroupService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class EggGroupControllerTest {
    @Test
    fun listEggGroups_delegatesToService() {
        val service = FakeEggGroupService()
        val controller = EggGroupController(service)
        val list = listOf(eggGroupView())
        service.listResult = list
        val specification = EggGroupSpecification(id = "1", internalName = "monster", name = "怪兽")

        val result = controller.listEggGroups(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeEggGroupService()
        val controller = EggGroupController(service)
        val command =
            SaveEggGroupInput(
                internalName = "monster",
                name = "怪兽",
                text = "像是怪兽一样，或者比较野性。",
                characteristics = "这个蛋群的宝可梦大多原型基于特摄影片中的怪兽以及爬行动物。",
            )
        service.saveResult = eggGroupView()

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeEggGroupService()
        val controller = EggGroupController(service)
        val command =
            UpdateEggGroupInput(
                id = "1",
                internalName = "monster",
                name = "怪兽",
                text = "像是怪兽一样，或者比较野性。",
                characteristics = "这个蛋群的宝可梦大多原型基于特摄影片中的怪兽以及爬行动物。",
            )
        service.updateResult = eggGroupView()

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeEggGroupService()
        val controller = EggGroupController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeEggGroupService : EggGroupService {
        var listCondition: EggGroupSpecification? = null
        var savedCommand: SaveEggGroupInput? = null
        var updatedCommand: UpdateEggGroupInput? = null
        var removedId: Long? = null

        var listResult: List<EggGroupView> = emptyList()
        lateinit var saveResult: EggGroupView
        lateinit var updateResult: EggGroupView

        override fun save(command: SaveEggGroupInput): EggGroupView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateEggGroupInput): EggGroupView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: EggGroupSpecification): List<EggGroupView> {
            listCondition = specification
            return listResult
        }
    }
}

private fun eggGroupView(): EggGroupView =
    EggGroupView(
        id = "1",
        internalName = "monster",
        name = "怪兽",
        text = "像是怪兽一样，或者比较野性。",
        characteristics = "这个蛋群的宝可梦大多原型基于特摄影片中的怪兽以及爬行动物。",
    )
