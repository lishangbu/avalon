package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.SaveGrowthRateInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateGrowthRateInput
import io.github.lishangbu.avalon.dataset.repository.GrowthRateRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GrowthRateServiceImplTest {
    @Test
    fun listByCondition_callsRepository() {
        val repository = FakeGrowthRateRepository()
        val service = GrowthRateServiceImpl(repository)
        val specification = GrowthRateSpecification(id = "1", internalName = "slow")
        repository.listResult = listOf(growthRateEntity(1L, "slow", "慢", "slow"))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("slow", result.first().description)
        assertEquals(specification, repository.listCondition)
    }

    @Test
    fun save_usesRepository() {
        val repository = FakeGrowthRateRepository()
        val service = GrowthRateServiceImpl(repository)
        val command = SaveGrowthRateInput("slow", "慢", "slow")
        repository.saveResult = growthRateEntity(1L, "slow", "慢", "slow")

        val result = service.save(command)

        assertEquals("1", result.id)
        assertEquals("slow", repository.savedGrowthRate!!.description)
    }

    @Test
    fun update_usesRepository() {
        val repository = FakeGrowthRateRepository()
        val service = GrowthRateServiceImpl(repository)
        val command = UpdateGrowthRateInput("1", "medium", "中", "medium")
        repository.saveResult = growthRateEntity(1L, "medium", "中", "medium")

        val result = service.update(command)

        assertEquals("1", result.id)
        assertEquals(1L, repository.savedGrowthRate!!.id)
        assertEquals("medium", repository.savedGrowthRate!!.internalName)
    }

    @Test
    fun removeById_callsRepository() {
        val repository = FakeGrowthRateRepository()
        val service = GrowthRateServiceImpl(repository)

        service.removeById(1L)

        assertEquals(1L, repository.deletedId)
    }

    private class FakeGrowthRateRepository : GrowthRateRepository {
        var listCondition: GrowthRateSpecification? = null
        var savedGrowthRate: GrowthRate? = null
        var deletedId: Long? = null

        var listResult: List<GrowthRate> = emptyList()
        var saveResult: GrowthRate = GrowthRate {}

        override fun findAll(): List<GrowthRate> = emptyList()

        override fun findAll(specification: GrowthRateSpecification?): List<GrowthRate> {
            listCondition = specification
            return listResult
        }

        override fun findById(id: Long): GrowthRate? = null

        override fun save(growthRate: GrowthRate): GrowthRate {
            savedGrowthRate = growthRate
            return saveResult
        }

        override fun saveAndFlush(growthRate: GrowthRate): GrowthRate = save(growthRate)

        override fun deleteById(id: Long) {
            deletedId = id
        }

        override fun flush() = Unit
    }
}

private fun growthRateEntity(
    id: Long,
    internalName: String,
    name: String,
    description: String,
): GrowthRate =
    GrowthRate {
        this.id = id
        this.internalName = internalName
        this.name = name
        this.description = description
    }
