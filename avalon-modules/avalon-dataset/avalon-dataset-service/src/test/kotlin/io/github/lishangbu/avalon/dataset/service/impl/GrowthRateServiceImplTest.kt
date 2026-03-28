package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.SaveGrowthRateInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateGrowthRateInput
import io.github.lishangbu.avalon.dataset.repository.GrowthRateRepository
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class GrowthRateServiceImplTest {
    private val repository = mock(GrowthRateRepository::class.java)
    private val service = GrowthRateServiceImpl(repository)

    @Test
    fun listByCondition_callsRepository() {
        val specification = GrowthRateSpecification(id = "1", internalName = "slow")
        `when`(repository.findAll(specification)).thenReturn(listOf(growthRateEntity(1L)))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("slow", result.first().internalName)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<GrowthRate>(), SaveMode.INSERT_ONLY)).thenReturn(growthRateEntity(1L))

        val result = service.save(SaveGrowthRateInput("slow", "慢", "slow"))

        assertEquals("1", result.id)
        verify(repository).save(any<GrowthRate>(), SaveMode.INSERT_ONLY)
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<GrowthRate>(), SaveMode.UPSERT)).thenReturn(growthRateEntity(1L))

        val result = service.update(UpdateGrowthRateInput("1", "slow", "慢", "slow"))

        assertEquals("1", result.id)
        verify(repository).save(any<GrowthRate>(), SaveMode.UPSERT)
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).removeById(1L)
    }
}

private fun growthRateEntity(id: Long): GrowthRate =
    GrowthRate {
        this.id = id
        internalName = "slow"
        name = "慢"
        description = "slow"
    }
