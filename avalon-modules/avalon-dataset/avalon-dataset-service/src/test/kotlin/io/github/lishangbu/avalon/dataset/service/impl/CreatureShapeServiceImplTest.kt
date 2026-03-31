package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.CreatureShape
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureShapeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureShapeView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureShapeInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureShapeInput
import io.github.lishangbu.avalon.dataset.repository.CreatureShapeRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class CreatureShapeServiceImplTest {
    private val repository = mock(CreatureShapeRepository::class.java)
    private val service = CreatureShapeServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = CreatureShapeSpecification(id = "1", internalName = "ball")
        `when`(repository.listViews(specification)).thenReturn(listOf(creatureShapeView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("ball", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<CreatureShape>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            creatureShapeEntity(1L),
        )

        val result = service.save(SaveCreatureShapeInput("ball", "Ball"))

        assertEquals("1", result.id)
        verify(repository).save(any<CreatureShape>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<CreatureShape>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            creatureShapeEntity(1L),
        )

        val result = service.update(UpdateCreatureShapeInput("1", "ball", "Ball"))

        assertEquals("1", result.id)
        verify(repository).save(any<CreatureShape>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun creatureShapeEntity(id: Long): CreatureShape =
    CreatureShape {
        this.id = id
        internalName = "ball"
        name = "Ball"
    }

private fun creatureShapeView(id: Long): CreatureShapeView = CreatureShapeView(creatureShapeEntity(id))
