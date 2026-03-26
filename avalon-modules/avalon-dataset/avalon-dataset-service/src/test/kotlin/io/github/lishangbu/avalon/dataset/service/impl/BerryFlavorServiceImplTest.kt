package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryFlavorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryFlavorInput
import io.github.lishangbu.avalon.dataset.repository.BerryFlavorRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BerryFlavorServiceImplTest {
    @Test
    fun listByCondition_callsRepository() {
        val repository = FakeBerryFlavorRepository()
        val service = BerryFlavorServiceImpl(repository)
        val specification = BerryFlavorSpecification(id = "1", internalName = "spicy")
        repository.listResult = listOf(berryFlavorEntity(1L, "spicy", "辣"))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("辣", result.first().name)
        assertEquals(specification, repository.listCondition)
    }

    @Test
    fun save_usesRepository() {
        val repository = FakeBerryFlavorRepository()
        val service = BerryFlavorServiceImpl(repository)
        val command = SaveBerryFlavorInput("spicy", "辣")
        repository.saveResult = berryFlavorEntity(1L, "spicy", "辣")

        val result = service.save(command)

        assertEquals("1", result.id)
        assertEquals("spicy", repository.savedBerryFlavor!!.internalName)
    }

    @Test
    fun update_usesRepository() {
        val repository = FakeBerryFlavorRepository()
        val service = BerryFlavorServiceImpl(repository)
        val command = UpdateBerryFlavorInput("1", "sweet", "甜")
        repository.saveResult = berryFlavorEntity(1L, "sweet", "甜")

        val result = service.update(command)

        assertEquals("1", result.id)
        assertEquals(1L, repository.savedBerryFlavor!!.id)
        assertEquals("sweet", repository.savedBerryFlavor!!.internalName)
    }

    @Test
    fun removeById_callsRepository() {
        val repository = FakeBerryFlavorRepository()
        val service = BerryFlavorServiceImpl(repository)

        service.removeById(1L)

        assertEquals(1L, repository.deletedId)
    }

    private class FakeBerryFlavorRepository : BerryFlavorRepository {
        var listCondition: BerryFlavorSpecification? = null
        var savedBerryFlavor: BerryFlavor? = null
        var deletedId: Long? = null

        var listResult: List<BerryFlavor> = emptyList()
        var saveResult: BerryFlavor = BerryFlavor()

        override fun findAll(): List<BerryFlavor> = emptyList()

        override fun findAll(specification: BerryFlavorSpecification?): List<BerryFlavor> {
            listCondition = specification
            return listResult
        }

        override fun findById(id: Long): BerryFlavor? = null

        override fun save(berryFlavor: BerryFlavor): BerryFlavor {
            savedBerryFlavor = berryFlavor
            return saveResult
        }

        override fun saveAndFlush(berryFlavor: BerryFlavor): BerryFlavor = save(berryFlavor)

        override fun deleteById(id: Long) {
            deletedId = id
        }

        override fun flush() = Unit
    }
}

private fun berryFlavorEntity(
    id: Long,
    internalName: String,
    name: String,
): BerryFlavor =
    BerryFlavor {
        this.id = id
        this.internalName = internalName
        this.name = name
    }
