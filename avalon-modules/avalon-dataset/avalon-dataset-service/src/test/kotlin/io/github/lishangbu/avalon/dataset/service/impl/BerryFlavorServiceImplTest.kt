package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorSpecification
import io.github.lishangbu.avalon.dataset.repository.BerryFlavorRepository
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class BerryFlavorServiceImplTest {
    @Mock
    private lateinit var berryFlavorRepository: BerryFlavorRepository

    @InjectMocks
    private lateinit var berryFlavorService: BerryFlavorServiceImpl

    @Test
    fun listByCondition_callsRepository() {
        val specification = BerryFlavorSpecification(id = "1", internalName = "spicy")
        val berryFlavor = BerryFlavor()
        val expected = listOf(berryFlavor)
        Mockito.`when`(berryFlavorRepository.findAll(specification)).thenReturn(expected)

        val result = berryFlavorService.listByCondition(specification)

        assertSame(expected, result)
        Mockito.verify(berryFlavorRepository).findAll(specification)
    }

    @Test
    fun save_usesRepository() {
        val berryFlavor = BerryFlavor()
        Mockito.`when`(berryFlavorRepository.save(berryFlavor)).thenReturn(berryFlavor)

        val result = berryFlavorService.save(berryFlavor)

        assertSame(berryFlavor, result)
        Mockito.verify(berryFlavorRepository).save(berryFlavor)
    }

    @Test
    fun update_usesRepository() {
        val berryFlavor = BerryFlavor()
        Mockito.`when`(berryFlavorRepository.save(berryFlavor)).thenReturn(berryFlavor)

        val result = berryFlavorService.update(berryFlavor)

        assertSame(berryFlavor, result)
        Mockito.verify(berryFlavorRepository).save(berryFlavor)
    }

    @Test
    fun removeById_callsRepository() {
        berryFlavorService.removeById(1L)
        Mockito.verify(berryFlavorRepository).deleteById(1L)
    }
}
