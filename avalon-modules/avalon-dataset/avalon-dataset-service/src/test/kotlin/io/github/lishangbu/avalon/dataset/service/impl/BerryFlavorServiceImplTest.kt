package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.repository.BerryFlavorRepository
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Example
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class BerryFlavorServiceImplTest {
    @Mock
    private lateinit var berryFlavorRepository: BerryFlavorRepository

    @InjectMocks
    private lateinit var berryFlavorService: BerryFlavorServiceImpl

    @Test
    fun getPageByCondition_callsRepository() {
        val berryFlavor = BerryFlavor()
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<BerryFlavor> = Page(listOf(berryFlavor), 1, 1)
        val repository =
            Mockito.mock(BerryFlavorRepository::class.java) { invocation ->
                if (invocation.method.name == "findAll" && invocation.arguments.size == 2) {
                    page
                } else {
                    Answers.RETURNS_DEFAULTS.answer(invocation)
                }
            }
        val service = BerryFlavorServiceImpl(repository)

        val result = service.getPageByCondition(berryFlavor, pageable)

        assertSame(page, result)
        val invocation =
            Mockito
                .mockingDetails(repository)
                .invocations
                .single { it.method.name == "findAll" && it.arguments.size == 2 }
        assertSame(berryFlavor, (invocation.arguments[0] as Example<*>).probe)
        assertSame(pageable, invocation.arguments[1])
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
