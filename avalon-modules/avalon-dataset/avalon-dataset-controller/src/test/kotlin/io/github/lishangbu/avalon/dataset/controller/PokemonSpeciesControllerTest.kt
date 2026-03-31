package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.PokemonColor
import io.github.lishangbu.avalon.dataset.entity.PokemonHabitat
import io.github.lishangbu.avalon.dataset.entity.PokemonShape
import io.github.lishangbu.avalon.dataset.entity.PokemonSpecies
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpeciesSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpeciesView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonSpeciesInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonSpeciesInput
import io.github.lishangbu.avalon.dataset.service.PokemonSpeciesService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class PokemonSpeciesControllerTest {
    @Test
    fun getPokemonSpeciesPage_delegatesToService() {
        val service = FakePokemonSpeciesService()
        val controller = PokemonSpeciesController(service)
        val pageable = PageRequest.of(0, 5)
        val page: Page<PokemonSpeciesView> = Page(listOf(pokemonSpeciesView()), 1, 1)
        service.pageResult = page
        val specification =
            PokemonSpeciesSpecification(
                id = "1",
                internalName = "bulbasaur",
                name = "妙蛙种子",
                sortingOrder = 1,
                growthRateId = "4",
                pokemonColorId = "5",
                pokemonHabitatId = "3",
                pokemonShapeId = "8",
            )

        val result = controller.getPokemonSpeciesPage(pageable = pageable, specification = specification)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("1", service.pageCondition!!.id)
        assertEquals("4", service.pageCondition!!.growthRateId)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakePokemonSpeciesService()
        val controller = PokemonSpeciesController(service)
        val command = savePokemonSpeciesInput()
        service.saveResult = pokemonSpeciesView()

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakePokemonSpeciesService()
        val controller = PokemonSpeciesController(service)
        val command = updatePokemonSpeciesInput()
        service.updateResult = pokemonSpeciesView()

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakePokemonSpeciesService()
        val controller = PokemonSpeciesController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakePokemonSpeciesService : PokemonSpeciesService {
        var pageCondition: PokemonSpeciesSpecification? = null
        var pageable: Pageable? = null
        var savedCommand: SavePokemonSpeciesInput? = null
        var updatedCommand: UpdatePokemonSpeciesInput? = null
        var removedId: Long? = null

        var pageResult: Page<PokemonSpeciesView> = Page(emptyList(), 0, 0)
        lateinit var saveResult: PokemonSpeciesView
        lateinit var updateResult: PokemonSpeciesView

        override fun getPageByCondition(
            specification: PokemonSpeciesSpecification,
            pageable: Pageable,
        ): Page<PokemonSpeciesView> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(command: SavePokemonSpeciesInput): PokemonSpeciesView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdatePokemonSpeciesInput): PokemonSpeciesView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }
    }
}

private fun pokemonSpeciesView(): PokemonSpeciesView =
    PokemonSpeciesView(
        PokemonSpecies {
            id = 1L
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
                    id = 4L
                    internalName = "medium-slow"
                    name = "较慢"
                }
            pokemonColor =
                PokemonColor {
                    id = 5L
                    internalName = "green"
                    name = "绿色"
                }
            pokemonHabitat =
                PokemonHabitat {
                    id = 3L
                    internalName = "grassland"
                    name = "grassland"
                }
            pokemonShape =
                PokemonShape {
                    id = 8L
                    internalName = "quadruped"
                    name = "Quadruped"
                }
        },
    )

private fun savePokemonSpeciesInput(): SavePokemonSpeciesInput =
    SavePokemonSpeciesInput(
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
        pokemonColorId = "5",
        pokemonHabitatId = "3",
        pokemonShapeId = "8",
    )

private fun updatePokemonSpeciesInput(): UpdatePokemonSpeciesInput =
    UpdatePokemonSpeciesInput(
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
        pokemonColorId = "5",
        pokemonHabitatId = "3",
        pokemonShapeId = "8",
    )
