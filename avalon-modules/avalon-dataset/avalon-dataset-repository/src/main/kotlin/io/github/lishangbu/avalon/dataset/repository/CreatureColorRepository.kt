package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureColorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureColorView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

/** 生物颜色仓储接口 */
@Repository
interface CreatureColorRepository : KRepository<CreatureColor, Long> {
    /** 按条件查询生物颜色视图 */
    fun listViews(specification: CreatureColorSpecification?): List<CreatureColorView> =
        sql
            .createQuery(CreatureColor::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(CreatureColorView::class))
            }.execute()

    /** 按 ID 查询生物颜色视图 */
    fun loadViewById(id: Long): CreatureColorView? =
        sql
            .createQuery(CreatureColor::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(CreatureColorView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
