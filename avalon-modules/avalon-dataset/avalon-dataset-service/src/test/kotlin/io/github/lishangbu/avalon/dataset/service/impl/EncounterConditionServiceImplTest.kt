package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.EncounterCondition
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterConditionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterConditionInput
import io.github.lishangbu.avalon.dataset.repository.EncounterConditionRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class EncounterConditionServiceImplTest {
    private val repository = mock(EncounterConditionRepository::class.java)
    private val service = EncounterConditionServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = EncounterConditionSpecification(id = "1", internalName = "swarm")
        `when`(repository.listViews(specification)).thenReturn(listOf(encounterConditionView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("swarm", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<EncounterCondition>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            encounterConditionEntity(1L),
        )

        val result = service.save(SaveEncounterConditionInput("swarm", "Swarm"))

        assertEquals("1", result.id)
        verify(repository).save(any<EncounterCondition>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<EncounterCondition>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            encounterConditionEntity(1L),
        )

        val result = service.update(UpdateEncounterConditionInput("1", "swarm", "Swarm"))

        assertEquals("1", result.id)
        verify(repository).save(any<EncounterCondition>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun encounterConditionEntity(id: Long): EncounterCondition =
    EncounterCondition {
        this.id = id
        internalName = "swarm"
        name = "Swarm"
    }

private fun encounterConditionView(id: Long): EncounterConditionView = EncounterConditionView(encounterConditionEntity(id))
