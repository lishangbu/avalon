package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.EggGroup
import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEggGroupInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEggGroupInput
import io.github.lishangbu.avalon.dataset.repository.EggGroupRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
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
        `when`(repository.listViews(specification)).thenReturn(listOf(eggGroupView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("monster", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<EggGroup>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(eggGroupEntity(1L))

        val result = service.save(SaveEggGroupInput("monster", "怪兽", "text", "characteristics"))

        assertEquals("1", result.id)
        verify(repository).save(any<EggGroup>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<EggGroup>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(eggGroupEntity(1L))

        val result = service.update(UpdateEggGroupInput("1", "monster", "怪兽", "text", "characteristics"))

        assertEquals("1", result.id)
        verify(repository).save(any<EggGroup>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
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

private fun eggGroupView(id: Long): EggGroupView = EggGroupView(eggGroupEntity(id))
