package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryFirmnessInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryFirmnessInput
import io.github.lishangbu.avalon.dataset.repository.BerryFirmnessRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class BerryFirmnessServiceImplTest {
    private val repository = mock(BerryFirmnessRepository::class.java)
    private val service = BerryFirmnessServiceImpl(repository)

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = BerryFirmnessSpecification(id = "1", internalName = "hard")
        val pageable = PageRequest.of(0, 5)
        `when`(repository.pageViews(specification, pageable)).thenReturn(Page(listOf(berryFirmnessView(1L)), 1, 1))

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals("1", result.rows.first().id)
    }

    @Test
    fun listByCondition_callsRepository() {
        val specification = BerryFirmnessSpecification(id = "1", internalName = "hard")
        `when`(repository.listViews(specification)).thenReturn(listOf(berryFirmnessView(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<BerryFirmness>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(berryFirmnessEntity(1L))

        val result = service.save(SaveBerryFirmnessInput("hard", "硬"))

        assertEquals("1", result.id)
        verify(repository).save(any<BerryFirmness>(), eq(SaveMode.INSERT_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<BerryFirmness>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())).thenReturn(berryFirmnessEntity(1L))

        val result = service.update(UpdateBerryFirmnessInput("1", "hard", "硬"))

        assertEquals("1", result.id)
        verify(repository).save(any<BerryFirmness>(), eq(SaveMode.UPDATE_ONLY), eq(AssociatedSaveMode.REPLACE), isNull())
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).deleteById(1L)
    }
}

private fun berryFirmnessEntity(id: Long): BerryFirmness =
    BerryFirmness {
        this.id = id
        internalName = "hard"
        name = "硬"
    }

private fun berryFirmnessView(id: Long): BerryFirmnessView = BerryFirmnessView(berryFirmnessEntity(id))
