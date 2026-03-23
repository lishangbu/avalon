package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelationId
import io.github.lishangbu.avalon.dataset.repository.TypeDamageRelationRepository
import io.github.lishangbu.avalon.dataset.service.TypeDamageRelationService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** 属性克制关系服务实现*/
@Service
class TypeDamageRelationServiceImpl(
    /** 属性克制关系仓储*/
    private val typeDamageRelationRepository: TypeDamageRelationRepository,
) : TypeDamageRelationService {
    /** 按条件分页查询属性克制关系*/
    override fun getPageByCondition(
        attackingTypeId: Long?,
        defendingTypeId: Long?,
        multiplier: Float?,
        pageable: Pageable,
    ): Page<TypeDamageRelation> =
        typeDamageRelationRepository.findPage(
            attackingTypeId = attackingTypeId,
            defendingTypeId = defendingTypeId,
            multiplier = multiplier,
            pageable = pageable,
        )

    /** 保存属性克制关系*/
    override fun save(relation: TypeDamageRelation): TypeDamageRelation = typeDamageRelationRepository.save(relation)

    /** 更新属性克制关系*/
    override fun update(relation: TypeDamageRelation): TypeDamageRelation = typeDamageRelationRepository.save(relation)

    /** 按 ID 删除属性克制关系*/
    override fun removeById(id: TypeDamageRelationId) {
        typeDamageRelationRepository.deleteById(id)
    }

    /** 根据条件查询属性克制关系列表*/
    override fun listByCondition(
        attackingTypeId: Long?,
        defendingTypeId: Long?,
        multiplier: Float?,
    ): List<TypeDamageRelation> =
        typeDamageRelationRepository.findAll(
            attackingTypeId = attackingTypeId,
            defendingTypeId = defendingTypeId,
            multiplier = multiplier,
        )
}
