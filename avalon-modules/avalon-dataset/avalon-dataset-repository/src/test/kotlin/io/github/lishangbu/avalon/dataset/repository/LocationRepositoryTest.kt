package io.github.lishangbu.avalon.dataset.repository

import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class LocationRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var locationRepository: LocationRepository

    @Test
    fun shouldLoadLocationSeedData() {
        val location = requireNotNull(locationRepository.findNullable(1L))

        assertEquals("canalave-city", location.internalName)
        assertEquals("Canalave City", location.name)
    }
}
