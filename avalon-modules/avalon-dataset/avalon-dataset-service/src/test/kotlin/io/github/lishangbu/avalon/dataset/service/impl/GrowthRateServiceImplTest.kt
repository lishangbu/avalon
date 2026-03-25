package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateSpecification
import io.github.lishangbu.avalon.dataset.repository.GrowthRateRepository
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class GrowthRateServiceImplTest {
    @Mock
    private lateinit var growthRateRepository: GrowthRateRepository

    @InjectMocks
    private lateinit var growthRateService: GrowthRateServiceImpl

    @Test
    fun listByCondition_callsRepository() {
        val specification = GrowthRateSpecification(id = "1", internalName = "medium")
        val growthRate = GrowthRate {}
        val expected = listOf(growthRate)
        Mockito.`when`(growthRateRepository.findAll(specification)).thenReturn(expected)

        val result = growthRateService.listByCondition(specification)

        assertSame(expected, result)
        Mockito.verify(growthRateRepository).findAll(specification)
    }

    @Test
    fun save_usesRepository() {
        val growthRate = GrowthRate {}
        Mockito.`when`(growthRateRepository.save(growthRate)).thenReturn(growthRate)

        val result = growthRateService.save(growthRate)

        assertSame(growthRate, result)
        Mockito.verify(growthRateRepository).save(growthRate)
    }

    @Test
    fun update_usesRepository() {
        val growthRate = GrowthRate {}
        Mockito.`when`(growthRateRepository.save(growthRate)).thenReturn(growthRate)

        val result = growthRateService.update(growthRate)

        assertSame(growthRate, result)
        Mockito.verify(growthRateRepository).save(growthRate)
    }

    @Test
    fun removeById_callsRepository() {
        growthRateService.removeById(1L)
        Mockito.verify(growthRateRepository).deleteById(1L)
    }
}
