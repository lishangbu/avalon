package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.PokemonHabitat
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonHabitatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonHabitatView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonHabitatInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonHabitatInput
import io.github.lishangbu.avalon.dataset.repository.PokemonHabitatRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class PokemonHabitatServiceImplTest {
    private val repository = mock(PokemonHabitatRepository::class.java)
    private val service = PokemonHabitatServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = PokemonHabitatSpecification(id = "1", internalName = "cave")
        `when`(repository.listViews(specification)).thenReturn(listOf(pokemonHabitatView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("cave", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<PokemonHabitat>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            pokemonHabitatEntity(1L),
        )

        val result = service.save(SavePokemonHabitatInput("cave", "cave"))

        assertEquals("1", result.id)
        verify(repository).save(any<PokemonHabitat>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<PokemonHabitat>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            pokemonHabitatEntity(1L),
        )

        val result = service.update(UpdatePokemonHabitatInput("1", "cave", "cave"))

        assertEquals("1", result.id)
        verify(repository).save(any<PokemonHabitat>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun pokemonHabitatEntity(id: Long): PokemonHabitat =
    PokemonHabitat {
        this.id = id
        internalName = "cave"
        name = "cave"
    }

private fun pokemonHabitatView(id: Long): PokemonHabitatView = PokemonHabitatView(pokemonHabitatEntity(id))
