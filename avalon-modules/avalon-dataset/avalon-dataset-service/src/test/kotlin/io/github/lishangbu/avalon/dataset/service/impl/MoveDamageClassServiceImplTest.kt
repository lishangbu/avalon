package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassSpecification
import io.github.lishangbu.avalon.dataset.repository.MoveDamageClassRepository
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class MoveDamageClassServiceImplTest {
    @Mock
    private lateinit var moveDamageClassRepository: MoveDamageClassRepository

    @InjectMocks
    private lateinit var moveDamageClassService: MoveDamageClassServiceImpl

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = MoveDamageClassSpecification(id = "1", internalName = "physical")
        val moveDamageClass = MoveDamageClass()
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<MoveDamageClass> = Page(listOf(moveDamageClass), 1, 1)
        Mockito.`when`(moveDamageClassRepository.findAll(specification, pageable)).thenReturn(page)

        val result = moveDamageClassService.getPageByCondition(specification, pageable)

        assertSame(page, result)
        Mockito.verify(moveDamageClassRepository).findAll(specification, pageable)
    }

    @Test
    fun listByCondition_callsRepository() {
        val specification = MoveDamageClassSpecification(id = "1", internalName = "physical")
        val moveDamageClass = MoveDamageClass()
        val expected = listOf(moveDamageClass)
        Mockito.`when`(moveDamageClassRepository.findAll(specification)).thenReturn(expected)

        val result = moveDamageClassService.listByCondition(specification)

        assertSame(expected, result)
        Mockito.verify(moveDamageClassRepository).findAll(specification)
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
