package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.dto.SaveTypeInput
import io.github.lishangbu.avalon.dataset.entity.dto.TypeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateTypeInput
import io.github.lishangbu.avalon.dataset.repository.TypeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.github.lishangbu.avalon.dataset.entity.Type as DatasetType

class TypeServiceImplTest {
    @Test
    fun listByCondition_callsRepository() {
        val repository = FakeTypeRepository()
        val service = TypeServiceImpl(repository)
        val specification = TypeSpecification(id = "1", internalName = "fire", name = "火")
        repository.listResult = listOf(typeEntity(1L, "fire", "火"))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals("火", result.first().name)
        assertEquals(specification, repository.listCondition)
    }

    @Test
    fun save_usesRepository() {
        val repository = FakeTypeRepository()
        val service = TypeServiceImpl(repository)
        val command = SaveTypeInput("fire", "火")
        repository.saveResult = typeEntity(1L, "fire", "火")

        val result = service.save(command)

        assertEquals("1", result.id)
        assertEquals("fire", repository.savedType!!.internalName)
        assertEquals("火", repository.savedType!!.name)
    }

    @Test
    fun update_usesRepository() {
        val repository = FakeTypeRepository()
        val service = TypeServiceImpl(repository)
        val command = UpdateTypeInput("1", "water", "水")
        repository.saveResult = typeEntity(1L, "water", "水")

        val result = service.update(command)

        assertEquals("1", result.id)
        assertEquals(1L, repository.savedType!!.id)
        assertEquals("water", repository.savedType!!.internalName)
    }

    @Test
    fun removeById_callsRepository() {
        val repository = FakeTypeRepository()
        val service = TypeServiceImpl(repository)

        service.removeById(1L)

        assertEquals(1L, repository.deletedId)
    }

    private class FakeTypeRepository : TypeRepository {
        var listCondition: TypeSpecification? = null
        var savedType: DatasetType? = null
        var deletedId: Long? = null

        var listResult: List<DatasetType> = emptyList()
        var saveResult: DatasetType = DatasetType()

        override fun findAll(): List<DatasetType> = emptyList()

        override fun findAll(specification: TypeSpecification?): List<DatasetType> {
            listCondition = specification
            return listResult
        }

        override fun findById(id: Long): DatasetType? = null

        override fun save(type: DatasetType): DatasetType {
            savedType = type
            return saveResult
        }

        override fun saveAndFlush(type: DatasetType): DatasetType = save(type)

        override fun deleteById(id: Long) {
            deletedId = id
        }

        override fun flush() = Unit
    }
}

private fun typeEntity(
    id: Long,
    internalName: String,
    name: String,
): DatasetType =
    DatasetType {
        this.id = id
        this.internalName = internalName
        this.name = name
    }
