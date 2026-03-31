package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Location
import io.github.lishangbu.avalon.dataset.entity.Region
import io.github.lishangbu.avalon.dataset.entity.dto.LocationSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.LocationView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveLocationInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateLocationInput
import io.github.lishangbu.avalon.dataset.repository.LocationRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class LocationServiceImplTest {
    private val repository = mock(LocationRepository::class.java)
    private val service = LocationServiceImpl(repository)

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = LocationSpecification(id = "1", internalName = "canalave-city", regionId = "4")
        val pageable = PageRequest.of(0, 5)
        `when`(repository.pageViews(specification, pageable)).thenReturn(Page(listOf(locationView(1L)), 1, 1))

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals("Canalave City", result.rows.first().name)
    }

    @Test
    fun listByCondition_callsRepository() {
        val specification = LocationSpecification(regionId = "4")
        `when`(repository.listViews(specification)).thenReturn(listOf(locationView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("sinnoh", result.first().region?.internalName)
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<Location>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(locationEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(locationView(1L))

        val result = service.save(SaveLocationInput("canalave-city", "Canalave City", "4"))

        assertEquals("1", result.id)
        verify(repository).save(any<Location>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.save(any<Location>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(locationEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(locationView(1L))

        val result = service.update(UpdateLocationInput("1", "canalave-city", "Canalave City", "4"))

        assertEquals("sinnoh", result.region?.internalName)
        verify(repository).save(any<Location>(), eq(SaveMode.UPSERT), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun locationEntity(id: Long): Location =
    Location {
        this.id = id
        internalName = "canalave-city"
        name = "Canalave City"
        region =
            Region {
                this.id = 4L
            }
    }

private fun locationWithRegion(id: Long): Location =
    Location {
        this.id = id
        internalName = "canalave-city"
        name = "Canalave City"
        region =
            Region {
                this.id = 4L
                internalName = "sinnoh"
                name = "Sinnoh"
            }
    }

private fun locationView(id: Long): LocationView = LocationView(locationWithRegion(id))
