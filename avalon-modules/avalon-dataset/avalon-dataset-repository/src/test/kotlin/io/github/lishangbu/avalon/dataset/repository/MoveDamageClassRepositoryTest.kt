package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassSpecification
import jakarta.annotation.Resource
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@Transactional
class MoveDamageClassRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var moveDamageClassRepository: MoveDamageClassRepository

    @Test
    fun shouldQueryPageAndCrudMoveDamageClass() {
        val condition = MoveDamageClassSpecification(internalName = "physical")

        val results = moveDamageClassRepository.findAll(condition)
        val page = moveDamageClassRepository.findAll(condition, PageRequest.of(0, 10))

        assertFalse(results.isEmpty())
        assertEquals(2L, results.first().id)
        assertEquals("physical", results.first().internalName)
        assertFalse(page.rows.isEmpty())

        val saved =
            moveDamageClassRepository.save(
                MoveDamageClass {
                    id = 990001L
                    internalName = "unit-damage-class"
                    name = "单元测试伤害分类"
                    description = "test description"
                },
                SaveMode.INSERT_ONLY,
            )

        val updated =
            requireNotNull(
                moveDamageClassRepository
                    .findAll(
                        MoveDamageClassSpecification(internalName = "unit-damage-class"),
                    ).firstOrNull(),
            )
        assertEquals(saved.id, updated.id)
        assertEquals("单元测试伤害分类", updated.name)

        moveDamageClassRepository.save(MoveDamageClass(updated) { description = "updated description" }, SaveMode.UPSERT)
        val afterUpdate =
            requireNotNull(
                moveDamageClassRepository
                    .findAll(
                        MoveDamageClassSpecification(internalName = "unit-damage-class"),
                    ).firstOrNull(),
            )
        assertEquals("updated description", afterUpdate.description)

        moveDamageClassRepository.deleteById(saved.id)
        assertTrue(
            moveDamageClassRepository
                .findAll(
                    MoveDamageClassSpecification(internalName = "unit-damage-class"),
                ).isEmpty(),
        )
    }

    private fun assertTrue(value: Boolean) {
        assertEquals(true, value)
    }
}
