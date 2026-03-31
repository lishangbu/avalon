package io.github.lishangbu.avalon.dataset.repository

import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class RegionRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var regionRepository: RegionRepository

    @Test
    fun shouldLoadRegionSeedData() {
        val region = requireNotNull(regionRepository.findNullable(1L))

        assertEquals("kanto", region.internalName)
        assertEquals("Kanto", region.name)
    }
}
