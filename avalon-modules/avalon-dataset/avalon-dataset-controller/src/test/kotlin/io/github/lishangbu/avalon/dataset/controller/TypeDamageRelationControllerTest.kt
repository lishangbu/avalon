package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelationId
import io.github.lishangbu.avalon.dataset.service.TypeDamageRelationService
import org.babyfish.jimmer.Page
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class TypeDamageRelationControllerTest {
    @Test
    fun getTypeDamageRelationPage_delegatesToService() {
        val service = FakeTypeDamageRelationService()
        val controller = TypeDamageRelationController(service)
        val pageIndex = 0
        val pageSize = 5
        val pageable = PageRequest.of(pageIndex, pageSize)
        val page: Page<TypeDamageRelation> = Page(listOf(TypeDamageRelation()), 1, 1)
        service.pageResult = page

        val result = controller.getTypeDamageRelationPage(pageable, 1L, 2L, 2.0f)

        assertSame(page, result)
        assertSame(pageable, service.pageable)
        assertEquals(1L, service.pageAttackingTypeId)
        assertEquals(2L, service.pageDefendingTypeId)
        assertEquals(2.0f, service.pageMultiplier)
    }

    @Test
    fun listTypeDamageRelations_delegatesToService() {
        val service = FakeTypeDamageRelationService()
        val controller = TypeDamageRelationController(service)
        val list = listOf(TypeDamageRelation())
        service.listResult = list

        val result = controller.listTypeDamageRelations(1L, 2L, 0.5f)

        assertSame(list, result)
        assertEquals(1L, service.listAttackingTypeId)
        assertEquals(2L, service.listDefendingTypeId)
        assertEquals(0.5f, service.listMultiplier)
    }

    @Test
    fun save_delegatesToService() {
        val service = FakeTypeDamageRelationService()
        val controller = TypeDamageRelationController(service)
        val relation = TypeDamageRelation()
        service.saveResult = relation

        val result = controller.save(relation)

        assertSame(relation, result)
        assertSame(relation, service.savedRelation)
    }

    @Test
    fun update_delegatesToService() {
        val service = FakeTypeDamageRelationService()
        val controller = TypeDamageRelationController(service)
        val relation = TypeDamageRelation()
        service.updateResult = relation

        val result = controller.update(relation)

        assertSame(relation, result)
        assertSame(relation, service.updatedRelation)
    }

    @Test
    fun deleteById_delegatesToService() {
        val service = FakeTypeDamageRelationService()
        val controller = TypeDamageRelationController(service)

        controller.deleteById(1L, 2L)

        assertNotNull(service.removedId)
        assertEquals(1L, service.removedId!!.attackingTypeId)
        assertEquals(2L, service.removedId!!.defendingTypeId)
    }

    private class FakeTypeDamageRelationService : TypeDamageRelationService {
        var pageAttackingTypeId: Long? = null
        var pageDefendingTypeId: Long? = null
        var pageMultiplier: Float? = null
        var pageable: Pageable? = null

        var listAttackingTypeId: Long? = null
        var listDefendingTypeId: Long? = null
        var listMultiplier: Float? = null

        var savedRelation: TypeDamageRelation? = null
        var updatedRelation: TypeDamageRelation? = null
        var removedId: TypeDamageRelationId? = null

        var pageResult: Page<TypeDamageRelation> = Page(emptyList(), 0, 0)
        var listResult: List<TypeDamageRelation> = emptyList()
        var saveResult: TypeDamageRelation = TypeDamageRelation()
        var updateResult: TypeDamageRelation = TypeDamageRelation()

        override fun getPageByCondition(
            attackingTypeId: Long?,
            defendingTypeId: Long?,
            multiplier: Float?,
            pageable: Pageable,
        ): Page<TypeDamageRelation> {
            pageAttackingTypeId = attackingTypeId
            pageDefendingTypeId = defendingTypeId
            pageMultiplier = multiplier
            this.pageable = pageable
            return pageResult
        }

        override fun save(relation: TypeDamageRelation): TypeDamageRelation {
            savedRelation = relation
            return saveResult
        }

        override fun update(relation: TypeDamageRelation): TypeDamageRelation {
            updatedRelation = relation
            return updateResult
        }

        override fun removeById(id: TypeDamageRelationId) {
            removedId = id
        }

        override fun listByCondition(
            attackingTypeId: Long?,
            defendingTypeId: Long?,
            multiplier: Float?,
        ): List<TypeDamageRelation> {
            listAttackingTypeId = attackingTypeId
            listDefendingTypeId = defendingTypeId
            listMultiplier = multiplier
            return listResult
        }
    }
}
