package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.MoveLearnMethod
import io.github.lishangbu.avalon.dataset.entity.dto.MoveLearnMethodSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveLearnMethodView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveLearnMethodInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveLearnMethodInput
import io.github.lishangbu.avalon.dataset.repository.MoveLearnMethodRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class MoveLearnMethodServiceImplTest {
    private val repository = mock(MoveLearnMethodRepository::class.java)
    private val service = MoveLearnMethodServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = MoveLearnMethodSpecification(id = "1", internalName = "level-up")
        `when`(repository.listViews(specification)).thenReturn(listOf(moveLearnMethodView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("level-up", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<MoveLearnMethod>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            moveLearnMethodEntity(1L),
        )

        val result = service.save(SaveMoveLearnMethodInput("level-up", "Level up", "Learned when a Pokemon reaches a certain level."))

        assertEquals("1", result.id)
        verify(repository).save(any<MoveLearnMethod>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<MoveLearnMethod>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            moveLearnMethodEntity(1L),
        )

        val result =
            service.update(
                UpdateMoveLearnMethodInput("1", "level-up", "Level up", "Learned when a Pokemon reaches a certain level."),
            )

        assertEquals("1", result.id)
        verify(repository).save(any<MoveLearnMethod>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun moveLearnMethodEntity(id: Long): MoveLearnMethod =
    MoveLearnMethod {
        this.id = id
        internalName = "level-up"
        name = "Level up"
        description = "Learned when a Pokemon reaches a certain level."
    }

private fun moveLearnMethodView(id: Long): MoveLearnMethodView = MoveLearnMethodView(moveLearnMethodEntity(id))
