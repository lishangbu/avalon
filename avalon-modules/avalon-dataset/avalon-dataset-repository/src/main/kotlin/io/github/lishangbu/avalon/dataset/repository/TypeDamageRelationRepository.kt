package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelationId
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/**
 * 属性克制关系数据访问层
 *
 * 由 Jimmer 实现的仓储契约，保持原有调用方式兼容
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface TypeDamageRelationRepository {
    fun findAll(
        attackingTypeId: Long?,
        defendingTypeId: Long?,
        multiplier: Float?,
    ): List<TypeDamageRelation>

    fun findPage(
        attackingTypeId: Long?,
        defendingTypeId: Long?,
        multiplier: Float?,
        pageable: Pageable,
    ): Page<TypeDamageRelation>

    fun save(typeDamageRelation: TypeDamageRelation): TypeDamageRelation

    fun deleteById(id: TypeDamageRelationId)
}
