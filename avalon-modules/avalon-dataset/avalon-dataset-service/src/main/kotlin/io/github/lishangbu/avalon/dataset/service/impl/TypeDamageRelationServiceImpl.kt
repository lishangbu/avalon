package io.github.lishangbu.avalon.dataset.service.impl

import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelationId
import io.github.lishangbu.avalon.dataset.repository.TypeDamageRelationRepository
import io.github.lishangbu.avalon.dataset.service.TypeDamageRelationService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/** 属性克制关系服务实现。 */
@Service
class TypeDamageRelationServiceImpl(
    private val typeDamageRelationRepository: TypeDamageRelationRepository,
) : TypeDamageRelationService {
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

    override fun save(relation: TypeDamageRelation): TypeDamageRelation = typeDamageRelationRepository.save(relation)

    override fun update(relation: TypeDamageRelation): TypeDamageRelation = typeDamageRelationRepository.save(relation)

    override fun removeById(id: TypeDamageRelationId) {
        typeDamageRelationRepository.deleteById(id)
    }

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
