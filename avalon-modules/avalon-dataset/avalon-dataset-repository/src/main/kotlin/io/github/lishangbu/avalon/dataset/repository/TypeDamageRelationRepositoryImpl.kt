package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class TypeDamageRelationRepositoryImpl(
    private val sql: KSqlClient,
) : TypeDamageRelationRepository {
    override fun findAll(
        attackingTypeId: Long?,
        defendingTypeId: Long?,
        multiplier: Float?,
    ): List<TypeDamageRelation> =
        sql
            .createQuery(TypeDamageRelation::class) {
                attackingTypeId?.let { where(table.id.attackingTypeId eq it) }
                defendingTypeId?.let { where(table.id.defendingTypeId eq it) }
                multiplier?.let { where(table.multiplier eq it) }
                select(table)
            }.execute()

    override fun findPage(
        attackingTypeId: Long?,
        defendingTypeId: Long?,
        multiplier: Float?,
        pageable: Pageable,
    ): Page<TypeDamageRelation> =
        sql
            .createQuery(TypeDamageRelation::class) {
                attackingTypeId?.let { where(table.id.attackingTypeId eq it) }
                defendingTypeId?.let { where(table.id.defendingTypeId eq it) }
                multiplier?.let { where(table.multiplier eq it) }
                select(table)
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    override fun save(typeDamageRelation: TypeDamageRelation): TypeDamageRelation = sql.save(typeDamageRelation).modifiedEntity

    override fun deleteById(id: TypeDamageRelationId) {
        sql
            .createDelete(TypeDamageRelation::class) {
                where(table.id.attackingTypeId eq id.attackingTypeId)
                where(table.id.defendingTypeId eq id.defendingTypeId)
                disableDissociation()
            }.execute()
    }
}
