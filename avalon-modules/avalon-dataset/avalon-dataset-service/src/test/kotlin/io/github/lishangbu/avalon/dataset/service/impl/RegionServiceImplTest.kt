package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Region
import io.github.lishangbu.avalon.dataset.entity.dto.RegionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.RegionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveRegionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateRegionInput
import io.github.lishangbu.avalon.dataset.repository.RegionRepository
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class RegionServiceImplTest {
    private val repository = mock(RegionRepository::class.java)
    private val service = RegionServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = RegionSpecification(id = "1", internalName = "kanto")
        `when`(repository.listViews(specification)).thenReturn(listOf(regionView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("Kanto", result.first().name)
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<Region>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(regionEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(regionView(1L))

        val result = service.save(SaveRegionInput("kanto", "Kanto"))

        assertEquals("1", result.id)
        verify(repository).save(any<Region>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.save(any<Region>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(regionEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(regionView(1L))

        val result = service.update(UpdateRegionInput("1", "kanto", "Kanto"))

        assertEquals("Kanto", result.name)
        verify(repository).save(any<Region>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun regionEntity(id: Long): Region =
    Region {
        this.id = id
        internalName = "kanto"
        name = "Kanto"
    }

private fun regionView(id: Long): RegionView = RegionView(regionEntity(id))
