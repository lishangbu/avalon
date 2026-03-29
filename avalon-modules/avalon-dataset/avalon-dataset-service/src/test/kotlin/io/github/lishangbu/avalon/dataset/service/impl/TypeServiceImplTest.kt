package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.SaveTypeInput
import io.github.lishangbu.avalon.dataset.entity.dto.TypeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.TypeView
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateTypeInput
import io.github.lishangbu.avalon.dataset.repository.TypeRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import io.github.lishangbu.avalon.dataset.entity.Type as DatasetType

class TypeServiceImplTest {
    private val repository = mock(TypeRepository::class.java)
    private val service = TypeServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = TypeSpecification(id = "1", internalName = "fire", name = "火", battleOnly = false)
        `when`(repository.listViews(specification)).thenReturn(listOf(typeView(1L, "fire", "火", false)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("火", result.first().name)
        assertEquals(false, result.first().battleOnly)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(
            repository.save(
                any<DatasetType>(),
                eq(SaveMode.INSERT_ONLY),
                eq(AssociatedSaveMode.REPLACE),
                isNull(),
            ),
        ).thenReturn(typeEntity(1L, "fire", "火", false))

        val result = service.save(SaveTypeInput("fire", "火", false))

        assertEquals("1", result.id)
        assertEquals(false, result.battleOnly)
        verify(repository).save(any<DatasetType>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(
            repository.save(
                any<DatasetType>(),
                eq(SaveMode.UPSERT),
                eq(AssociatedSaveMode.REPLACE),
                isNull(),
            ),
        ).thenReturn(typeEntity(1L, "shadow", "暗", true))

        val result = service.update(UpdateTypeInput("1", "shadow", "暗", true))

        assertEquals("1", result.id)
        assertEquals(true, result.battleOnly)
        verify(repository).save(any<DatasetType>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun typeEntity(
    id: Long,
    internalName: String,
    name: String,
    battleOnly: Boolean,
): DatasetType =
    DatasetType {
        this.id = id
        this.internalName = internalName
        this.name = name
        this.battleOnly = battleOnly
    }

private fun typeView(
    id: Long,
    internalName: String,
    name: String,
    battleOnly: Boolean,
): TypeView = TypeView(typeEntity(id, internalName, name, battleOnly))
