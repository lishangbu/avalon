package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import io.github.lishangbu.avalon.dataset.repository.MoveDamageClassRepository
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Example
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class MoveDamageClassServiceImplTest {
    @Mock
    private lateinit var moveDamageClassRepository: MoveDamageClassRepository

    @InjectMocks
    private lateinit var moveDamageClassService: MoveDamageClassServiceImpl

    @Test
    fun getPageByCondition_callsRepository() {
        val moveDamageClass = MoveDamageClass()
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<MoveDamageClass> = Page(listOf(moveDamageClass), 1, 1)
        val repository =
            Mockito.mock(MoveDamageClassRepository::class.java) { invocation ->
                if (invocation.method.name == "findAll" && invocation.arguments.size == 2) {
                    page
                } else {
                    Answers.RETURNS_DEFAULTS.answer(invocation)
                }
            }
        val service = MoveDamageClassServiceImpl(repository)

        val result = service.getPageByCondition(moveDamageClass, pageable)

        assertSame(page, result)
        val invocation =
            Mockito
                .mockingDetails(repository)
                .invocations
                .single { it.method.name == "findAll" && it.arguments.size == 2 }
        assertSame(moveDamageClass, (invocation.arguments[0] as Example<*>).probe)
        assertSame(pageable, invocation.arguments[1])
    }

    @Test
    fun listByCondition_callsRepository() {
        val moveDamageClass = MoveDamageClass()
        val expected = listOf(moveDamageClass)
        Mockito
            .`when`(
                moveDamageClassRepository.findAll(ArgumentMatchers.any<Example<MoveDamageClass>>()),
            ).thenReturn(expected)

        val result = moveDamageClassService.listByCondition(moveDamageClass)

        assertSame(expected, result)
        Mockito
            .verify(moveDamageClassRepository)
            .findAll(ArgumentMatchers.any<Example<MoveDamageClass>>())
    }

    @Test
    fun save_usesRepository() {
        val moveDamageClass = MoveDamageClass()
        Mockito.`when`(moveDamageClassRepository.save(moveDamageClass)).thenReturn(moveDamageClass)

        val result = moveDamageClassService.save(moveDamageClass)

        assertSame(moveDamageClass, result)
        Mockito.verify(moveDamageClassRepository).save(moveDamageClass)
    }

    @Test
    fun update_usesRepository() {
        val moveDamageClass = MoveDamageClass()
        Mockito.`when`(moveDamageClassRepository.save(moveDamageClass)).thenReturn(moveDamageClass)

        val result = moveDamageClassService.update(moveDamageClass)

        assertSame(moveDamageClass, result)
        Mockito.verify(moveDamageClassRepository).save(moveDamageClass)
    }

    @Test
    fun removeById_callsRepository() {
        moveDamageClassService.removeById(1L)
        Mockito.verify(moveDamageClassRepository).deleteById(1L)
    }
}
