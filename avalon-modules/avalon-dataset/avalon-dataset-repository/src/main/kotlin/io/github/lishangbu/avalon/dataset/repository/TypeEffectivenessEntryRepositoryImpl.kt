package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class TypeEffectivenessEntryRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : TypeEffectivenessEntryRepositoryExt {
    /** 按条件查询属性相克条目列表 */
    override fun findAll(
        attackingTypeId: Long?,
        defendingTypeId: Long?,
        multiplierPercent: Int?,
    ): List<TypeEffectivenessEntry> =
        sql
            .createQuery(TypeEffectivenessEntry::class) {
                attackingTypeId?.let { where(table.id.attackingTypeId eq it) }
                defendingTypeId?.let { where(table.id.defendingTypeId eq it) }
                multiplierPercent?.let { where(table.multiplierPercent eq it) }
                select(table)
            }.execute()

    /** 按条件分页查询属性相克条目 */
    override fun findPage(
        attackingTypeId: Long?,
        defendingTypeId: Long?,
        multiplierPercent: Int?,
        pageable: Pageable,
    ): Page<TypeEffectivenessEntry> =
        sql
            .createQuery(TypeEffectivenessEntry::class) {
                attackingTypeId?.let { where(table.id.attackingTypeId eq it) }
                defendingTypeId?.let { where(table.id.defendingTypeId eq it) }
                multiplierPercent?.let { where(table.multiplierPercent eq it) }
                select(table)
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 删除属性相克条目 */
    override fun removeById(id: TypeEffectivenessEntryId) {
        sql
            .createDelete(TypeEffectivenessEntry::class) {
                where(table.id.attackingTypeId eq id.attackingTypeId)
                where(table.id.defendingTypeId eq id.defendingTypeId)
                disableDissociation()
            }.execute()
    }
}
