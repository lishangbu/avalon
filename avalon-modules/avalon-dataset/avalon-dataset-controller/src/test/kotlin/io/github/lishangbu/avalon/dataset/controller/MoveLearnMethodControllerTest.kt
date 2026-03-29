package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.MoveLearnMethodSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveLearnMethodView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveLearnMethodInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveLearnMethodInput
import io.github.lishangbu.avalon.dataset.service.MoveLearnMethodService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class MoveLearnMethodControllerTest {
    @Test
    fun listMoveLearnMethods_delegatesToService() {
        val service = FakeMoveLearnMethodService()
        val controller = MoveLearnMethodController(service)
        val list =
            listOf(
                MoveLearnMethodView(
                    "1",
                    "level-up",
                    "Level up",
                    "Learned when a Pokemon reaches a certain level.",
                ),
            )
        service.listResult = list
        val specification = MoveLearnMethodSpecification(id = "1", internalName = "level-up", name = "Level up")

        val result = controller.listMoveLearnMethods(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeMoveLearnMethodService()
        val controller = MoveLearnMethodController(service)
        val command = SaveMoveLearnMethodInput("level-up", "Level up", "Learned when a Pokemon reaches a certain level.")
        service.saveResult = MoveLearnMethodView("1", "level-up", "Level up", "Learned when a Pokemon reaches a certain level.")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeMoveLearnMethodService()
        val controller = MoveLearnMethodController(service)
        val command =
            UpdateMoveLearnMethodInput("1", "level-up", "Level up", "Learned when a Pokemon reaches a certain level.")
        service.updateResult = MoveLearnMethodView("1", "level-up", "Level up", "Learned when a Pokemon reaches a certain level.")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeMoveLearnMethodService()
        val controller = MoveLearnMethodController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeMoveLearnMethodService : MoveLearnMethodService {
        var listCondition: MoveLearnMethodSpecification? = null
        var savedCommand: SaveMoveLearnMethodInput? = null
        var updatedCommand: UpdateMoveLearnMethodInput? = null
        var removedId: Long? = null

        var listResult: List<MoveLearnMethodView> = emptyList()
        lateinit var saveResult: MoveLearnMethodView
        lateinit var updateResult: MoveLearnMethodView

        override fun save(command: SaveMoveLearnMethodInput): MoveLearnMethodView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateMoveLearnMethodInput): MoveLearnMethodView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: MoveLearnMethodSpecification): List<MoveLearnMethodView> {
            listCondition = specification
            return listResult
        }
    }
}
