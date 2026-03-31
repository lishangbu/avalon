package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Creature
import io.github.lishangbu.avalon.dataset.entity.CreatureSpecies
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureInput
import io.github.lishangbu.avalon.dataset.repository.CreatureRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class CreatureServiceImplTest {
    private val repository = mock(CreatureRepository::class.java)
    private val service = CreatureServiceImpl(repository)

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = CreatureSpecification(id = "1", internalName = "bulbasaur", creatureSpeciesId = "1")
        val pageable = PageRequest.of(0, 5)
        `when`(repository.pageViews(specification, pageable)).thenReturn(Page(listOf(creatureView(1L)), 1, 1))

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals("1", result.rows.first().id)
        assertEquals("bulbasaur", result.rows.first().internalName)
        assertEquals(
            "妙蛙种子",
            result.rows
                .first()
                .creatureSpecies
                ?.name,
        )
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<Creature>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            creatureSavedEntity(1L),
        )
        `when`(repository.loadViewById(1L)).thenReturn(creatureView(1L))

        val result = service.save(saveCreatureInput())

        assertEquals("1", result.id)
        assertEquals("bulbasaur", result.name)
        assertEquals("bulbasaur", result.creatureSpecies?.internalName)
        verify(repository).save(any<Creature>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.save(any<Creature>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            creatureSavedEntity(1L),
        )
        `when`(repository.loadViewById(1L)).thenReturn(creatureView(1L))

        val result = service.update(updateCreatureInput())

        assertEquals("1", result.id)
        assertEquals(64, result.baseExperience)
        assertEquals("妙蛙种子", result.creatureSpecies?.name)
        verify(repository).save(any<Creature>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun saveCreatureInput(): SaveCreatureInput =
    SaveCreatureInput(
        internalName = "bulbasaur",
        name = "bulbasaur",
        height = 7,
        weight = 69,
        baseExperience = 64,
        sortingOrder = 1,
        creatureSpeciesId = "1",
    )

private fun updateCreatureInput(): UpdateCreatureInput =
    UpdateCreatureInput(
        id = "1",
        internalName = "bulbasaur",
        name = "bulbasaur",
        height = 7,
        weight = 69,
        baseExperience = 64,
        sortingOrder = 1,
        creatureSpeciesId = "1",
    )

private fun creatureSavedEntity(id: Long): Creature =
    Creature {
        this.id = id
        internalName = "bulbasaur"
        name = "bulbasaur"
        height = 7
        weight = 69
        baseExperience = 64
        sortingOrder = 1
        creatureSpecies =
            CreatureSpecies {
                this.id = 1L
            }
    }

private fun creatureWithAssociations(id: Long): Creature =
    Creature {
        this.id = id
        internalName = "bulbasaur"
        name = "bulbasaur"
        height = 7
        weight = 69
        baseExperience = 64
        sortingOrder = 1
        creatureSpecies =
            CreatureSpecies {
                this.id = 1L
                internalName = "bulbasaur"
                name = "妙蛙种子"
            }
    }

private fun creatureView(id: Long): CreatureView = CreatureView(creatureWithAssociations(id))
