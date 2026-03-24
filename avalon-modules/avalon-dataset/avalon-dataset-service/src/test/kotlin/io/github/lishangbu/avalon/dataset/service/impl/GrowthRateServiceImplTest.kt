package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.repository.GrowthRateRepository
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Example

@ExtendWith(MockitoExtension::class)
class GrowthRateServiceImplTest {
    @Mock
    private lateinit var growthRateRepository: GrowthRateRepository

    @InjectMocks
    private lateinit var growthRateService: GrowthRateServiceImpl

    @Test
    fun listByCondition_callsRepository() {
        val growthRate = GrowthRate {}
        val expected = listOf(growthRate)
        Mockito
            .`when`(growthRateRepository.findAll(ArgumentMatchers.any<Example<GrowthRate>>()))
            .thenReturn(expected)

        val result = growthRateService.listByCondition(growthRate)

        assertSame(expected, result)
        Mockito.verify(growthRateRepository).findAll(ArgumentMatchers.any<Example<GrowthRate>>())
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
