package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Berry
import io.github.lishangbu.avalon.dataset.repository.BerryRepository
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Example
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class BerryServiceImplTest {
    @Mock
    private lateinit var berryRepository: BerryRepository

    @InjectMocks
    private lateinit var berryService: BerryServiceImpl

    @Test
    fun getPageByCondition_callsRepository() {
        val berry = Berry()
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<Berry> = Page(listOf(berry), 1, 1)
        val repository =
            Mockito.mock(BerryRepository::class.java) { invocation ->
                if (invocation.method.name == "findAll" && invocation.arguments.size == 2) {
                    page
                } else {
                    Answers.RETURNS_DEFAULTS.answer(invocation)
                }
            }
        val service = BerryServiceImpl(repository)

        val result = service.getPageByCondition(berry, pageable)

        assertSame(page, result)
        val invocation =
            Mockito
                .mockingDetails(repository)
                .invocations
                .single { it.method.name == "findAll" && it.arguments.size == 2 }
        assertSame(berry, (invocation.arguments[0] as Example<*>).probe)
        assertSame(pageable, invocation.arguments[1])
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
