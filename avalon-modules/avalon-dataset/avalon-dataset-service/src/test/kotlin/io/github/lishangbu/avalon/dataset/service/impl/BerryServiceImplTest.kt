package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Berry
import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.entity.Type
import io.github.lishangbu.avalon.dataset.entity.dto.BerrySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryInput
import io.github.lishangbu.avalon.dataset.repository.BerryRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class BerryServiceImplTest {
    private val repository = mock(BerryRepository::class.java)
    private val service = BerryServiceImpl(repository)

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = BerrySpecification(id = "1", internalName = "cheri")
        val pageable = PageRequest.of(0, 5)
        `when`(repository.pageViews(specification, pageable)).thenReturn(Page(listOf(berryView(1L)), 1, 1))

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals("1", result.rows.first().id)
        assertEquals(
            "hard",
            result.rows
                .first()
                .berryFirmness
                ?.internalName,
        )
    }

    @Test
    fun save_usesInsertOnlyModeAndReloadsView() {
        `when`(repository.save(any<Berry>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(berrySavedEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(berryView(1L))

        val result = service.save(SaveBerryInput("cheri", "樱子", 2, 3, 4, 5, 6, "7", "8", 9))

        assertEquals("1", result.id)
        assertEquals("hard", result.berryFirmness?.internalName)
        verify(repository).save(any<Berry>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun update_usesUpsertModeAndReloadsView() {
        `when`(repository.save(any<Berry>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(berrySavedEntity(1L))
        `when`(repository.loadViewById(1L)).thenReturn(berryView(1L))

        val result = service.update(UpdateBerryInput("1", "cheri", "樱子", 2, 3, 4, 5, 6, "7", "8", 9))

        assertEquals("1", result.id)
        assertEquals("硬", result.berryFirmness?.name)
        verify(repository).save(any<Berry>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
        verify(repository).loadViewById(1L)
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun berrySavedEntity(id: Long): Berry =
    Berry {
        this.id = id
        internalName = "cheri"
        name = "樱子"
        growthTime = 2
        maxHarvest = 3
        bulk = 4
        smoothness = 5
        soilDryness = 6
        berryFirmness =
            BerryFirmness {
                this.id = 7L
            }
        naturalGiftType =
            Type {
                this.id = 8L
            }
        naturalGiftPower = 9
    }

private fun berryWithAssociations(id: Long): Berry =
    Berry {
        this.id = id
        internalName = "cheri"
        name = "樱子"
        growthTime = 2
        maxHarvest = 3
        bulk = 4
        smoothness = 5
        soilDryness = 6
        berryFirmness =
            BerryFirmness {
                this.id = 7L
                internalName = "hard"
                name = "硬"
            }
        naturalGiftType =
            Type {
                this.id = 8L
                internalName = "fire"
                name = "火"
            }
        naturalGiftPower = 9
    }

private fun berryView(id: Long): BerryView = BerryView(berryWithAssociations(id))
