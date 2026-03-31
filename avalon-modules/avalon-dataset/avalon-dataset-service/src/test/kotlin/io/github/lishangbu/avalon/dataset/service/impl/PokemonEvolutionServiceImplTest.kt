package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.EvolutionChain
import io.github.lishangbu.avalon.dataset.entity.EvolutionTrigger
import io.github.lishangbu.avalon.dataset.entity.PokemonEvolution
import io.github.lishangbu.avalon.dataset.entity.PokemonSpecies
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonEvolutionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonEvolutionView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonEvolutionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonEvolutionInput
import io.github.lishangbu.avalon.dataset.repository.PokemonEvolutionRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class PokemonEvolutionServiceImplTest {
    private val repository = mock(PokemonEvolutionRepository::class.java)
    private val service = PokemonEvolutionServiceImpl(repository)

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = PokemonEvolutionSpecification(evolutionChainId = "1", fromPokemonSpeciesId = "1", toPokemonSpeciesId = "2")
        val pageable = PageRequest.of(0, 5)
        `when`(repository.pageViews(specification, pageable)).thenReturn(Page(listOf(pokemonEvolutionView(1L)), 1, 1))

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals(16, result.rows.first().minLevel)
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<PokemonEvolution>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(pokemonEvolutionEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(pokemonEvolutionView(1L))

        val result = service.save(savePokemonEvolutionInput())

        assertEquals("1", result.id)
        assertEquals("1", result.evolutionChain?.id)
        verify(repository).save(any<PokemonEvolution>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.save(any<PokemonEvolution>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(pokemonEvolutionEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(pokemonEvolutionView(1L))

        val result = service.update(updatePokemonEvolutionInput())

        assertEquals("level-up", result.trigger?.internalName)
        verify(repository).save(any<PokemonEvolution>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

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

private fun pokemonEvolutionEntity(id: Long): PokemonEvolution =
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
            }
        toPokemonSpecies =
            PokemonSpecies {
                this.id = 2L
            }
        trigger =
            EvolutionTrigger {
                this.id = 1L
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
