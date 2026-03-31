package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Creature
import io.github.lishangbu.avalon.dataset.entity.CreatureVariant
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureVariantSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureVariantView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureVariantInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureVariantInput
import io.github.lishangbu.avalon.dataset.repository.CreatureVariantRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class CreatureVariantServiceImplTest {
    private val repository = mock(CreatureVariantRepository::class.java)
    private val service = CreatureVariantServiceImpl(repository)

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = CreatureVariantSpecification(id = "1", internalName = "bulbasaur", creatureId = "1")
        val pageable = PageRequest.of(0, 5)
        `when`(repository.pageViews(specification, pageable)).thenReturn(Page(listOf(creatureVariantView(1L)), 1, 1))

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals(true, result.rows.first().defaultForm)
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<CreatureVariant>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(creatureVariantEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(creatureVariantView(1L))

        val result = service.save(saveCreatureVariantInput())

        assertEquals("1", result.id)
        assertEquals("bulbasaur", result.creature?.internalName)
        verify(repository).save(any<CreatureVariant>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.save(any<CreatureVariant>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(creatureVariantEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(creatureVariantView(1L))

        val result = service.update(updateCreatureVariantInput())

        assertEquals(false, result.battleOnly)
        assertEquals(false, result.mega)
        verify(repository).save(any<CreatureVariant>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

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

private fun creatureVariantEntity(id: Long): CreatureVariant =
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
            }
        sortingOrder = 1
    }

private fun creatureVariantWithCreature(id: Long): CreatureVariant =
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
    }

private fun creatureVariantView(id: Long): CreatureVariantView = CreatureVariantView(creatureVariantWithCreature(id))
