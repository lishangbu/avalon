package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Berry
import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.entity.Type
import io.github.lishangbu.avalon.dataset.entity.dto.BerrySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryInput
import io.github.lishangbu.avalon.dataset.repository.BerryRepository
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class BerryServiceImplTest {
    @Test
    fun getPageByCondition_callsRepository() {
        val repository = FakeBerryRepository()
        val service = BerryServiceImpl(repository)
        val specification = BerrySpecification(id = "1", internalName = "cheri")
        val pageable = PageRequest.of(0, 5)
        repository.pageResult = Page(listOf(berryWithAssociations(1L)), 1, 1)

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals("1", result.rows.first().id)
        assertEquals("hard", result.rows.first().berryFirmnessInternalName)
        assertEquals(specification, repository.pageCondition)
        assertEquals(pageable, repository.pageable)
    }

    @Test
    fun save_usesRepository() {
        val repository = FakeBerryRepository()
        val service = BerryServiceImpl(repository)
        val command = SaveBerryInput("cheri", "樱子", 2, 3, 4, 5, 6, "7", "8", 9)
        repository.saveResult = berrySavedEntity(1L)
        repository.findByIdResult = berryWithAssociations(1L)

        val result = service.save(command)

        assertEquals("1", result.id)
        assertEquals("hard", result.berryFirmnessInternalName)
        assertEquals("fire", result.naturalGiftTypeInternalName)
        assertEquals("cheri", repository.savedBerry!!.internalName)
        assertEquals(7L, repository.savedBerry!!.berryFirmnessId)
        assertEquals(1L, repository.findByIdValue)
    }

    @Test
    fun update_usesRepository() {
        val repository = FakeBerryRepository()
        val service = BerryServiceImpl(repository)
        val command = UpdateBerryInput("1", "cheri", "樱子", 2, 3, 4, 5, 6, "7", "8", 9)
        repository.saveResult = berrySavedEntity(1L)
        repository.findByIdResult = berryWithAssociations(1L)

        val result = service.update(command)

        assertEquals("1", result.id)
        assertEquals("硬", result.berryFirmnessName)
        assertEquals(1L, repository.savedBerry!!.id)
        assertEquals(1L, repository.findByIdValue)
    }

    @Test
    fun removeById_callsRepository() {
        val repository = FakeBerryRepository()
        val service = BerryServiceImpl(repository)

        service.removeById(1L)

        assertEquals(1L, repository.deletedId)
    }

    private class FakeBerryRepository : BerryRepository {
        var pageCondition: BerrySpecification? = null
        var pageable: Pageable? = null
        var savedBerry: Berry? = null
        var deletedId: Long? = null
        var findByIdValue: Long? = null

        var listResult: List<Berry> = emptyList()
        var pageResult: Page<Berry> = Page(emptyList(), 0, 0)
        var saveResult: Berry = Berry()
        var findByIdResult: Berry? = null

        override fun findAll(specification: BerrySpecification?): List<Berry> {
            pageCondition = specification
            return listResult
        }

        override fun findAll(
            specification: BerrySpecification?,
            pageable: Pageable,
        ): Page<Berry> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun findById(id: Long): Berry? {
            findByIdValue = id
            return findByIdResult
        }

        override fun save(berry: Berry): Berry {
            savedBerry = berry
            return saveResult
        }

        override fun saveAndFlush(berry: Berry): Berry = save(berry)

        override fun deleteById(id: Long) {
            deletedId = id
        }

        override fun flush() = Unit
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
        berryFirmnessId = 7L
        naturalGiftTypeId = 8L
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
