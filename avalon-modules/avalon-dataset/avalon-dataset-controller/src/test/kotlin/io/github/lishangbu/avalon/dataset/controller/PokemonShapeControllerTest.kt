package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonShapeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonShapeView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonShapeInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonShapeInput
import io.github.lishangbu.avalon.dataset.service.PokemonShapeService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class PokemonShapeControllerTest {
    @Test
    fun listPokemonShapes_delegatesToService() {
        val service = FakePokemonShapeService()
        val controller = PokemonShapeController(service)
        val list = listOf(PokemonShapeView("1", "ball", "Ball"))
        service.listResult = list
        val specification = PokemonShapeSpecification(id = "1", internalName = "ball", name = "Ball")

        val result = controller.listPokemonShapes(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakePokemonShapeService()
        val controller = PokemonShapeController(service)
        val command = SavePokemonShapeInput("ball", "Ball")
        service.saveResult = PokemonShapeView("1", "ball", "Ball")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakePokemonShapeService()
        val controller = PokemonShapeController(service)
        val command = UpdatePokemonShapeInput("1", "ball", "Ball")
        service.updateResult = PokemonShapeView("1", "ball", "Ball")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakePokemonShapeService()
        val controller = PokemonShapeController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakePokemonShapeService : PokemonShapeService {
        var listCondition: PokemonShapeSpecification? = null
        var savedCommand: SavePokemonShapeInput? = null
        var updatedCommand: UpdatePokemonShapeInput? = null
        var removedId: Long? = null

        var listResult: List<PokemonShapeView> = emptyList()
        lateinit var saveResult: PokemonShapeView
        lateinit var updateResult: PokemonShapeView

        override fun save(command: SavePokemonShapeInput): PokemonShapeView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdatePokemonShapeInput): PokemonShapeView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: PokemonShapeSpecification): List<PokemonShapeView> {
            listCondition = specification
            return listResult
        }
    }
}
