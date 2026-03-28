package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.EggGroup
import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEggGroupInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEggGroupInput
import io.github.lishangbu.avalon.dataset.repository.EggGroupRepository
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class EggGroupServiceImplTest {
    private val repository = mock(EggGroupRepository::class.java)
    private val service = EggGroupServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = EggGroupSpecification(id = "1", internalName = "monster")
        `when`(repository.findAll(specification)).thenReturn(listOf(eggGroupEntity(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("monster", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<EggGroup>(), SaveMode.INSERT_ONLY)).thenReturn(eggGroupEntity(1L))

        val result = service.save(SaveEggGroupInput("monster", "怪兽", "text", "characteristics"))

        assertEquals("1", result.id)
        verify(repository).save(any<EggGroup>(), SaveMode.INSERT_ONLY)
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<EggGroup>(), SaveMode.UPSERT)).thenReturn(eggGroupEntity(1L))

        val result = service.update(UpdateEggGroupInput("1", "monster", "怪兽", "text", "characteristics"))

        assertEquals("1", result.id)
        verify(repository).save(any<EggGroup>(), SaveMode.UPSERT)
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).removeById(1L)
    }
}

private fun eggGroupEntity(id: Long): EggGroup =
    EggGroup {
        this.id = id
        internalName = "monster"
        name = "怪兽"
        text = "text"
        characteristics = "characteristics"
    }
