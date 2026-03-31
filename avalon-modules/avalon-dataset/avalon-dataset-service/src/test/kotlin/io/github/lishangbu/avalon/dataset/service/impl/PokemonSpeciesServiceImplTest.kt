package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.PokemonColor
import io.github.lishangbu.avalon.dataset.entity.PokemonHabitat
import io.github.lishangbu.avalon.dataset.entity.PokemonShape
import io.github.lishangbu.avalon.dataset.entity.PokemonSpecies
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpeciesSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.PokemonSpeciesView
import io.github.lishangbu.avalon.dataset.entity.dto.SavePokemonSpeciesInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdatePokemonSpeciesInput
import io.github.lishangbu.avalon.dataset.repository.PokemonSpeciesRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class PokemonSpeciesServiceImplTest {
    private val repository = mock(PokemonSpeciesRepository::class.java)
    private val service = PokemonSpeciesServiceImpl(repository)

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = PokemonSpeciesSpecification(id = "1", internalName = "bulbasaur", growthRateId = "4", pokemonColorId = "5")
        val pageable = PageRequest.of(0, 5)
        `when`(repository.pageViews(specification, pageable)).thenReturn(Page(listOf(pokemonSpeciesView(1L)), 1, 1))

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
                .pokemonColor
                ?.name,
        )
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(
            repository.save(
                any<PokemonSpecies>(),
                eq(SaveMode.INSERT_ONLY),
                eq(AssociatedSaveMode.REPLACE),
                isNull(),
            ),
        ).thenReturn(pokemonSpeciesSavedEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(pokemonSpeciesView(1L))

        val result = service.save(savePokemonSpeciesInput())

        assertEquals("1", result.id)
        assertEquals("medium-slow", result.growthRate?.internalName)
        assertEquals("绿色", result.pokemonColor?.name)
        verify(repository).save(any<PokemonSpecies>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(
            repository.save(
                any<PokemonSpecies>(),
                eq(SaveMode.UPSERT),
                eq(AssociatedSaveMode.REPLACE),
                isNull(),
            ),
        ).thenReturn(pokemonSpeciesSavedEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(pokemonSpeciesView(1L))

        val result = service.update(updatePokemonSpeciesInput())

        assertEquals("1", result.id)
        assertEquals(false, result.legendary)
        assertEquals("grassland", result.pokemonHabitat?.name)
        assertEquals("Quadruped", result.pokemonShape?.name)
        verify(repository).save(any<PokemonSpecies>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

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

private fun pokemonSpeciesSavedEntity(id: Long): PokemonSpecies =
    PokemonSpecies {
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
        pokemonColor =
            PokemonColor {
                this.id = 5L
            }
        pokemonHabitat =
            PokemonHabitat {
                this.id = 3L
            }
        pokemonShape =
            PokemonShape {
                this.id = 8L
            }
    }

private fun pokemonSpeciesWithAssociations(id: Long): PokemonSpecies =
    PokemonSpecies {
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
        pokemonColor =
            PokemonColor {
                this.id = 5L
                internalName = "green"
                name = "绿色"
            }
        pokemonHabitat =
            PokemonHabitat {
                this.id = 3L
                internalName = "grassland"
                name = "grassland"
            }
        pokemonShape =
            PokemonShape {
                this.id = 8L
                internalName = "quadruped"
                name = "Quadruped"
            }
    }

private fun pokemonSpeciesView(id: Long): PokemonSpeciesView = PokemonSpeciesView(pokemonSpeciesWithAssociations(id))
