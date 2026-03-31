package io.github.lishangbu.avalon.dataset.repository

import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class LocationAreaEncounterRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var locationAreaEncounterRepository: LocationAreaEncounterRepository

    @Test
    fun shouldLoadEncounterSeedData() {
        val encounter = requireNotNull(locationAreaEncounterRepository.findNullable(1L))

        assertEquals(60, encounter.maxChance)
        assertEquals(60, encounter.chance)
        assertEquals(20, encounter.minLevel)
        assertEquals(30, encounter.maxLevel)
    }
}
