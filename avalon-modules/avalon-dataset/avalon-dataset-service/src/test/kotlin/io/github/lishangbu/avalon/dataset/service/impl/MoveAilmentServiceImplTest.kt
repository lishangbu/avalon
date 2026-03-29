package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.MoveAilment
import io.github.lishangbu.avalon.dataset.entity.dto.MoveAilmentSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveAilmentView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveAilmentInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveAilmentInput
import io.github.lishangbu.avalon.dataset.repository.MoveAilmentRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class MoveAilmentServiceImplTest {
    private val repository = mock(MoveAilmentRepository::class.java)
    private val service = MoveAilmentServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = MoveAilmentSpecification(id = "1", internalName = "paralysis")
        `when`(repository.listViews(specification)).thenReturn(listOf(moveAilmentView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("paralysis", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<MoveAilment>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            moveAilmentEntity(1L),
        )

        val result = service.save(SaveMoveAilmentInput("paralysis", "paralysis"))

        assertEquals("1", result.id)
        verify(repository).save(any<MoveAilment>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<MoveAilment>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            moveAilmentEntity(1L),
        )

        val result = service.update(UpdateMoveAilmentInput("1", "paralysis", "paralysis"))

        assertEquals("1", result.id)
        verify(repository).save(any<MoveAilment>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun moveAilmentEntity(id: Long): MoveAilment =
    MoveAilment {
        this.id = id
        internalName = "paralysis"
        name = "paralysis"
    }

private fun moveAilmentView(id: Long): MoveAilmentView = MoveAilmentView(moveAilmentEntity(id))
