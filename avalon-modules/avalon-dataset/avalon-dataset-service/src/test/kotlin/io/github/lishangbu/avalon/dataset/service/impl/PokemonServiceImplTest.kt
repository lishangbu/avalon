package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Pokemon
import io.github.lishangbu.avalon.dataset.entity.PokemonSpecies
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonInput
import io.github.lishangbu.avalon.dataset.repository.PokemonRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class PokemonServiceImplTest {
    private val repository = mock(PokemonRepository::class.java)
    private val service = PokemonServiceImpl(repository)

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = PokemonSpecification(id = "1", internalName = "bulbasaur", pokemonSpeciesId = "1")
        val pageable = PageRequest.of(0, 5)
        `when`(repository.pageViews(specification, pageable)).thenReturn(Page(listOf(pokemonView(1L)), 1, 1))

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals("1", result.rows.first().id)
        assertEquals("bulbasaur", result.rows.first().internalName)
        assertEquals(
            "妙蛙种子",
            result.rows
                .first()
                .pokemonSpecies
                ?.name,
        )
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<Pokemon>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            pokemonSavedEntity(1L),
        )
        `when`(repository.loadViewById(1L)).thenReturn(pokemonView(1L))

        val result = service.save(savePokemonInput())

        assertEquals("1", result.id)
        assertEquals("bulbasaur", result.name)
        assertEquals("bulbasaur", result.pokemonSpecies?.internalName)
        verify(repository).save(any<Pokemon>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.save(any<Pokemon>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            pokemonSavedEntity(1L),
        )
        `when`(repository.loadViewById(1L)).thenReturn(pokemonView(1L))

        val result = service.update(updatePokemonInput())

        assertEquals("1", result.id)
        assertEquals(64, result.baseExperience)
        assertEquals("妙蛙种子", result.pokemonSpecies?.name)
        verify(repository).save(any<Pokemon>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun savePokemonInput(): SavePokemonInput =
    SavePokemonInput(
        internalName = "bulbasaur",
        name = "bulbasaur",
        height = 7,
        weight = 69,
        baseExperience = 64,
        sortingOrder = 1,
        pokemonSpeciesId = "1",
    )

private fun updatePokemonInput(): UpdatePokemonInput =
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

private fun pokemonSavedEntity(id: Long): Pokemon =
    Pokemon {
        this.id = id
        internalName = "bulbasaur"
        name = "bulbasaur"
        height = 7
        weight = 69
        baseExperience = 64
        sortingOrder = 1
        pokemonSpecies =
            PokemonSpecies {
                this.id = 1L
            }
    }

private fun pokemonWithAssociations(id: Long): Pokemon =
    Pokemon {
        this.id = id
        internalName = "bulbasaur"
        name = "bulbasaur"
        height = 7
        weight = 69
        baseExperience = 64
        sortingOrder = 1
        pokemonSpecies =
            PokemonSpecies {
                this.id = 1L
                internalName = "bulbasaur"
                name = "妙蛙种子"
            }
    }

private fun pokemonView(id: Long): PokemonView = PokemonView(pokemonWithAssociations(id))
