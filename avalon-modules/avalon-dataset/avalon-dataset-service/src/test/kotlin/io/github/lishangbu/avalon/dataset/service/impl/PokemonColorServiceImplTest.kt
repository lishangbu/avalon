package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.PokemonColor
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonColorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonColorView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonColorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonColorInput
import io.github.lishangbu.avalon.dataset.repository.PokemonColorRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class PokemonColorServiceImplTest {
    private val repository = mock(PokemonColorRepository::class.java)
    private val service = PokemonColorServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = PokemonColorSpecification(id = "1", internalName = "black")
        `when`(repository.listViews(specification)).thenReturn(listOf(pokemonColorView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("black", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<PokemonColor>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            pokemonColorEntity(1L),
        )

        val result = service.save(SavePokemonColorInput("black", "黑色"))

        assertEquals("1", result.id)
        verify(repository).save(any<PokemonColor>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<PokemonColor>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            pokemonColorEntity(1L),
        )

        val result = service.update(UpdatePokemonColorInput("1", "black", "黑色"))

        assertEquals("1", result.id)
        verify(repository).save(any<PokemonColor>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun pokemonColorEntity(id: Long): PokemonColor =
    PokemonColor {
        this.id = id
        internalName = "black"
        name = "黑色"
    }

private fun pokemonColorView(id: Long): PokemonColorView = PokemonColorView(pokemonColorEntity(id))
