package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryFlavorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryFlavorInput
import io.github.lishangbu.avalon.dataset.repository.BerryFlavorRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class BerryFlavorServiceImplTest {
    private val repository = mock(BerryFlavorRepository::class.java)
    private val service = BerryFlavorServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = BerryFlavorSpecification(id = "1", internalName = "spicy")
        `when`(repository.findAll(specification)).thenReturn(listOf(berryFlavorEntity(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("spicy", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<BerryFlavor>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(berryFlavorEntity(1L))

        val result = service.save(SaveBerryFlavorInput("spicy", "辣"))

        assertEquals("1", result.id)
        verify(repository).save(any<BerryFlavor>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<BerryFlavor>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(berryFlavorEntity(1L))

        val result = service.update(UpdateBerryFlavorInput("1", "spicy", "辣"))

        assertEquals("1", result.id)
        verify(repository).save(any<BerryFlavor>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun berryFlavorEntity(id: Long): BerryFlavor =
    BerryFlavor {
        this.id = id
        internalName = "spicy"
        name = "辣"
    }
