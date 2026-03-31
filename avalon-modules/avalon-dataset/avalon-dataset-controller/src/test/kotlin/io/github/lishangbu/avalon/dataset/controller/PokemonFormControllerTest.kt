package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Pokemon
import io.github.lishangbu.avalon.dataset.entity.PokemonForm
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonFormSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonFormView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonFormInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonFormInput
import io.github.lishangbu.avalon.dataset.service.PokemonFormService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class PokemonFormControllerTest {
    @Test
    fun getPokemonFormPage_delegatesToService() {
        val service = FakePokemonFormService()
        val controller = PokemonFormController(service)
        val pageable = PageRequest.of(0, 5)
        val page: Page<PokemonFormView> = Page(listOf(pokemonFormView(1L)), 1, 1)
        service.pageResult = page
        val specification = PokemonFormSpecification(id = "1", internalName = "bulbasaur", pokemonId = "1")

        val result = controller.getPokemonFormPage(pageable = pageable, specification = specification)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("1", service.pageCondition!!.pokemonId)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakePokemonFormService()
        val controller = PokemonFormController(service)
        val command = savePokemonFormInput()
        service.saveResult = pokemonFormView(1L)

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakePokemonFormService()
        val controller = PokemonFormController(service)
        val command = updatePokemonFormInput()
        service.updateResult = pokemonFormView(1L)

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakePokemonFormService()
        val controller = PokemonFormController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakePokemonFormService : PokemonFormService {
        var pageCondition: PokemonFormSpecification? = null
        var pageable: Pageable? = null
        var savedCommand: SavePokemonFormInput? = null
        var updatedCommand: UpdatePokemonFormInput? = null
        var removedId: Long? = null

        var pageResult: Page<PokemonFormView> = Page(emptyList(), 0, 0)
        lateinit var saveResult: PokemonFormView
        lateinit var updateResult: PokemonFormView

        override fun getPageByCondition(
            specification: PokemonFormSpecification,
            pageable: Pageable,
        ): Page<PokemonFormView> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(command: SavePokemonFormInput): PokemonFormView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdatePokemonFormInput): PokemonFormView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }
    }
}

private fun pokemonFormView(id: Long): PokemonFormView =
    PokemonFormView(
        PokemonForm {
            this.id = id
            backDefault = "https://example.com/back.png"
            backFemale = null
            backShiny = "https://example.com/back-shiny.png"
            backShinyFemale = null
            battleOnly = false
            defaultForm = true
            formName = null
            formOrder = 1
            frontDefault = "https://example.com/front.png"
            frontFemale = null
            frontShiny = "https://example.com/front-shiny.png"
            frontShinyFemale = null
            internalName = "bulbasaur"
            mega = false
            name = "bulbasaur"
            pokemon =
                Pokemon {
                    this.id = 1L
                    internalName = "bulbasaur"
                    name = "bulbasaur"
                }
            sortingOrder = 1
        },
    )

private fun savePokemonFormInput(): SavePokemonFormInput =
    SavePokemonFormInput(
        backDefault = "https://example.com/back.png",
        backFemale = null,
        backShiny = "https://example.com/back-shiny.png",
        backShinyFemale = null,
        battleOnly = false,
        defaultForm = true,
        formName = null,
        formOrder = 1,
        frontDefault = "https://example.com/front.png",
        frontFemale = null,
        frontShiny = "https://example.com/front-shiny.png",
        frontShinyFemale = null,
        internalName = "bulbasaur",
        mega = false,
        name = "bulbasaur",
        pokemonId = "1",
        sortingOrder = 1,
    )

private fun updatePokemonFormInput(): UpdatePokemonFormInput =
    UpdatePokemonFormInput(
        id = "1",
        backDefault = "https://example.com/back.png",
        backFemale = null,
        backShiny = "https://example.com/back-shiny.png",
        backShinyFemale = null,
        battleOnly = false,
        defaultForm = true,
        formName = null,
        formOrder = 1,
        frontDefault = "https://example.com/front.png",
        frontFemale = null,
        frontShiny = "https://example.com/front-shiny.png",
        frontShinyFemale = null,
        internalName = "bulbasaur",
        mega = false,
        name = "bulbasaur",
        pokemonId = "1",
        sortingOrder = 1,
    )
