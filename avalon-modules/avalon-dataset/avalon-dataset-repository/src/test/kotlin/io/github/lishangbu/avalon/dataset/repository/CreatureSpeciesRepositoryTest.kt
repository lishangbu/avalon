package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.CreatureColor
import io.github.lishangbu.avalon.dataset.entity.CreatureHabitat
import io.github.lishangbu.avalon.dataset.entity.CreatureShape
import io.github.lishangbu.avalon.dataset.entity.CreatureSpecies
import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpeciesSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@Transactional
class CreatureSpeciesRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var creatureSpeciesRepository: CreatureSpeciesRepository

    @Test
    fun shouldQueryPageAndCrudCreatureSpecies() {
        val condition = CreatureSpeciesSpecification(internalName = "bulbasaur", growthRateId = "4", creatureColorId = "5")

        val results = creatureSpeciesRepository.listViews(condition)
        val page = creatureSpeciesRepository.pageViews(condition, PageRequest.of(0, 10))

        assertFalse(results.isEmpty())
        assertEquals("1", results.first().id)
        assertEquals("bulbasaur", results.first().internalName)
        assertEquals("medium-slow", results.first().growthRate?.internalName)
        assertEquals("绿色", results.first().creatureColor?.name)
        assertTrue(page.totalRowCount >= 1)
        assertFalse(page.rows.isEmpty())

        val created =
            creatureSpeciesRepository.save(
                CreatureSpecies {
                    internalName = "test-species"
                    name = "测试种族"
                    sortingOrder = 9999
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
                        }
                    creatureColor =
                        CreatureColor {
                            id = 5L
                        }
                    creatureHabitat =
                        CreatureHabitat {
                            id = 3L
                        }
                    creatureShape =
                        CreatureShape {
                            id = 8L
                        }
                },
                SaveMode.INSERT_ONLY,
            )

        val createdView = requireNotNull(creatureSpeciesRepository.loadViewById(created.id))
        assertEquals("test-species", createdView.internalName)
        assertEquals("4", createdView.growthRate?.id)

        val existing = requireNotNull(creatureSpeciesRepository.findNullable(created.id))
        creatureSpeciesRepository.save(CreatureSpecies(existing) { name = "更新后的测试种族" }, SaveMode.UPSERT)

        val updated = requireNotNull(creatureSpeciesRepository.loadViewById(created.id))
        assertEquals("更新后的测试种族", updated.name)

        creatureSpeciesRepository.deleteById(created.id)
        assertNull(creatureSpeciesRepository.loadViewById(created.id))
    }
}
