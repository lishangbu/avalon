package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.EncounterCondition
import io.github.lishangbu.avalon.dataset.entity.EncounterConditionValue
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionValueSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionValueView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterConditionValueInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterConditionValueInput
import io.github.lishangbu.avalon.dataset.repository.EncounterConditionValueRepository
import io.github.lishangbu.avalon.dataset.repository.LocationAreaEncounterConditionValueRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class EncounterConditionValueServiceImplTest {
    private val repository = mock(EncounterConditionValueRepository::class.java)
    private val locationAreaEncounterConditionValueRepository = mock(LocationAreaEncounterConditionValueRepository::class.java)
    private val service = EncounterConditionValueServiceImpl(repository, locationAreaEncounterConditionValueRepository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = EncounterConditionValueSpecification(id = "1", internalName = "swarm-yes")
        `when`(repository.listViews(specification)).thenReturn(listOf(encounterConditionValueView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("swarm", result.first().encounterCondition?.internalName)
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<EncounterConditionValue>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            encounterConditionValueSavedEntity(1L),
        )
        `when`(repository.loadViewById(1L)).thenReturn(encounterConditionValueView(1L))

        val result =
            service.save(
                SaveEncounterConditionValueInput(
                    internalName = "swarm-yes",
                    name = "During a swarm",
                    encounterConditionId = "1",
                ),
            )

        assertEquals("1", result.id)
        assertEquals("Swarm", result.encounterCondition?.name)
        verify(repository).save(any<EncounterConditionValue>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.save(any<EncounterConditionValue>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(
            encounterConditionValueSavedEntity(1L),
        )
        `when`(repository.loadViewById(1L)).thenReturn(encounterConditionValueView(1L))

        val result =
            service.update(
                UpdateEncounterConditionValueInput(
                    id = "1",
                    internalName = "swarm-yes",
                    name = "During a swarm",
                    encounterConditionId = "1",
                ),
            )

        assertEquals("1", result.id)
        assertEquals("swarm", result.encounterCondition?.internalName)
        verify(repository).save(any<EncounterConditionValue>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun removeById_callsRepository() {
        `when`(locationAreaEncounterConditionValueRepository.existsByEncounterConditionValueId(1L)).thenReturn(false)

        service.removeById(1L)

        verify(repository).deleteById(1L)
    }

    @Test
    fun removeById_throwsFriendlyMessageWhenReferencedByLocationAreaEncounter() {
        `when`(locationAreaEncounterConditionValueRepository.existsByEncounterConditionValueId(1L)).thenReturn(true)

        val error =
            assertThrows(IllegalStateException::class.java) {
                service.removeById(1L)
            }

        assertEquals("当前遭遇条件值已被地点区域遭遇引用，请先删除相关遭遇引用后再删除当前遭遇条件值", error.message)
        verify(repository, never()).deleteById(1L)
    }
}

private fun encounterConditionValueSavedEntity(id: Long): EncounterConditionValue =
    EncounterConditionValue {
        this.id = id
        internalName = "swarm-yes"
        name = "During a swarm"
        encounterCondition =
            EncounterCondition {
                this.id = 1L
            }
    }

private fun encounterConditionValueWithAssociation(id: Long): EncounterConditionValue =
    EncounterConditionValue {
        this.id = id
        internalName = "swarm-yes"
        name = "During a swarm"
        encounterCondition =
            EncounterCondition {
                this.id = 1L
                internalName = "swarm"
                name = "Swarm"
            }
    }

private fun encounterConditionValueView(id: Long): EncounterConditionValueView = EncounterConditionValueView(encounterConditionValueWithAssociation(id))
