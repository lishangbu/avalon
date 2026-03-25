package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.entity.dto.StatSpecification
import io.github.lishangbu.avalon.dataset.repository.StatRepository
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class StatServiceImplTest {
    @Mock
    private lateinit var statRepository: StatRepository

    @InjectMocks
    private lateinit var statService: StatServiceImpl

    @Test
    fun listByCondition_callsRepository() {
        val specification = StatSpecification(id = "1", internalName = "speed")
        val stat = Stat()
        val expected = listOf(stat)
        Mockito.`when`(statRepository.findAll(specification)).thenReturn(expected)

        val result = statService.listByCondition(specification)

        assertSame(expected, result)
        Mockito.verify(statRepository).findAll(specification)
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
