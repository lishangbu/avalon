package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable

/**
 * 属性相克条目仓储接口
 *
 * 定义属性相克矩阵单元格的查询与持久化操作。
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface TypeEffectivenessEntryRepository : KRepository<TypeEffectivenessEntry, TypeEffectivenessEntryId> {
    /** 按条件查询属性相克条目列表 */
    fun listByFilter(
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
    fun pageByFilter(
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
}
