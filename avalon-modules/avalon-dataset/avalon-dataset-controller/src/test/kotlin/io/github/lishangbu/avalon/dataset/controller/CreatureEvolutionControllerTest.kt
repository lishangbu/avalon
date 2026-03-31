package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.CreatureEvolution
import io.github.lishangbu.avalon.dataset.entity.CreatureSpecies
import io.github.lishangbu.avalon.dataset.entity.EvolutionChain
import io.github.lishangbu.avalon.dataset.entity.EvolutionTrigger
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureEvolutionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureEvolutionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureEvolutionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureEvolutionInput
import io.github.lishangbu.avalon.dataset.service.CreatureEvolutionService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class CreatureEvolutionControllerTest {
    @Test
    fun getCreatureEvolutionPage_delegatesToService() {
        val service = FakeCreatureEvolutionService()
        val controller = CreatureEvolutionController(service)
        val pageable = PageRequest.of(0, 5)
        val page: Page<CreatureEvolutionView> = Page(listOf(creatureEvolutionView(1L)), 1, 1)
        service.pageResult = page
        val specification = CreatureEvolutionSpecification(evolutionChainId = "1", fromCreatureSpeciesId = "1", toCreatureSpeciesId = "2")

        val result = controller.getCreatureEvolutionPage(pageable = pageable, specification = specification)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("1", service.pageCondition!!.evolutionChainId)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeCreatureEvolutionService()
        val controller = CreatureEvolutionController(service)
        val command = saveCreatureEvolutionInput()
        service.saveResult = creatureEvolutionView(1L)

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeCreatureEvolutionService()
        val controller = CreatureEvolutionController(service)
        val command = updateCreatureEvolutionInput()
        service.updateResult = creatureEvolutionView(1L)

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeCreatureEvolutionService()
        val controller = CreatureEvolutionController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeCreatureEvolutionService : CreatureEvolutionService {
        var pageCondition: CreatureEvolutionSpecification? = null
        var pageable: Pageable? = null
        var savedCommand: SaveCreatureEvolutionInput? = null
        var updatedCommand: UpdateCreatureEvolutionInput? = null
        var removedId: Long? = null

        var pageResult: Page<CreatureEvolutionView> = Page(emptyList(), 0, 0)
        lateinit var saveResult: CreatureEvolutionView
        lateinit var updateResult: CreatureEvolutionView

        override fun getPageByCondition(
            specification: CreatureEvolutionSpecification,
            pageable: Pageable,
        ): Page<CreatureEvolutionView> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(command: SaveCreatureEvolutionInput): CreatureEvolutionView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateCreatureEvolutionInput): CreatureEvolutionView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }
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
