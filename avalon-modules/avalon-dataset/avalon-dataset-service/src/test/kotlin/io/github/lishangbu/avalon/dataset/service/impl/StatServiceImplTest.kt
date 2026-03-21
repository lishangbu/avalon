package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.repository.StatRepository
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Example
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class StatServiceImplTest {
    @Mock
    private lateinit var statRepository: StatRepository

    @InjectMocks
    private lateinit var statService: StatServiceImpl

    @Test
    fun getPageByCondition_callsRepository() {
        val stat = Stat()
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<Stat> = Page(listOf(stat), 1, 1)
        val repository =
            Mockito.mock(StatRepository::class.java) { invocation ->
                if (invocation.method.name == "findAll" && invocation.arguments.size == 2) {
                    page
                } else {
                    Answers.RETURNS_DEFAULTS.answer(invocation)
                }
            }
        val service = StatServiceImpl(repository)

        val result = service.getPageByCondition(stat, pageable)

        assertSame(page, result)
        val invocation =
            Mockito
                .mockingDetails(repository)
                .invocations
                .single { it.method.name == "findAll" && it.arguments.size == 2 }
        assertSame(stat, (invocation.arguments[0] as Example<*>).probe)
        assertSame(pageable, invocation.arguments[1])
    }

    @Test
    fun listByCondition_callsRepository() {
        val stat = Stat()
        val expected = listOf(stat)
        Mockito
            .`when`(statRepository.findAll(ArgumentMatchers.any<Example<Stat>>()))
            .thenReturn(expected)

        val result = statService.listByCondition(stat)

        assertSame(expected, result)
        Mockito.verify(statRepository).findAll(ArgumentMatchers.any<Example<Stat>>())
    }

    @Test
    fun save_usesRepository() {
        val stat = Stat()
        Mockito.`when`(statRepository.save(stat)).thenReturn(stat)

        val result = statService.save(stat)

        assertSame(stat, result)
        Mockito.verify(statRepository).save(stat)
    }

    @Test
    fun update_usesRepository() {
        val stat = Stat()
        Mockito.`when`(statRepository.save(stat)).thenReturn(stat)

        val result = statService.update(stat)

        assertSame(stat, result)
        Mockito.verify(statRepository).save(stat)
    }

    @Test
    fun removeById_callsRepository() {
        statService.removeById(1L)
        Mockito.verify(statRepository).deleteById(1L)
    }
}
