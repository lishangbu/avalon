package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Gender
import io.github.lishangbu.avalon.dataset.repository.GenderRepository
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Example

@ExtendWith(MockitoExtension::class)
class GenderServiceImplTest {
    @Mock
    private lateinit var genderRepository: GenderRepository

    @InjectMocks
    private lateinit var genderService: GenderServiceImpl

    @Test
    fun listByCondition_callsRepository() {
        val gender = Gender {}
        val expected = listOf(gender)
        Mockito
            .`when`(genderRepository.findAll(ArgumentMatchers.any<Example<Gender>>()))
            .thenReturn(expected)

        val result = genderService.listByCondition(gender)

        assertSame(expected, result)
        Mockito.verify(genderRepository).findAll(ArgumentMatchers.any<Example<Gender>>())
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
