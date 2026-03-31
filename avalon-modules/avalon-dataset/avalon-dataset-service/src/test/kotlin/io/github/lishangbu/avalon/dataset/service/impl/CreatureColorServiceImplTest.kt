package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.CreatureColor
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureColorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureColorView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureColorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureColorInput
import io.github.lishangbu.avalon.dataset.repository.CreatureColorRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class CreatureColorServiceImplTest {
    private val repository = mock(CreatureColorRepository::class.java)
    private val service = CreatureColorServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = CreatureColorSpecification(id = "1", internalName = "black")
        `when`(repository.listViews(specification)).thenReturn(listOf(creatureColorView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("black", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<CreatureColor>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            creatureColorEntity(1L),
        )

        val result = service.save(SaveCreatureColorInput("black", "黑色"))

        assertEquals("1", result.id)
        verify(repository).save(any<CreatureColor>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<CreatureColor>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            creatureColorEntity(1L),
        )

        val result = service.update(UpdateCreatureColorInput("1", "black", "黑色"))

        assertEquals("1", result.id)
        verify(repository).save(any<CreatureColor>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun creatureColorEntity(id: Long): CreatureColor =
    CreatureColor {
        this.id = id
        internalName = "black"
        name = "黑色"
    }

private fun creatureColorView(id: Long): CreatureColorView = CreatureColorView(creatureColorEntity(id))
