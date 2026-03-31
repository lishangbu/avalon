package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.CreatureColor
import io.github.lishangbu.avalon.dataset.entity.CreatureHabitat
import io.github.lishangbu.avalon.dataset.entity.CreatureShape
import io.github.lishangbu.avalon.dataset.entity.CreatureSpecies
import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpeciesSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpeciesView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureSpeciesInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureSpeciesInput
import io.github.lishangbu.avalon.dataset.service.CreatureSpeciesService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class CreatureSpeciesControllerTest {
    @Test
    fun getCreatureSpeciesPage_delegatesToService() {
        val service = FakeCreatureSpeciesService()
        val controller = CreatureSpeciesController(service)
        val pageable = PageRequest.of(0, 5)
        val page: Page<CreatureSpeciesView> = Page(listOf(creatureSpeciesView()), 1, 1)
        service.pageResult = page
        val specification =
            CreatureSpeciesSpecification(
                id = "1",
                internalName = "bulbasaur",
                name = "妙蛙种子",
                sortingOrder = 1,
                growthRateId = "4",
                creatureColorId = "5",
                creatureHabitatId = "3",
                creatureShapeId = "8",
            )

        val result = controller.getCreatureSpeciesPage(pageable = pageable, specification = specification)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("1", service.pageCondition!!.id)
        assertEquals("4", service.pageCondition!!.growthRateId)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeCreatureSpeciesService()
        val controller = CreatureSpeciesController(service)
        val command = saveCreatureSpeciesInput()
        service.saveResult = creatureSpeciesView()

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeCreatureSpeciesService()
        val controller = CreatureSpeciesController(service)
        val command = updateCreatureSpeciesInput()
        service.updateResult = creatureSpeciesView()

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeCreatureSpeciesService()
        val controller = CreatureSpeciesController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeCreatureSpeciesService : CreatureSpeciesService {
        var pageCondition: CreatureSpeciesSpecification? = null
        var pageable: Pageable? = null
        var savedCommand: SaveCreatureSpeciesInput? = null
        var updatedCommand: UpdateCreatureSpeciesInput? = null
        var removedId: Long? = null

        var pageResult: Page<CreatureSpeciesView> = Page(emptyList(), 0, 0)
        lateinit var saveResult: CreatureSpeciesView
        lateinit var updateResult: CreatureSpeciesView

        override fun getPageByCondition(
            specification: CreatureSpeciesSpecification,
            pageable: Pageable,
        ): Page<CreatureSpeciesView> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(command: SaveCreatureSpeciesInput): CreatureSpeciesView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateCreatureSpeciesInput): CreatureSpeciesView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }
    }
}

private fun creatureSpeciesView(): CreatureSpeciesView =
    CreatureSpeciesView(
        CreatureSpecies {
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
            creatureColor =
                CreatureColor {
                    id = 5L
                    internalName = "green"
                    name = "绿色"
                }
            creatureHabitat =
                CreatureHabitat {
                    id = 3L
                    internalName = "grassland"
                    name = "grassland"
                }
            creatureShape =
                CreatureShape {
                    id = 8L
                    internalName = "quadruped"
                    name = "Quadruped"
                }
        },
    )

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
