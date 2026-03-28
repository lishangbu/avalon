package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveDamageClassInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveDamageClassInput
import io.github.lishangbu.avalon.dataset.repository.MoveDamageClassRepository
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest

class MoveDamageClassServiceImplTest {
    private val repository = mock(MoveDamageClassRepository::class.java)
    private val service = MoveDamageClassServiceImpl(repository)

    @Test
    fun getPageByCondition_callsRepository() {
        val specification = MoveDamageClassSpecification(id = "1", internalName = "physical")
        val pageable = PageRequest.of(0, 5)
        `when`(repository.findAll(specification, pageable)).thenReturn(
            Page(listOf(moveDamageClassEntity(1L, "physical", "物理", "desc")), 1, 1),
        )

        val result = service.getPageByCondition(specification, pageable)

        assertEquals(1, result.rows.size)
        assertEquals("1", result.rows.first().id)
    }

    @Test
    fun listByCondition_callsRepository() {
        val specification = MoveDamageClassSpecification(id = "1", internalName = "physical")
        `when`(repository.findAll(specification)).thenReturn(listOf(moveDamageClassEntity(1L, "physical", "物理", "desc")))

        val result = service.listByCondition(specification)

        assertEquals(1, result.size)
        assertEquals("1", result.first().id)
    }

    @Test
    fun save_usesInsertOnlyMode() {
        `when`(repository.save(any<MoveDamageClass>(), SaveMode.INSERT_ONLY)).thenReturn(moveDamageClassEntity(1L, "physical", "物理", "desc"))

        val result = service.save(SaveMoveDamageClassInput("physical", "物理", "desc"))

        assertEquals("1", result.id)
        verify(repository).save(any<MoveDamageClass>(), SaveMode.INSERT_ONLY)
    }

    @Test
    fun update_usesUpsertMode() {
        `when`(repository.save(any<MoveDamageClass>(), SaveMode.UPSERT)).thenReturn(moveDamageClassEntity(1L, "special", "特殊", "desc"))

        val result = service.update(UpdateMoveDamageClassInput("1", "special", "特殊", "desc"))

        assertEquals("1", result.id)
        verify(repository).save(any<MoveDamageClass>(), SaveMode.UPSERT)
    }

    @Test
    fun removeById_callsRepository() {
        service.removeById(1L)

        verify(repository).removeById(1L)
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
