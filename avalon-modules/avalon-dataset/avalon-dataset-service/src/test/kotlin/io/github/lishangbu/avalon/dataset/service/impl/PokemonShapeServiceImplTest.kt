package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.PokemonShape
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonShapeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonShapeView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonShapeInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonShapeInput
import io.github.lishangbu.avalon.dataset.repository.PokemonShapeRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class PokemonShapeServiceImplTest {
    private val repository = mock(PokemonShapeRepository::class.java)
    private val service = PokemonShapeServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = PokemonShapeSpecification(id = "1", internalName = "ball")
        `when`(repository.listViews(specification)).thenReturn(listOf(pokemonShapeView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("ball", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<PokemonShape>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            pokemonShapeEntity(1L),
        )

        val result = service.save(SavePokemonShapeInput("ball", "Ball"))

        assertEquals("1", result.id)
        verify(repository).save(any<PokemonShape>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<PokemonShape>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            pokemonShapeEntity(1L),
        )

        val result = service.update(UpdatePokemonShapeInput("1", "ball", "Ball"))

        assertEquals("1", result.id)
        verify(repository).save(any<PokemonShape>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun pokemonShapeEntity(id: Long): PokemonShape =
    PokemonShape {
        this.id = id
        internalName = "ball"
        name = "Ball"
    }

private fun pokemonShapeView(id: Long): PokemonShapeView = PokemonShapeView(pokemonShapeEntity(id))
