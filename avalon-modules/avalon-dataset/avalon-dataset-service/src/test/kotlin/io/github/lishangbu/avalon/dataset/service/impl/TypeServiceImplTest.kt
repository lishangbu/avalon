package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.repository.TypeRepository
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Example
import org.springframework.data.domain.PageRequest
import io.github.lishangbu.avalon.dataset.entity.Type as DatasetType

@ExtendWith(MockitoExtension::class)
class TypeServiceImplTest {
    @Mock
    private lateinit var typeRepository: TypeRepository

    @InjectMocks
    private lateinit var typeService: TypeServiceImpl

    @Test
    fun getPageByCondition_callsRepository() {
        val type = DatasetType()
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<DatasetType> = Page(listOf(type), 1, 1)
        val repository =
            Mockito.mock(TypeRepository::class.java) { invocation ->
                if (invocation.method.name == "findAll" && invocation.arguments.size == 2) {
                    page
                } else {
                    Answers.RETURNS_DEFAULTS.answer(invocation)
                }
            }
        val service = TypeServiceImpl(repository)

        val result = service.getPageByCondition(type, pageable)

        assertSame(page, result)
        val invocation =
            Mockito
                .mockingDetails(repository)
                .invocations
                .single { it.method.name == "findAll" && it.arguments.size == 2 }
        assertSame(type, (invocation.arguments[0] as Example<*>).probe)
        assertSame(pageable, invocation.arguments[1])
    }

    @Test
    fun listByCondition_callsRepository() {
        val type = DatasetType()
        val expected = listOf(type)
        Mockito
            .`when`(typeRepository.findAll(ArgumentMatchers.any<Example<DatasetType>>()))
            .thenReturn(expected)

        val result = typeService.listByCondition(type)

        assertSame(expected, result)
        Mockito.verify(typeRepository).findAll(ArgumentMatchers.any<Example<DatasetType>>())
    }

    @Test
    fun save_usesRepository() {
        val type = DatasetType()
        Mockito.`when`(typeRepository.save(type)).thenReturn(type)

        val result = typeService.save(type)

        assertSame(type, result)
        Mockito.verify(typeRepository).save(type)
    }

    @Test
    fun update_usesRepository() {
        val type = DatasetType()
        Mockito.`when`(typeRepository.save(type)).thenReturn(type)

        val result = typeService.update(type)

        assertSame(type, result)
        Mockito.verify(typeRepository).save(type)
    }

    @Test
    fun removeById_callsRepository() {
        typeService.removeById(1L)
        Mockito.verify(typeRepository).deleteById(1L)
    }
}
