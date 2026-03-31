package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Location
import io.github.lishangbu.avalon.dataset.entity.LocationArea
import io.github.lishangbu.avalon.dataset.entity.dto.LocationAreaSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.LocationAreaView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveLocationAreaInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateLocationAreaInput
import io.github.lishangbu.avalon.dataset.repository.LocationAreaRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class LocationAreaServiceImplTest {
    private val repository = mock(LocationAreaRepository::class.java)
    private val service = LocationAreaServiceImpl(repository)

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = LocationAreaSpecification(id = "1", internalName = "canalave-city-area", locationId = "1")
        val pageable = PageRequest.of(0, 5)
        `when`(repository.pageViews(specification, pageable)).thenReturn(Page(listOf(locationAreaView(1L)), 1, 1))

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals(
            "Canalave City",
            result.rows
                .first()
                .location
                ?.name,
        )
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<LocationArea>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(locationAreaEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(locationAreaView(1L))

        val result = service.save(SaveLocationAreaInput(1, "canalave-city-area", "Canalave City", "1"))

        assertEquals("1", result.id)
        verify(repository).save(any<LocationArea>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.save(any<LocationArea>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(locationAreaEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(locationAreaView(1L))

        val result = service.update(UpdateLocationAreaInput("1", 1, "canalave-city-area", "Canalave City", "1"))

        assertEquals(1, result.gameIndex)
        verify(repository).save(any<LocationArea>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun locationAreaEntity(id: Long): LocationArea =
    LocationArea {
        this.id = id
        gameIndex = 1
        internalName = "canalave-city-area"
        name = "Canalave City"
        location =
            Location {
                this.id = 1L
            }
    }

private fun locationAreaWithLocation(id: Long): LocationArea =
    LocationArea {
        this.id = id
        gameIndex = 1
        internalName = "canalave-city-area"
        name = "Canalave City"
        location =
            Location {
                this.id = 1L
                internalName = "canalave-city"
                name = "Canalave City"
            }
    }

private fun locationAreaView(id: Long): LocationAreaView = LocationAreaView(locationAreaWithLocation(id))
