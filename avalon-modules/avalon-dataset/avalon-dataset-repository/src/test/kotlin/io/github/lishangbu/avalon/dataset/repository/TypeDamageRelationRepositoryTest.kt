package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation
import jakarta.annotation.Resource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@Transactional
class TypeDamageRelationRepositoryTest : AbstractRepositoryTest() {
    @Resource
    private lateinit var typeDamageRelationRepository: TypeDamageRelationRepository

    @Test
    fun shouldQueryPageAndCrudTypeDamageRelation() {
        val results = typeDamageRelationRepository.findAll(2L, null, 2f)
        val page = typeDamageRelationRepository.findPage(2L, null, 2f, PageRequest.of(0, 10))

        assertFalse(results.isEmpty())
        assertFalse(page.rows.isEmpty())

        val existing = results.first()
        val updated =
            typeDamageRelationRepository.save(
                TypeDamageRelation(existing) {
                    multiplier = 1.5f
                },
            )

        val foundAfterUpdate =
            typeDamageRelationRepository.findAll(
                updated.id.attackingTypeId,
                updated.id.defendingTypeId,
                1.5f,
            )
        assertFalse(foundAfterUpdate.isEmpty())

        typeDamageRelationRepository.deleteById(updated.id)
        val afterDelete =
            typeDamageRelationRepository.findAll(
                updated.id.attackingTypeId,
                updated.id.defendingTypeId,
                1.5f,
            )
        assertEquals(0, afterDelete.size)
    }
}
