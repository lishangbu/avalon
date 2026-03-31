package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.CreatureEvolution
import io.github.lishangbu.avalon.dataset.entity.CreatureSpecies
import io.github.lishangbu.avalon.dataset.entity.EvolutionChain
import io.github.lishangbu.avalon.dataset.entity.EvolutionTrigger
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureEvolutionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureEvolutionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureEvolutionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureEvolutionInput
import io.github.lishangbu.avalon.dataset.repository.CreatureEvolutionRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class CreatureEvolutionServiceImplTest {
    private val repository = mock(CreatureEvolutionRepository::class.java)
    private val service = CreatureEvolutionServiceImpl(repository)

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = CreatureEvolutionSpecification(evolutionChainId = "1", fromCreatureSpeciesId = "1", toCreatureSpeciesId = "2")
        val pageable = PageRequest.of(0, 5)
        `when`(repository.pageViews(specification, pageable)).thenReturn(Page(listOf(creatureEvolutionView(1L)), 1, 1))

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals(16, result.rows.first().minLevel)
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<CreatureEvolution>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(creatureEvolutionEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(creatureEvolutionView(1L))

        val result = service.save(saveCreatureEvolutionInput())

        assertEquals("1", result.id)
        assertEquals("1", result.evolutionChain?.id)
        verify(repository).save(any<CreatureEvolution>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.save(any<CreatureEvolution>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(creatureEvolutionEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(creatureEvolutionView(1L))

        val result = service.update(updateCreatureEvolutionInput())

        assertEquals("level-up", result.trigger?.internalName)
        verify(repository).save(any<CreatureEvolution>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun saveCreatureEvolutionInput(): SaveCreatureEvolutionInput =
    SaveCreatureEvolutionInput(
        branchSortOrder = 1,
        detailSortOrder = 1,
        needsMultiplayer = false,
        needsOverworldRain = false,
        turnUpsideDown = false,
        timeOfDay = null,
        minAffection = null,
        minBeauty = null,
        minDamageTaken = null,
        minHappiness = null,
        minLevel = 16,
        minMoveCount = null,
        minSteps = null,
        relativePhysicalStats = null,
        genderId = null,
        baseVariantId = null,
        regionId = null,
        evolutionChainId = "1",
        fromCreatureSpeciesId = "1",
        toCreatureSpeciesId = "2",
        heldItemId = null,
        itemId = null,
        knownMoveId = null,
        knownMoveTypeId = null,
        locationId = null,
        partyCreatureSpeciesId = null,
        partyTypeId = null,
        tradeCreatureSpeciesId = null,
        triggerId = "1",
        usedMoveId = null,
    )

private fun updateCreatureEvolutionInput(): UpdateCreatureEvolutionInput =
    UpdateCreatureEvolutionInput(
        id = "1",
        branchSortOrder = 1,
        detailSortOrder = 1,
        needsMultiplayer = false,
        needsOverworldRain = false,
        turnUpsideDown = false,
        timeOfDay = null,
        minAffection = null,
        minBeauty = null,
        minDamageTaken = null,
        minHappiness = null,
        minLevel = 16,
        minMoveCount = null,
        minSteps = null,
        relativePhysicalStats = null,
        genderId = null,
        baseVariantId = null,
        regionId = null,
        evolutionChainId = "1",
        fromCreatureSpeciesId = "1",
        toCreatureSpeciesId = "2",
        heldItemId = null,
        itemId = null,
        knownMoveId = null,
        knownMoveTypeId = null,
        locationId = null,
        partyCreatureSpeciesId = null,
        partyTypeId = null,
        tradeCreatureSpeciesId = null,
        triggerId = "1",
        usedMoveId = null,
    )

private fun creatureEvolutionEntity(id: Long): CreatureEvolution =
    CreatureEvolution {
        this.id = id
        branchSortOrder = 1
        detailSortOrder = 1
        needsMultiplayer = false
        needsOverworldRain = false
        turnUpsideDown = false
        timeOfDay = null
        minAffection = null
        minBeauty = null
        minDamageTaken = null
        minHappiness = null
        minLevel = 16
        minMoveCount = null
        minSteps = null
        relativePhysicalStats = null
        evolutionChain =
            EvolutionChain {
                this.id = 1L
            }
        fromCreatureSpecies =
            CreatureSpecies {
                this.id = 1L
            }
        toCreatureSpecies =
            CreatureSpecies {
                this.id = 2L
            }
        trigger =
            EvolutionTrigger {
                this.id = 1L
            }
    }

private fun creatureEvolutionView(id: Long): CreatureEvolutionView =
    CreatureEvolutionView(
        CreatureEvolution {
            this.id = id
            branchSortOrder = 1
            detailSortOrder = 1
            needsMultiplayer = false
            needsOverworldRain = false
            turnUpsideDown = false
            timeOfDay = null
            minAffection = null
            minBeauty = null
            minDamageTaken = null
            minHappiness = null
            minLevel = 16
            minMoveCount = null
            minSteps = null
            relativePhysicalStats = null
            evolutionChain =
                EvolutionChain {
                    this.id = 1L
                }
            fromCreatureSpecies =
                CreatureSpecies {
                    this.id = 1L
                    internalName = "bulbasaur"
                    name = "妙蛙种子"
                }
            toCreatureSpecies =
                CreatureSpecies {
                    this.id = 2L
                    internalName = "ivysaur"
                    name = "妙蛙草"
                }
            trigger =
                EvolutionTrigger {
                    this.id = 1L
                    internalName = "level-up"
                    name = "Level up"
                }
        },
    )
