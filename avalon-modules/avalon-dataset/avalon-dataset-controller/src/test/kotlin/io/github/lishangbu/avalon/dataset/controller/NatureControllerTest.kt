package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.NatureSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.NatureView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveNatureInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateNatureInput
import io.github.lishangbu.avalon.dataset.service.NatureService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class NatureControllerTest {
    @Test
    fun listNatures_delegatesToService() {
        val service = FakeNatureService()
        val controller = NatureController(service)
        val list = listOf(natureView())
        service.listResult = list
        val specification = NatureSpecification(id = "2", internalName = "bold", decreasedStatId = "2")

        val result = controller.listNatures(specification)

        assertSame(list, result)
        assertEquals("2", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeNatureService()
        val controller = NatureController(service)
        val command =
            SaveNatureInput(
                internalName = "bold",
                name = "大胆",
                decreasedStatId = "2",
                increasedStatId = "3",
                hatesBerryFlavorId = "1",
                likesBerryFlavorId = "5",
            )
        service.saveResult = natureView()

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeNatureService()
        val controller = NatureController(service)
        val command =
            UpdateNatureInput(
                id = "2",
                internalName = "bold",
                name = "大胆",
                decreasedStatId = "2",
                increasedStatId = "3",
                hatesBerryFlavorId = "1",
                likesBerryFlavorId = "5",
            )
        service.updateResult = natureView()

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeNatureService()
        val controller = NatureController(service)

        controller.deleteById(2L)

        assertEquals(2L, service.removedId)
    }

    private class FakeNatureService : NatureService {
        var listCondition: NatureSpecification? = null
        var savedCommand: SaveNatureInput? = null
        var updatedCommand: UpdateNatureInput? = null
        var removedId: Long? = null

        var listResult: List<NatureView> = emptyList()
        lateinit var saveResult: NatureView
        lateinit var updateResult: NatureView

        override fun save(command: SaveNatureInput): NatureView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateNatureInput): NatureView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: NatureSpecification): List<NatureView> {
            listCondition = specification
            return listResult
        }
    }
}

private fun natureView(): NatureView =
    NatureView(
        id = "2",
        internalName = "bold",
        name = "大胆",
        decreasedStat =
            NatureView.TargetOf_decreasedStat(
                id = "2",
                internalName = "attack",
                name = "攻击",
            ),
        increasedStat =
            NatureView.TargetOf_increasedStat(
                id = "3",
                internalName = "defense",
                name = "防御",
            ),
        hatesBerryFlavor =
            NatureView.TargetOf_hatesBerryFlavor(
                id = "1",
                internalName = "spicy",
                name = "辣",
            ),
        likesBerryFlavor =
            NatureView.TargetOf_likesBerryFlavor(
                id = "5",
                internalName = "sour",
                name = "酸",
            ),
    )
