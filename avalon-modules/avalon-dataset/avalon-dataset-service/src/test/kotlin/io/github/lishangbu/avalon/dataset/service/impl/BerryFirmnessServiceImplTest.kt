package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryFirmnessInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryFirmnessInput
import io.github.lishangbu.avalon.dataset.repository.BerryFirmnessRepository
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class BerryFirmnessServiceImplTest {
    @Test
    fun getPageByCondition_callsRepository() {
        val repository = FakeBerryFirmnessRepository()
        val service = BerryFirmnessServiceImpl(repository)
        val specification = BerryFirmnessSpecification(id = "1", internalName = "hard")
        val pageable = PageRequest.of(0, 5)
        repository.pageResult = Page(listOf(berryFirmnessEntity(1L, "hard", "硬")), 1, 1)

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals("1", result.rows.first().id)
        assertEquals(specification, repository.pageCondition)
        assertEquals(pageable, repository.pageable)
    }

    @Test
    fun listByCondition_callsRepository() {
        val repository = FakeBerryFirmnessRepository()
        val service = BerryFirmnessServiceImpl(repository)
        val specification = BerryFirmnessSpecification(id = "1", internalName = "hard")
        repository.listResult = listOf(berryFirmnessEntity(1L, "hard", "硬"))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals(specification, repository.listCondition)
    }

    @Test
    fun save_usesRepository() {
        val repository = FakeBerryFirmnessRepository()
        val service = BerryFirmnessServiceImpl(repository)
        val command = SaveBerryFirmnessInput("hard", "硬")
        repository.saveResult = berryFirmnessEntity(1L, "hard", "硬")

        val result = service.save(command)

        assertEquals("1", result.id)
        assertEquals("hard", repository.savedBerryFirmness!!.internalName)
    }

    @Test
    fun update_usesRepository() {
        val repository = FakeBerryFirmnessRepository()
        val service = BerryFirmnessServiceImpl(repository)
        val command = UpdateBerryFirmnessInput("1", "very-hard", "很硬")
        repository.saveResult = berryFirmnessEntity(1L, "very-hard", "很硬")

        val result = service.update(command)

        assertEquals("1", result.id)
        assertEquals(1L, repository.savedBerryFirmness!!.id)
        assertEquals("very-hard", repository.savedBerryFirmness!!.internalName)
    }

    @Test
    fun removeById_callsRepository() {
        val repository = FakeBerryFirmnessRepository()
        val service = BerryFirmnessServiceImpl(repository)

        service.removeById(1L)

        assertEquals(1L, repository.deletedId)
    }

    private class FakeBerryFirmnessRepository : BerryFirmnessRepository {
        var pageCondition: BerryFirmnessSpecification? = null
        var pageable: Pageable? = null
        var listCondition: BerryFirmnessSpecification? = null
        var savedBerryFirmness: BerryFirmness? = null
        var deletedId: Long? = null

        var pageResult: Page<BerryFirmness> = Page(emptyList(), 0, 0)
        var listResult: List<BerryFirmness> = emptyList()
        var saveResult: BerryFirmness = BerryFirmness()

        override fun findAll(): List<BerryFirmness> = emptyList()

        override fun findAll(specification: BerryFirmnessSpecification?): List<BerryFirmness> {
            listCondition = specification
            return listResult
        }

        override fun findAll(
            specification: BerryFirmnessSpecification?,
            pageable: Pageable,
        ): Page<BerryFirmness> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun findById(id: Long): BerryFirmness? = null

        override fun save(berryFirmness: BerryFirmness): BerryFirmness {
            savedBerryFirmness = berryFirmness
            return saveResult
        }

        override fun saveAndFlush(berryFirmness: BerryFirmness): BerryFirmness = save(berryFirmness)

        override fun deleteById(id: Long) {
            deletedId = id
        }

        override fun flush() = Unit
    }
}

private fun berryFirmnessEntity(
    id: Long,
    internalName: String,
    name: String,
): BerryFirmness =
    BerryFirmness {
        this.id = id
        this.internalName = internalName
        this.name = name
    }
