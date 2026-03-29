package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.PokemonHabitatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonHabitatView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonHabitatInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonHabitatInput
import io.github.lishangbu.avalon.dataset.service.PokemonHabitatService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class PokemonHabitatControllerTest {
    @Test
    fun listPokemonHabitats_delegatesToService() {
        val service = FakePokemonHabitatService()
        val controller = PokemonHabitatController(service)
        val list = listOf(PokemonHabitatView("1", "cave", "cave"))
        service.listResult = list
        val specification = PokemonHabitatSpecification(id = "1", internalName = "cave", name = "cave")

        val result = controller.listPokemonHabitats(specification)

        assertSame(list, result)
        assertEquals("1", service.listCondition!!.id)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakePokemonHabitatService()
        val controller = PokemonHabitatController(service)
        val command = SavePokemonHabitatInput("cave", "cave")
        service.saveResult = PokemonHabitatView("1", "cave", "cave")

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakePokemonHabitatService()
        val controller = PokemonHabitatController(service)
        val command = UpdatePokemonHabitatInput("1", "cave", "cave")
        service.updateResult = PokemonHabitatView("1", "cave", "cave")

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakePokemonHabitatService()
        val controller = PokemonHabitatController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakePokemonHabitatService : PokemonHabitatService {
        var listCondition: PokemonHabitatSpecification? = null
        var savedCommand: SavePokemonHabitatInput? = null
        var updatedCommand: UpdatePokemonHabitatInput? = null
        var removedId: Long? = null

        var listResult: List<PokemonHabitatView> = emptyList()
        lateinit var saveResult: PokemonHabitatView
        lateinit var updateResult: PokemonHabitatView

        override fun save(command: SavePokemonHabitatInput): PokemonHabitatView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdatePokemonHabitatInput): PokemonHabitatView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }

        override fun listByCondition(specification: PokemonHabitatSpecification): List<PokemonHabitatView> {
            listCondition = specification
            return listResult
        }
    }
}
