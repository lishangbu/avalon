package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.repository.BerryFirmnessRepository
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Example
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class BerryFirmnessServiceImplTest {
    @Mock
    private lateinit var berryFirmnessRepository: BerryFirmnessRepository

    @InjectMocks
    private lateinit var berryFirmnessService: BerryFirmnessServiceImpl

    @Test
    fun getPageByCondition_callsRepository() {
        val berryFirmness = BerryFirmness()
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<BerryFirmness> = Page(listOf(berryFirmness), 1, 1)
        val repository =
            Mockito.mock(BerryFirmnessRepository::class.java) { invocation ->
                if (invocation.method.name == "findAll" && invocation.arguments.size == 2) {
                    page
                } else {
                    Answers.RETURNS_DEFAULTS.answer(invocation)
                }
            }
        val service = BerryFirmnessServiceImpl(repository)

        val result = service.getPageByCondition(berryFirmness, pageable)

        assertSame(page, result)
        val invocation =
            Mockito
                .mockingDetails(repository)
                .invocations
                .single { it.method.name == "findAll" && it.arguments.size == 2 }
        assertSame(berryFirmness, (invocation.arguments[0] as Example<*>).probe)
        assertSame(pageable, invocation.arguments[1])
    }

    @Test
    fun listByCondition_callsRepository() {
        val berryFirmness = BerryFirmness()
        val expected = listOf(berryFirmness)
        Mockito
            .`when`(
                berryFirmnessRepository.findAll(ArgumentMatchers.any<Example<BerryFirmness>>()),
            ).thenReturn(expected)

        val result = berryFirmnessService.listByCondition(berryFirmness)

        assertSame(expected, result)
        Mockito
            .verify(berryFirmnessRepository)
            .findAll(ArgumentMatchers.any<Example<BerryFirmness>>())
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
