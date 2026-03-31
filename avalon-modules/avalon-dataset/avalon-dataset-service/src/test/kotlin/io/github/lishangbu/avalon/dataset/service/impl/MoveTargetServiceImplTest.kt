package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.MoveTarget
import io.github.lishangbu.avalon.dataset.entity.dto.MoveTargetSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveTargetView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveTargetInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveTargetInput
import io.github.lishangbu.avalon.dataset.repository.MoveTargetRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class MoveTargetServiceImplTest {
    private val repository = mock(MoveTargetRepository::class.java)
    private val service = MoveTargetServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = MoveTargetSpecification(id = "1", internalName = "specific-move")
        `when`(repository.listViews(specification)).thenReturn(listOf(moveTargetView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("specific-move", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<MoveTarget>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            moveTargetEntity(1L),
        )

        val result = service.save(SaveMoveTargetInput("specific-move", "specific-move", "One specific move."))

        assertEquals("1", result.id)
        verify(repository).save(any<MoveTarget>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<MoveTarget>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            moveTargetEntity(1L),
        )

        val result = service.update(UpdateMoveTargetInput("1", "specific-move", "specific-move", "One specific move."))

        assertEquals("1", result.id)
        verify(repository).save(any<MoveTarget>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun moveTargetEntity(id: Long): MoveTarget =
    MoveTarget {
        this.id = id
        internalName = "specific-move"
        name = "specific-move"
        description = "One specific move."
    }

private fun moveTargetView(id: Long): MoveTargetView = MoveTargetView(moveTargetEntity(id))
