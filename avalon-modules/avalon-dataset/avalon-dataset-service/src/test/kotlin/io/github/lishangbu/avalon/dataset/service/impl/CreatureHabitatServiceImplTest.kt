package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.CreatureHabitat
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureHabitatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureHabitatView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureHabitatInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureHabitatInput
import io.github.lishangbu.avalon.dataset.repository.CreatureHabitatRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class CreatureHabitatServiceImplTest {
    private val repository = mock(CreatureHabitatRepository::class.java)
    private val service = CreatureHabitatServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = CreatureHabitatSpecification(id = "1", internalName = "cave")
        `when`(repository.listViews(specification)).thenReturn(listOf(creatureHabitatView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("cave", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<CreatureHabitat>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            creatureHabitatEntity(1L),
        )

        val result = service.save(SaveCreatureHabitatInput("cave", "cave"))

        assertEquals("1", result.id)
        verify(repository).save(any<CreatureHabitat>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<CreatureHabitat>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            creatureHabitatEntity(1L),
        )

        val result = service.update(UpdateCreatureHabitatInput("1", "cave", "cave"))

        assertEquals("1", result.id)
        verify(repository).save(any<CreatureHabitat>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun creatureHabitatEntity(id: Long): CreatureHabitat =
    CreatureHabitat {
        this.id = id
        internalName = "cave"
        name = "cave"
    }

private fun creatureHabitatView(id: Long): CreatureHabitatView = CreatureHabitatView(creatureHabitatEntity(id))
