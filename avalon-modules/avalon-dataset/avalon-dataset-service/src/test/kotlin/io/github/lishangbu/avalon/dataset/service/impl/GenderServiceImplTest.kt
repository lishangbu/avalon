package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Gender
import io.github.lishangbu.avalon.dataset.entity.dto.GenderSpecification
import io.github.lishangbu.avalon.dataset.repository.GenderRepository
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class GenderServiceImplTest {
    @Mock
    private lateinit var genderRepository: GenderRepository

    @InjectMocks
    private lateinit var genderService: GenderServiceImpl

    @Test
    fun listByCondition_callsRepository() {
        val specification = GenderSpecification(id = "1", internalName = "male")
        val gender = Gender {}
        val expected = listOf(gender)
        Mockito.`when`(genderRepository.findAll(specification)).thenReturn(expected)

        val result = genderService.listByCondition(specification)

        assertSame(expected, result)
        Mockito.verify(genderRepository).findAll(specification)
    }

    @Test
    fun save_usesRepository() {
        val gender = Gender {}
        Mockito.`when`(genderRepository.save(gender)).thenReturn(gender)

        val result = genderService.save(gender)

        assertSame(gender, result)
        Mockito.verify(genderRepository).save(gender)
    }

    @Test
    fun update_usesRepository() {
        val gender = Gender {}
        Mockito.`when`(genderRepository.save(gender)).thenReturn(gender)

        val result = genderService.update(gender)

        assertSame(gender, result)
        Mockito.verify(genderRepository).save(gender)
    }

    @Test
    fun removeById_callsRepository() {
        genderService.removeById(1L)
        Mockito.verify(genderRepository).deleteById(1L)
    }
}
