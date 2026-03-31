package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.CreatureColor
import io.github.lishangbu.avalon.dataset.entity.CreatureHabitat
import io.github.lishangbu.avalon.dataset.entity.CreatureShape
import io.github.lishangbu.avalon.dataset.entity.CreatureSpecies
import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpeciesSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpeciesView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureSpeciesInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureSpeciesInput
import io.github.lishangbu.avalon.dataset.repository.CreatureSpeciesRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class CreatureSpeciesServiceImplTest {
    private val repository = mock(CreatureSpeciesRepository::class.java)
    private val service = CreatureSpeciesServiceImpl(repository)

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = CreatureSpeciesSpecification(id = "1", internalName = "bulbasaur", growthRateId = "4", creatureColorId = "5")
        val pageable = PageRequest.of(0, 5)
        `when`(repository.pageViews(specification, pageable)).thenReturn(Page(listOf(creatureSpeciesView(1L)), 1, 1))

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals("1", result.rows.first().id)
        assertEquals("bulbasaur", result.rows.first().internalName)
        assertEquals(
            "较慢",
            result.rows
                .first()
                .growthRate
                ?.name,
        )
        assertEquals(
            "绿色",
            result.rows
                .first()
                .creatureColor
                ?.name,
        )
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(
            repository.save(
                any<CreatureSpecies>(),
                eq(SaveMode.INSERT_ONLY),
                eq(AssociatedSaveMode.REPLACE),
                isNull(),
            ),
        ).thenReturn(creatureSpeciesSavedEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(creatureSpeciesView(1L))

        val result = service.save(saveCreatureSpeciesInput())

        assertEquals("1", result.id)
        assertEquals("medium-slow", result.growthRate?.internalName)
        assertEquals("绿色", result.creatureColor?.name)
        verify(repository).save(any<CreatureSpecies>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(
            repository.save(
                any<CreatureSpecies>(),
                eq(SaveMode.UPDATE_ONLY),
                eq(AssociatedSaveMode.REPLACE),
                isNull(),
            ),
        ).thenReturn(creatureSpeciesSavedEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(creatureSpeciesView(1L))

        val result = service.update(updateCreatureSpeciesInput())

        assertEquals("1", result.id)
        assertEquals(false, result.legendary)
        assertEquals("grassland", result.creatureHabitat?.name)
        assertEquals("Quadruped", result.creatureShape?.name)
        verify(repository).save(any<CreatureSpecies>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun saveCreatureSpeciesInput(): SaveCreatureSpeciesInput =
    SaveCreatureSpeciesInput(
        internalName = "bulbasaur",
        name = "妙蛙种子",
        sortingOrder = 1,
        genderRate = 1,
        captureRate = 45,
        baseHappiness = 70,
        baby = false,
        legendary = false,
        mythical = false,
        hatchCounter = 20,
        hasGenderDifferences = false,
        formsSwitchable = false,
        evolvesFromSpeciesId = null,
        evolutionChainId = null,
        growthRateId = "4",
        creatureColorId = "5",
        creatureHabitatId = "3",
        creatureShapeId = "8",
    )

private fun updateCreatureSpeciesInput(): UpdateCreatureSpeciesInput =
    UpdateCreatureSpeciesInput(
        id = "1",
        internalName = "bulbasaur",
        name = "妙蛙种子",
        sortingOrder = 1,
        genderRate = 1,
        captureRate = 45,
        baseHappiness = 70,
        baby = false,
        legendary = false,
        mythical = false,
        hatchCounter = 20,
        hasGenderDifferences = false,
        formsSwitchable = false,
        evolvesFromSpeciesId = null,
        evolutionChainId = null,
        growthRateId = "4",
        creatureColorId = "5",
        creatureHabitatId = "3",
        creatureShapeId = "8",
    )

private fun creatureSpeciesSavedEntity(id: Long): CreatureSpecies =
    CreatureSpecies {
        this.id = id
        internalName = "bulbasaur"
        name = "妙蛙种子"
        sortingOrder = 1
        genderRate = 1
        captureRate = 45
        baseHappiness = 70
        baby = false
        legendary = false
        mythical = false
        hatchCounter = 20
        hasGenderDifferences = false
        formsSwitchable = false
        evolvesFromSpeciesId = null
        evolutionChainId = 1L
        growthRate =
            GrowthRate {
                this.id = 4L
            }
        creatureColor =
            CreatureColor {
                this.id = 5L
            }
        creatureHabitat =
            CreatureHabitat {
                this.id = 3L
            }
        creatureShape =
            CreatureShape {
                this.id = 8L
            }
    }

private fun creatureSpeciesWithAssociations(id: Long): CreatureSpecies =
    CreatureSpecies {
        this.id = id
        internalName = "bulbasaur"
        name = "妙蛙种子"
        sortingOrder = 1
        genderRate = 1
        captureRate = 45
        baseHappiness = 70
        baby = false
        legendary = false
        mythical = false
        hatchCounter = 20
        hasGenderDifferences = false
        formsSwitchable = false
        evolvesFromSpeciesId = null
        evolutionChainId = 1L
        growthRate =
            GrowthRate {
                this.id = 4L
                internalName = "medium-slow"
                name = "较慢"
            }
        creatureColor =
            CreatureColor {
                this.id = 5L
                internalName = "green"
                name = "绿色"
            }
        creatureHabitat =
            CreatureHabitat {
                this.id = 3L
                internalName = "grassland"
                name = "grassland"
            }
        creatureShape =
            CreatureShape {
                this.id = 8L
                internalName = "quadruped"
                name = "Quadruped"
            }
    }

private fun creatureSpeciesView(id: Long): CreatureSpeciesView = CreatureSpeciesView(creatureSpeciesWithAssociations(id))
