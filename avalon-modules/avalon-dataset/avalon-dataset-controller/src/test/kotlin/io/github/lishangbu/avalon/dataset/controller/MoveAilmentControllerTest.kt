package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.MoveAilmentSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveAilmentView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveAilmentInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveAilmentInput
import io.github.lishangbu.avalon.dataset.service.MoveAilmentService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class MoveAilmentControllerTest {
    @Test
    fun listMoveAilments_delegatesToService() {
        val service = FakeMoveAilmentService()
        val controller = MoveAilmentController(service)
        val list = listOf(MoveAilmentView("1", "paralysis", "paralysis"))
        service.listResult = list
        val specification = MoveAilmentSpecification(id = "1", internalName = "paralysis", name = "paralysis")

        val result = controller.listMoveAilments(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeMoveAilmentService()
        val controller = MoveAilmentController(service)
        val command = SaveMoveAilmentInput("paralysis", "paralysis")
        service.saveResult = MoveAilmentView("1", "paralysis", "paralysis")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeMoveAilmentService()
        val controller = MoveAilmentController(service)
        val command = UpdateMoveAilmentInput("1", "paralysis", "paralysis")
        service.updateResult = MoveAilmentView("1", "paralysis", "paralysis")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeMoveAilmentService()
        val controller = MoveAilmentController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeMoveAilmentService : MoveAilmentService {
        var listCondition: MoveAilmentSpecification? = null
        var savedCommand: SaveMoveAilmentInput? = null
        var updatedCommand: UpdateMoveAilmentInput? = null
        var removedId: Long? = null

        var listResult: List<MoveAilmentView> = emptyList()
        lateinit var saveResult: MoveAilmentView
        lateinit var updateResult: MoveAilmentView

        override fun save(command: SaveMoveAilmentInput): MoveAilmentView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateMoveAilmentInput): MoveAilmentView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: MoveAilmentSpecification): List<MoveAilmentView> {
            listCondition = specification
            return listResult
        }
    }
}
