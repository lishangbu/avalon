package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Gender
import io.github.lishangbu.avalon.dataset.entity.dto.GenderSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.SaveGenderInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateGenderInput
import io.github.lishangbu.avalon.dataset.repository.GenderRepository
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class GenderServiceImplTest {
    private val repository = mock(GenderRepository::class.java)
    private val service = GenderServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = GenderSpecification(id = "1", internalName = "female")
        `when`(repository.findAll(specification)).thenReturn(listOf(genderEntity(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("female", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<Gender>(), SaveMode.INSERT_ONLY)).thenReturn(genderEntity(1L))

        val result = service.save(SaveGenderInput("female", "♀"))

        assertEquals("1", result.id)
        verify(repository).save(any<Gender>(), SaveMode.INSERT_ONLY)
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<Gender>(), SaveMode.UPSERT)).thenReturn(genderEntity(1L))

        val result = service.update(UpdateGenderInput("1", "female", "♀"))

        assertEquals("1", result.id)
        verify(repository).save(any<Gender>(), SaveMode.UPSERT)
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).removeById(1L)
    }
}

private fun genderEntity(id: Long): Gender =
    Gender {
        this.id = id
        internalName = "female"
        name = "♀"
    }
