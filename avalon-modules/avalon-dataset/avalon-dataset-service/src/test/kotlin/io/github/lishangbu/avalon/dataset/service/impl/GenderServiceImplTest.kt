package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.Gender
import io.github.lishangbu.avalon.dataset.entity.dto.GenderSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.SaveGenderInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateGenderInput
import io.github.lishangbu.avalon.dataset.repository.GenderRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GenderServiceImplTest {
    @Test
    fun listByCondition_callsRepository() {
        val repository = FakeGenderRepository()
        val service = GenderServiceImpl(repository)
        val specification = GenderSpecification(id = "1", internalName = "female")
        repository.listResult = listOf(genderEntity(1L, "female", "雌性"))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("female", result.first().internalName)
        assertEquals(specification, repository.listCondition)
    }

    @Test
    fun save_usesRepository() {
        val repository = FakeGenderRepository()
        val service = GenderServiceImpl(repository)
        val command = SaveGenderInput("female", "雌性")
        repository.saveResult = genderEntity(1L, "female", "雌性")

        val result = service.save(command)

        assertEquals("1", result.id)
        assertEquals("female", repository.savedGender!!.internalName)
    }

    @Test
    fun update_usesRepository() {
        val repository = FakeGenderRepository()
        val service = GenderServiceImpl(repository)
        val command = UpdateGenderInput("1", "male", "雄性")
        repository.saveResult = genderEntity(1L, "male", "雄性")

        val result = service.update(command)

        assertEquals("1", result.id)
        assertEquals(1L, repository.savedGender!!.id)
        assertEquals("male", repository.savedGender!!.internalName)
    }

    @Test
    fun removeById_callsRepository() {
        val repository = FakeGenderRepository()
        val service = GenderServiceImpl(repository)

        service.removeById(1L)

        assertEquals(1L, repository.deletedId)
    }

    private class FakeGenderRepository : GenderRepository {
        var listCondition: GenderSpecification? = null
        var savedGender: Gender? = null
        var deletedId: Long? = null

        var listResult: List<Gender> = emptyList()
        var saveResult: Gender = Gender {}

        override fun findAll(): List<Gender> = emptyList()

        override fun findAll(specification: GenderSpecification?): List<Gender> {
            listCondition = specification
            return listResult
        }

        override fun findById(id: Long): Gender? = null

        override fun save(gender: Gender): Gender {
            savedGender = gender
            return saveResult
        }

        override fun saveAndFlush(gender: Gender): Gender = save(gender)

        override fun deleteById(id: Long) {
            deletedId = id
        }

        override fun flush() = Unit
    }
}

private fun genderEntity(
    id: Long,
    internalName: String,
    name: String,
): Gender =
    Gender {
        this.id = id
        this.internalName = internalName
        this.name = name
    }
