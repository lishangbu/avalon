package io.github.lishangbu.avalon.dataset.repository

import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class LocationAreaRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var locationAreaRepository: LocationAreaRepository

    @Test
    fun shouldLoadLocationAreaSeedData() {
        val locationArea = requireNotNull(locationAreaRepository.findNullable(1L))

        assertEquals(1, locationArea.gameIndex)
        assertEquals("canalave-city-area", locationArea.internalName)
        assertEquals("Canalave City", locationArea.name)
    }
}
