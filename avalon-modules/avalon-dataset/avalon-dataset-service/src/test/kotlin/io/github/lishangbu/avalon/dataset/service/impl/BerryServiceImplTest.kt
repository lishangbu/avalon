package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Berry
import io.github.lishangbu.avalon.dataset.entity.dto.BerrySpecification
import io.github.lishangbu.avalon.dataset.repository.BerryRepository
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class BerryServiceImplTest {
    @Mock
    private lateinit var berryRepository: BerryRepository

    @InjectMocks
    private lateinit var berryService: BerryServiceImpl

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = BerrySpecification(id = "1", internalName = "cheri")
        val berry = Berry()
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<Berry> = Page(listOf(berry), 1, 1)
        Mockito.`when`(berryRepository.findAll(specification, pageable)).thenReturn(page)

        val result = berryService.getPageByCondition(specification, pageable)

        assertSame(page, result)
        Mockito.verify(berryRepository).findAll(specification, pageable)
    }

    @Test
    fun save_usesRepository() {
        val berry = Berry()
        Mockito.`when`(berryRepository.save(berry)).thenReturn(berry)

        val result = berryService.save(berry)

        assertSame(berry, result)
        Mockito.verify(berryRepository).save(berry)
    }

    @Test
    fun update_usesRepository() {
        val berry = Berry()
        Mockito.`when`(berryRepository.save(berry)).thenReturn(berry)

        val result = berryService.update(berry)

        assertSame(berry, result)
        Mockito.verify(berryRepository).save(berry)
    }

    @Test
    fun removeById_callsRepository() {
        berryService.removeById(1L)
        Mockito.verify(berryRepository).deleteById(1L)
    }
}
