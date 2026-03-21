package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelationId
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 属性克制关系服务。 */
interface TypeDamageRelationService {
    /** 根据条件分页查询属性克制关系。 */
    fun getPageByCondition(
        attackingTypeId: Long?,
        defendingTypeId: Long?,
        multiplier: Float?,
        pageable: Pageable,
    ): Page<TypeDamageRelation>

    /** 新增属性克制关系。 */
    fun save(relation: TypeDamageRelation): TypeDamageRelation

    /** 更新属性克制关系。 */
    fun update(relation: TypeDamageRelation): TypeDamageRelation

    /** 根据主键删除属性克制关系。 */
    fun removeById(id: TypeDamageRelationId)

    /** 根据条件查询属性克制关系列表。 */
    fun listByCondition(
        attackingTypeId: Long?,
        defendingTypeId: Long?,
        multiplier: Float?,
    ): List<TypeDamageRelation>
}
