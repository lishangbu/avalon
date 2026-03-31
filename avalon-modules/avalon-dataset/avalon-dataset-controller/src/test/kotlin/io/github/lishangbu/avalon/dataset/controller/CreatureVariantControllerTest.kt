package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Creature
import io.github.lishangbu.avalon.dataset.entity.CreatureVariant
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureVariantSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureVariantView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureVariantInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureVariantInput
import io.github.lishangbu.avalon.dataset.service.CreatureVariantService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class CreatureVariantControllerTest {
    @Test
    fun getCreatureVariantPage_delegatesToService() {
        val service = FakeCreatureVariantService()
        val controller = CreatureVariantController(service)
        val pageable = PageRequest.of(0, 5)
        val page: Page<CreatureVariantView> = Page(listOf(creatureVariantView(1L)), 1, 1)
        service.pageResult = page
        val specification = CreatureVariantSpecification(id = "1", internalName = "bulbasaur", creatureId = "1")

        val result = controller.getCreatureVariantPage(pageable = pageable, specification = specification)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals("1", service.pageCondition!!.creatureId)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeCreatureVariantService()
        val controller = CreatureVariantController(service)
        val command = saveCreatureVariantInput()
        service.saveResult = creatureVariantView(1L)

        val result = controller.save(command)

        assertSame(service.saveResult, result)
        assertSame(command, service.savedCommand)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeCreatureVariantService()
        val controller = CreatureVariantController(service)
        val command = updateCreatureVariantInput()
        service.updateResult = creatureVariantView(1L)

        val result = controller.update(command)

        assertSame(service.updateResult, result)
        assertSame(command, service.updatedCommand)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeCreatureVariantService()
        val controller = CreatureVariantController(service)

        controller.deleteById(1L)

        assertEquals(1L, service.removedId)
    }

    private class FakeCreatureVariantService : CreatureVariantService {
        var pageCondition: CreatureVariantSpecification? = null
        var pageable: Pageable? = null
        var savedCommand: SaveCreatureVariantInput? = null
        var updatedCommand: UpdateCreatureVariantInput? = null
        var removedId: Long? = null

        var pageResult: Page<CreatureVariantView> = Page(emptyList(), 0, 0)
        lateinit var saveResult: CreatureVariantView
        lateinit var updateResult: CreatureVariantView

        override fun getPageByCondition(
            specification: CreatureVariantSpecification,
            pageable: Pageable,
        ): Page<CreatureVariantView> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(command: SaveCreatureVariantInput): CreatureVariantView {
            savedCommand = command
            return saveResult
        }

        override fun update(command: UpdateCreatureVariantInput): CreatureVariantView {
            updatedCommand = command
            return updateResult
        }

        override fun removeById(id: Long) {
            removedId = id
        }
    }
}

private fun creatureVariantView(id: Long): CreatureVariantView =
    CreatureVariantView(
        CreatureVariant {
            this.id = id
            backDefault = "https://example.com/back.png"
            backFemale = null
            backShiny = "https://example.com/back-shiny.png"
            backShinyFemale = null
            battleOnly = false
            defaultForm = true
            formName = null
            formOrder = 1
            frontDefault = "https://example.com/front.png"
            frontFemale = null
            frontShiny = "https://example.com/front-shiny.png"
            frontShinyFemale = null
            internalName = "bulbasaur"
            mega = false
            name = "bulbasaur"
            creature =
                Creature {
                    this.id = 1L
                    internalName = "bulbasaur"
                    name = "bulbasaur"
                }
            sortingOrder = 1
        },
    )

private fun saveCreatureVariantInput(): SaveCreatureVariantInput =
    SaveCreatureVariantInput(
        backDefault = "https://example.com/back.png",
        backFemale = null,
        backShiny = "https://example.com/back-shiny.png",
        backShinyFemale = null,
        battleOnly = false,
        defaultForm = true,
        formName = null,
        formOrder = 1,
        frontDefault = "https://example.com/front.png",
        frontFemale = null,
        frontShiny = "https://example.com/front-shiny.png",
        frontShinyFemale = null,
        internalName = "bulbasaur",
        mega = false,
        name = "bulbasaur",
        creatureId = "1",
        sortingOrder = 1,
    )

private fun updateCreatureVariantInput(): UpdateCreatureVariantInput =
    UpdateCreatureVariantInput(
        id = "1",
        backDefault = "https://example.com/back.png",
        backFemale = null,
        backShiny = "https://example.com/back-shiny.png",
        backShinyFemale = null,
        battleOnly = false,
        defaultForm = true,
        formName = null,
        formOrder = 1,
        frontDefault = "https://example.com/front.png",
        frontFemale = null,
        frontShiny = "https://example.com/front-shiny.png",
        frontShinyFemale = null,
        internalName = "bulbasaur",
        mega = false,
        name = "bulbasaur",
        creatureId = "1",
        sortingOrder = 1,
    )
