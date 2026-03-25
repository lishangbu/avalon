package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessSpecification
import io.github.lishangbu.avalon.dataset.repository.BerryFirmnessRepository
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class BerryFirmnessServiceImplTest {
    @Mock
    private lateinit var berryFirmnessRepository: BerryFirmnessRepository

    @InjectMocks
    private lateinit var berryFirmnessService: BerryFirmnessServiceImpl

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = BerryFirmnessSpecification(id = "1", internalName = "very-soft")
        val berryFirmness = BerryFirmness()
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<BerryFirmness> = Page(listOf(berryFirmness), 1, 1)
        Mockito.`when`(berryFirmnessRepository.findAll(specification, pageable)).thenReturn(page)

        val result = berryFirmnessService.getPageByCondition(specification, pageable)

        assertSame(page, result)
        Mockito.verify(berryFirmnessRepository).findAll(specification, pageable)
    }

    @Test
    fun listByCondition_callsRepository() {
        val specification = BerryFirmnessSpecification(id = "1", internalName = "very-soft")
        val berryFirmness = BerryFirmness()
        val expected = listOf(berryFirmness)
        Mockito.`when`(berryFirmnessRepository.findAll(specification)).thenReturn(expected)

        val result = berryFirmnessService.listByCondition(specification)

        assertSame(expected, result)
        Mockito.verify(berryFirmnessRepository).findAll(specification)
    }

    @Test
    fun save_usesRepository() {
        val berryFirmness = BerryFirmness()
        Mockito.`when`(berryFirmnessRepository.save(berryFirmness)).thenReturn(berryFirmness)

        val result = berryFirmnessService.save(berryFirmness)

        assertSame(berryFirmness, result)
        Mockito.verify(berryFirmnessRepository).save(berryFirmness)
    }

    @Test
    fun update_usesRepository() {
        val berryFirmness = BerryFirmness()
        Mockito.`when`(berryFirmnessRepository.save(berryFirmness)).thenReturn(berryFirmness)

        val result = berryFirmnessService.update(berryFirmness)

        assertSame(berryFirmness, result)
        Mockito.verify(berryFirmnessRepository).save(berryFirmness)
    }

    @Test
    fun removeById_callsRepository() {
        berryFirmnessService.removeById(1L)
        Mockito.verify(berryFirmnessRepository).deleteById(1L)
    }
}
