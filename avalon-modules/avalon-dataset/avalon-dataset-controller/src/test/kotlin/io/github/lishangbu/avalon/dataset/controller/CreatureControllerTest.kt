package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Creature
import io.github.lishangbu.avalon.dataset.entity.CreatureSpecies
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureInput
import io.github.lishangbu.avalon.dataset.service.CreatureService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class CreatureControllerTest {
    @Test
    fun getCreaturePage_delegatesToService() {
        val service = FakeCreatureService()
        val controller = CreatureController(service)
        val pageable = PageRequest.of(0, 5)
        val page: Page<CreatureView> = Page(listOf(creatureView()), 1, 1)
        service.pageResult = page
        val specification =
            CreatureSpecification(
                id = "1",
                internalName = "bulbasaur",
                name = "bulbasaur",
                height = 7,
                weight = 69,
                baseExperience = 64,
                sortingOrder = 1,
                creatureSpeciesId = "1",
            )

        val result = controller.getCreaturePage(pageable = pageable, specification = specification)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("1", service.pageCondition!!.id)
        assertEquals("1", service.pageCondition!!.creatureSpeciesId)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeCreatureService()
        val controller = CreatureController(service)
        val command =
            SaveCreatureInput(
                internalName = "bulbasaur",
                name = "bulbasaur",
                height = 7,
                weight = 69,
                baseExperience = 64,
                sortingOrder = 1,
                creatureSpeciesId = "1",
            )
        service.saveResult = creatureView()

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeCreatureService()
        val controller = CreatureController(service)
        val command =
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
        service.updateResult = creatureView()

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeCreatureService()
        val controller = CreatureController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeCreatureService : CreatureService {
        var pageCondition: CreatureSpecification? = null
        var pageable: Pageable? = null
        var savedCommand: SaveCreatureInput? = null
        var updatedCommand: UpdateCreatureInput? = null
        var removedId: Long? = null

        var pageResult: Page<CreatureView> = Page(emptyList(), 0, 0)
        lateinit var saveResult: CreatureView
        lateinit var updateResult: CreatureView

        override fun getPageByCondition(
            specification: CreatureSpecification,
            pageable: Pageable,
        ): Page<CreatureView> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(command: SaveCreatureInput): CreatureView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateCreatureInput): CreatureView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }
    }
}

private fun creatureView(): CreatureView =
    CreatureView(
        Creature {
            id = 1L
            internalName = "bulbasaur"
            name = "bulbasaur"
            height = 7
            weight = 69
            baseExperience = 64
            sortingOrder = 1
            creatureSpecies =
                CreatureSpecies {
                    id = 1L
                    internalName = "bulbasaur"
                    name = "妙蛙种子"
                }
        },
    )
