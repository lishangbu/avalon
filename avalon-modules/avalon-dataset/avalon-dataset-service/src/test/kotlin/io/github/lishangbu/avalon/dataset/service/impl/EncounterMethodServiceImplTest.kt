package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.EncounterMethod
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterMethodSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterMethodView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterMethodInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterMethodInput
import io.github.lishangbu.avalon.dataset.repository.EncounterMethodRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class EncounterMethodServiceImplTest {
    private val repository = mock(EncounterMethodRepository::class.java)
    private val service = EncounterMethodServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = EncounterMethodSpecification(id = "1", internalName = "walk", sortingOrder = 1)
        `when`(repository.listViews(specification)).thenReturn(listOf(encounterMethodView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("walk", result.first().internalName)
        assertEquals(1, result.first().sortingOrder)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<EncounterMethod>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            encounterMethodEntity(1L),
        )

        val result = service.save(SaveEncounterMethodInput("walk", "Walking in tall grass or a cave", 1))

        assertEquals("1", result.id)
        verify(repository).save(any<EncounterMethod>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<EncounterMethod>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            encounterMethodEntity(1L),
        )

        val result = service.update(UpdateEncounterMethodInput("1", "walk", "Walking in tall grass or a cave", 1))

        assertEquals("1", result.id)
        verify(repository).save(any<EncounterMethod>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun encounterMethodEntity(id: Long): EncounterMethod =
    EncounterMethod {
        this.id = id
        internalName = "walk"
        name = "Walking in tall grass or a cave"
        sortingOrder = 1
    }

private fun encounterMethodView(id: Long): EncounterMethodView = EncounterMethodView(encounterMethodEntity(id))
