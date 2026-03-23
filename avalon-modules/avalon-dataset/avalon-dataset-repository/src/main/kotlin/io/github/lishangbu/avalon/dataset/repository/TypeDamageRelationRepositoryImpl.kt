package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class TypeDamageRelationRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : TypeDamageRelationRepository {
    /** 按条件查询属性克制关系列表 */
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

    /** 按条件分页查询属性克制关系 */
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

    /** 保存属性克制关系 */
    override fun save(typeDamageRelation: TypeDamageRelation): TypeDamageRelation = sql.save(typeDamageRelation).modifiedEntity

    /** 按 ID 删除属性克制关系 */
    override fun deleteById(id: TypeDamageRelationId) {
        sql
            .createDelete(TypeDamageRelation::class) {
                where(table.id.attackingTypeId eq id.attackingTypeId)
                where(table.id.defendingTypeId eq id.defendingTypeId)
                disableDissociation()
            }.execute()
    }
}
