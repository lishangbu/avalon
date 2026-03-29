package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonColorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonColorView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonColorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonColorInput
import io.github.lishangbu.avalon.dataset.service.PokemonColorService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class PokemonColorControllerTest {
    @Test
    fun listPokemonColors_delegatesToService() {
        val service = FakePokemonColorService()
        val controller = PokemonColorController(service)
        val list = listOf(PokemonColorView("1", "black", "黑色"))
        service.listResult = list
        val specification = PokemonColorSpecification(id = "1", internalName = "black", name = "黑色")

        val result = controller.listPokemonColors(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakePokemonColorService()
        val controller = PokemonColorController(service)
        val command = SavePokemonColorInput("black", "黑色")
        service.saveResult = PokemonColorView("1", "black", "黑色")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakePokemonColorService()
        val controller = PokemonColorController(service)
        val command = UpdatePokemonColorInput("1", "black", "黑色")
        service.updateResult = PokemonColorView("1", "black", "黑色")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakePokemonColorService()
        val controller = PokemonColorController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakePokemonColorService : PokemonColorService {
        var listCondition: PokemonColorSpecification? = null
        var savedCommand: SavePokemonColorInput? = null
        var updatedCommand: UpdatePokemonColorInput? = null
        var removedId: Long? = null

        var listResult: List<PokemonColorView> = emptyList()
        lateinit var saveResult: PokemonColorView
        lateinit var updateResult: PokemonColorView

        override fun save(command: SavePokemonColorInput): PokemonColorView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdatePokemonColorInput): PokemonColorView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: PokemonColorSpecification): List<PokemonColorView> {
            listCondition = specification
            return listResult
        }
    }
}
