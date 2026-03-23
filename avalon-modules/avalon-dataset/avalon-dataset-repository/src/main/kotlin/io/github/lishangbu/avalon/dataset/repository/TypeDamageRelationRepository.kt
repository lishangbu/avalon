package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelationId
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/**
 * 属性克制关系仓储接口
 *
 * 定义属性克制关系数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface TypeDamageRelationRepository {
    /** 按条件查询属性克制关系列表 */
    fun findAll(
        attackingTypeId: Long?,
        defendingTypeId: Long?,
        multiplier: Float?,
    ): List<TypeDamageRelation>

    /** 按条件分页查询属性克制关系 */
    fun findPage(
        attackingTypeId: Long?,
        defendingTypeId: Long?,
        multiplier: Float?,
        pageable: Pageable,
    ): Page<TypeDamageRelation>

    /** 保存属性克制关系 */
    fun save(typeDamageRelation: TypeDamageRelation): TypeDamageRelation

    /** 按 ID 删除属性克制关系 */
    fun deleteById(id: TypeDamageRelationId)
}
