package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.TypeSpecification
import io.github.lishangbu.avalon.dataset.repository.TypeRepository
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import io.github.lishangbu.avalon.dataset.entity.Type as DatasetType

@ExtendWith(MockitoExtension::class)
class TypeServiceImplTest {
    @Mock
    private lateinit var typeRepository: TypeRepository

    @InjectMocks
    private lateinit var typeService: TypeServiceImpl

    @Test
    fun listByCondition_callsRepository() {
        val specification = TypeSpecification(id = "1", internalName = "fire", name = "火")
        val type = DatasetType()
        val expected = listOf(type)
        Mockito.`when`(typeRepository.findAll(specification)).thenReturn(expected)

        val result = typeService.listByCondition(specification)

        assertSame(expected, result)
        Mockito.verify(typeRepository).findAll(specification)
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
