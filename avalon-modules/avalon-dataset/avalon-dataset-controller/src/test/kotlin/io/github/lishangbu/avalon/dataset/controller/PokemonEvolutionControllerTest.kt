package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.EvolutionChain
import io.github.lishangbu.avalon.dataset.entity.EvolutionTrigger
import io.github.lishangbu.avalon.dataset.entity.PokemonEvolution
import io.github.lishangbu.avalon.dataset.entity.PokemonSpecies
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonEvolutionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonEvolutionView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonEvolutionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonEvolutionInput
import io.github.lishangbu.avalon.dataset.service.PokemonEvolutionService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class PokemonEvolutionControllerTest {
    @Test
    fun getPokemonEvolutionPage_delegatesToService() {
        val service = FakePokemonEvolutionService()
        val controller = PokemonEvolutionController(service)
        val pageable = PageRequest.of(0, 5)
        val page: Page<PokemonEvolutionView> = Page(listOf(pokemonEvolutionView(1L)), 1, 1)
        service.pageResult = page
        val specification = PokemonEvolutionSpecification(evolutionChainId = "1", fromPokemonSpeciesId = "1", toPokemonSpeciesId = "2")

        val result = controller.getPokemonEvolutionPage(pageable = pageable, specification = specification)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("1", service.pageCondition!!.evolutionChainId)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakePokemonEvolutionService()
        val controller = PokemonEvolutionController(service)
        val command = savePokemonEvolutionInput()
        service.saveResult = pokemonEvolutionView(1L)

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakePokemonEvolutionService()
        val controller = PokemonEvolutionController(service)
        val command = updatePokemonEvolutionInput()
        service.updateResult = pokemonEvolutionView(1L)

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakePokemonEvolutionService()
        val controller = PokemonEvolutionController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakePokemonEvolutionService : PokemonEvolutionService {
        var pageCondition: PokemonEvolutionSpecification? = null
        var pageable: Pageable? = null
        var savedCommand: SavePokemonEvolutionInput? = null
        var updatedCommand: UpdatePokemonEvolutionInput? = null
        var removedId: Long? = null

        var pageResult: Page<PokemonEvolutionView> = Page(emptyList(), 0, 0)
        lateinit var saveResult: PokemonEvolutionView
        lateinit var updateResult: PokemonEvolutionView

        override fun getPageByCondition(
            specification: PokemonEvolutionSpecification,
            pageable: Pageable,
        ): Page<PokemonEvolutionView> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(command: SavePokemonEvolutionInput): PokemonEvolutionView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdatePokemonEvolutionInput): PokemonEvolutionView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }
    }
}

private fun pokemonEvolutionView(id: Long): PokemonEvolutionView =
    PokemonEvolutionView(
        PokemonEvolution {
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
            fromPokemonSpecies =
                PokemonSpecies {
                    this.id = 1L
                    internalName = "bulbasaur"
                    name = "妙蛙种子"
                }
            toPokemonSpecies =
                PokemonSpecies {
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

private fun savePokemonEvolutionInput(): SavePokemonEvolutionInput =
    SavePokemonEvolutionInput(
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
        baseFormId = null,
        regionId = null,
        evolutionChainId = "1",
        fromPokemonSpeciesId = "1",
        toPokemonSpeciesId = "2",
        heldItemId = null,
        itemId = null,
        knownMoveId = null,
        knownMoveTypeId = null,
        locationId = null,
        partySpeciesId = null,
        partyTypeId = null,
        tradeSpeciesId = null,
        triggerId = "1",
        usedMoveId = null,
    )

private fun updatePokemonEvolutionInput(): UpdatePokemonEvolutionInput =
    UpdatePokemonEvolutionInput(
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
        baseFormId = null,
        regionId = null,
        evolutionChainId = "1",
        fromPokemonSpeciesId = "1",
        toPokemonSpeciesId = "2",
        heldItemId = null,
        itemId = null,
        knownMoveId = null,
        knownMoveTypeId = null,
        locationId = null,
        partySpeciesId = null,
        partyTypeId = null,
        tradeSpeciesId = null,
        triggerId = "1",
        usedMoveId = null,
    )
