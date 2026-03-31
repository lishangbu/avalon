package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.MoveCategory
import io.github.lishangbu.avalon.dataset.entity.dto.MoveCategorySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveCategoryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveCategoryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveCategoryInput
import io.github.lishangbu.avalon.dataset.repository.MoveCategoryRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class MoveCategoryServiceImplTest {
    private val repository = mock(MoveCategoryRepository::class.java)
    private val service = MoveCategoryServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = MoveCategorySpecification(id = "0", internalName = "damage")
        `when`(repository.listViews(specification)).thenReturn(listOf(moveCategoryView(0L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("0", result.first().id)
        assertEquals("damage", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<MoveCategory>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            moveCategoryEntity(1L),
        )

        val result = service.save(SaveMoveCategoryInput("damage", "damage", "Inflicts damage"))

        assertEquals("1", result.id)
        verify(repository).save(any<MoveCategory>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<MoveCategory>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            moveCategoryEntity(1L),
        )

        val result = service.update(UpdateMoveCategoryInput("1", "damage", "damage", "Inflicts damage"))

        assertEquals("1", result.id)
        verify(repository).save(any<MoveCategory>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun moveCategoryEntity(id: Long): MoveCategory =
    MoveCategory {
        this.id = id
        internalName = "damage"
        name = "damage"
        description = "Inflicts damage"
    }

private fun moveCategoryView(id: Long): MoveCategoryView = MoveCategoryView(moveCategoryEntity(id))
