package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Pokemon
import io.github.lishangbu.avalon.dataset.entity.PokemonForm
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonFormSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonFormView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonFormInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonFormInput
import io.github.lishangbu.avalon.dataset.repository.PokemonFormRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class PokemonFormServiceImplTest {
    private val repository = mock(PokemonFormRepository::class.java)
    private val service = PokemonFormServiceImpl(repository)

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = PokemonFormSpecification(id = "1", internalName = "bulbasaur", pokemonId = "1")
        val pageable = PageRequest.of(0, 5)
        `when`(repository.pageViews(specification, pageable)).thenReturn(Page(listOf(pokemonFormView(1L)), 1, 1))

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals(true, result.rows.first().defaultForm)
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<PokemonForm>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(pokemonFormEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(pokemonFormView(1L))

        val result = service.save(savePokemonFormInput())

        assertEquals("1", result.id)
        assertEquals("bulbasaur", result.pokemon?.internalName)
        verify(repository).save(any<PokemonForm>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.save(any<PokemonForm>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(pokemonFormEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(pokemonFormView(1L))

        val result = service.update(updatePokemonFormInput())

        assertEquals(false, result.battleOnly)
        assertEquals(false, result.mega)
        verify(repository).save(any<PokemonForm>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

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

private fun pokemonFormEntity(id: Long): PokemonForm =
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
            }
        sortingOrder = 1
    }

private fun pokemonFormWithPokemon(id: Long): PokemonForm =
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
    }

private fun pokemonFormView(id: Long): PokemonFormView = PokemonFormView(pokemonFormWithPokemon(id))
