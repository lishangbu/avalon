package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Pokemon
import io.github.lishangbu.avalon.dataset.entity.PokemonSpecies
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonInput
import io.github.lishangbu.avalon.dataset.service.PokemonService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class PokemonControllerTest {
    @Test
    fun getPokemonPage_delegatesToService() {
        val service = FakePokemonService()
        val controller = PokemonController(service)
        val pageable = PageRequest.of(0, 5)
        val page: Page<PokemonView> = Page(listOf(pokemonView()), 1, 1)
        service.pageResult = page
        val specification =
            PokemonSpecification(
                id = "1",
                internalName = "bulbasaur",
                name = "bulbasaur",
                height = 7,
                weight = 69,
                baseExperience = 64,
                sortingOrder = 1,
                pokemonSpeciesId = "1",
            )

        val result = controller.getPokemonPage(pageable = pageable, specification = specification)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("1", service.pageCondition!!.id)
        assertEquals("1", service.pageCondition!!.pokemonSpeciesId)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakePokemonService()
        val controller = PokemonController(service)
        val command =
            SavePokemonInput(
                internalName = "bulbasaur",
                name = "bulbasaur",
                height = 7,
                weight = 69,
                baseExperience = 64,
                sortingOrder = 1,
                pokemonSpeciesId = "1",
            )
        service.saveResult = pokemonView()

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakePokemonService()
        val controller = PokemonController(service)
        val command =
            UpdatePokemonInput(
                id = "1",
                internalName = "bulbasaur",
                name = "bulbasaur",
                height = 7,
                weight = 69,
                baseExperience = 64,
                sortingOrder = 1,
                pokemonSpeciesId = "1",
            )
        service.updateResult = pokemonView()

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakePokemonService()
        val controller = PokemonController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakePokemonService : PokemonService {
        var pageCondition: PokemonSpecification? = null
        var pageable: Pageable? = null
        var savedCommand: SavePokemonInput? = null
        var updatedCommand: UpdatePokemonInput? = null
        var removedId: Long? = null

        var pageResult: Page<PokemonView> = Page(emptyList(), 0, 0)
        lateinit var saveResult: PokemonView
        lateinit var updateResult: PokemonView

        override fun getPageByCondition(
            specification: PokemonSpecification,
            pageable: Pageable,
        ): Page<PokemonView> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(command: SavePokemonInput): PokemonView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdatePokemonInput): PokemonView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }
    }
}

private fun pokemonView(): PokemonView =
    PokemonView(
        Pokemon {
            id = 1L
            internalName = "bulbasaur"
            name = "bulbasaur"
            height = 7
            weight = 69
            baseExperience = 64
            sortingOrder = 1
            pokemonSpecies =
                PokemonSpecies {
                    id = 1L
                    internalName = "bulbasaur"
                    name = "妙蛙种子"
                }
        },
    )
