package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.EvolutionTrigger
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionTriggerSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionTriggerView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEvolutionTriggerInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEvolutionTriggerInput
import io.github.lishangbu.avalon.dataset.repository.EvolutionTriggerRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class EvolutionTriggerServiceImplTest {
    private val repository = mock(EvolutionTriggerRepository::class.java)
    private val service = EvolutionTriggerServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = EvolutionTriggerSpecification(id = "1", internalName = "level-up")
        `when`(repository.listViews(specification)).thenReturn(listOf(evolutionTriggerView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("level-up", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<EvolutionTrigger>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            evolutionTriggerEntity(1L),
        )

        val result = service.save(SaveEvolutionTriggerInput("level-up", "Level up"))

        assertEquals("1", result.id)
        verify(repository).save(any<EvolutionTrigger>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<EvolutionTrigger>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            evolutionTriggerEntity(1L),
        )

        val result = service.update(UpdateEvolutionTriggerInput("1", "level-up", "Level up"))

        assertEquals("1", result.id)
        verify(repository).save(any<EvolutionTrigger>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun evolutionTriggerEntity(id: Long): EvolutionTrigger =
    EvolutionTrigger {
        this.id = id
        internalName = "level-up"
        name = "Level up"
    }

private fun evolutionTriggerView(id: Long): EvolutionTriggerView = EvolutionTriggerView(evolutionTriggerEntity(id))
