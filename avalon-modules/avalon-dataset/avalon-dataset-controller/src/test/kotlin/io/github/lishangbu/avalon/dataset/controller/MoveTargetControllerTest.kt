package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.MoveTargetSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveTargetView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveTargetInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveTargetInput
import io.github.lishangbu.avalon.dataset.service.MoveTargetService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class MoveTargetControllerTest {
    @Test
    fun listMoveTargets_delegatesToService() {
        val service = FakeMoveTargetService()
        val controller = MoveTargetController(service)
        val list = listOf(MoveTargetView("1", "specific-move", "specific-move", "One specific move."))
        service.listResult = list
        val specification = MoveTargetSpecification(id = "1", internalName = "specific-move", name = "specific-move")

        val result = controller.listMoveTargets(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeMoveTargetService()
        val controller = MoveTargetController(service)
        val command = SaveMoveTargetInput("specific-move", "specific-move", "One specific move.")
        service.saveResult = MoveTargetView("1", "specific-move", "specific-move", "One specific move.")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeMoveTargetService()
        val controller = MoveTargetController(service)
        val command = UpdateMoveTargetInput("1", "specific-move", "specific-move", "One specific move.")
        service.updateResult = MoveTargetView("1", "specific-move", "specific-move", "One specific move.")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeMoveTargetService()
        val controller = MoveTargetController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeMoveTargetService : MoveTargetService {
        var listCondition: MoveTargetSpecification? = null
        var savedCommand: SaveMoveTargetInput? = null
        var updatedCommand: UpdateMoveTargetInput? = null
        var removedId: Long? = null

        var listResult: List<MoveTargetView> = emptyList()
        lateinit var saveResult: MoveTargetView
        lateinit var updateResult: MoveTargetView

        override fun save(command: SaveMoveTargetInput): MoveTargetView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateMoveTargetInput): MoveTargetView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: MoveTargetSpecification): List<MoveTargetView> {
            listCondition = specification
            return listResult
        }
    }
}
