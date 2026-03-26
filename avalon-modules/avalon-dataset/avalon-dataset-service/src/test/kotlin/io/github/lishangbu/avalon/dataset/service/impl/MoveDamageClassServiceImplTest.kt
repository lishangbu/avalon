package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveDamageClassInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveDamageClassInput
import io.github.lishangbu.avalon.dataset.repository.MoveDamageClassRepository
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class MoveDamageClassServiceImplTest {
    @Test
    fun getPageByCondition_callsRepository() {
        val repository = FakeMoveDamageClassRepository()
        val service = MoveDamageClassServiceImpl(repository)
        val specification = MoveDamageClassSpecification(id = "1", internalName = "physical")
        val pageable = PageRequest.of(0, 5)
        repository.pageResult = Page(listOf(moveDamageClassEntity(1L, "physical", "物理", "desc")), 1, 1)

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals("1", result.rows.first().id)
        assertEquals(specification, repository.pageCondition)
        assertEquals(pageable, repository.pageable)
    }

    @Test
    fun listByCondition_callsRepository() {
        val repository = FakeMoveDamageClassRepository()
        val service = MoveDamageClassServiceImpl(repository)
        val specification = MoveDamageClassSpecification(id = "1", internalName = "physical")
        repository.listResult = listOf(moveDamageClassEntity(1L, "physical", "物理", "desc"))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
        assertEquals(specification, repository.listCondition)
    }

    @Test
    fun save_usesRepository() {
        val repository = FakeMoveDamageClassRepository()
        val service = MoveDamageClassServiceImpl(repository)
        val command = SaveMoveDamageClassInput("physical", "物理", "desc")
        repository.saveResult = moveDamageClassEntity(1L, "physical", "物理", "desc")

        val result = service.save(command)

        assertEquals("1", result.id)
        assertEquals("physical", repository.savedMoveDamageClass!!.internalName)
    }

    @Test
    fun update_usesRepository() {
        val repository = FakeMoveDamageClassRepository()
        val service = MoveDamageClassServiceImpl(repository)
        val command = UpdateMoveDamageClassInput("1", "special", "特殊", "desc")
        repository.saveResult = moveDamageClassEntity(1L, "special", "特殊", "desc")

        val result = service.update(command)

        assertEquals("1", result.id)
        assertEquals(1L, repository.savedMoveDamageClass!!.id)
        assertEquals("special", repository.savedMoveDamageClass!!.internalName)
    }

    @Test
    fun removeById_callsRepository() {
        val repository = FakeMoveDamageClassRepository()
        val service = MoveDamageClassServiceImpl(repository)

        service.removeById(1L)

        assertEquals(1L, repository.deletedId)
    }

    private class FakeMoveDamageClassRepository : MoveDamageClassRepository {
        var pageCondition: MoveDamageClassSpecification? = null
        var pageable: Pageable? = null
        var listCondition: MoveDamageClassSpecification? = null
        var savedMoveDamageClass: MoveDamageClass? = null
        var deletedId: Long? = null

        var pageResult: Page<MoveDamageClass> = Page(emptyList(), 0, 0)
        var listResult: List<MoveDamageClass> = emptyList()
        var saveResult: MoveDamageClass = MoveDamageClass()

        override fun findAll(specification: MoveDamageClassSpecification?): List<MoveDamageClass> {
            listCondition = specification
            return listResult
        }

        override fun findAll(
            specification: MoveDamageClassSpecification?,
            pageable: Pageable,
        ): Page<MoveDamageClass> {
            pageCondition = specification
            this.pageable = pageable
            return pageResult
        }

        override fun save(moveDamageClass: MoveDamageClass): MoveDamageClass {
            savedMoveDamageClass = moveDamageClass
            return saveResult
        }

        override fun deleteById(id: Long) {
            deletedId = id
        }
    }
}

private fun moveDamageClassEntity(
    id: Long,
    internalName: String,
    name: String,
    description: String,
): MoveDamageClass =
    MoveDamageClass {
        this.id = id
        this.internalName = internalName
        this.name = name
        this.description = description
    }
