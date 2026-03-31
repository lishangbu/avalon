package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.StatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.StatView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort

/**
 * 能力值仓储接口
 *
 * 定义能力值数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/2/11
 */
interface StatRepository : KRepository<Stat, Long> {
    /** 按条件查询能力值列表 */
    fun listViews(specification: StatSpecification?): List<StatView> =
        sql
            .createQuery(Stat::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(StatView::class))
            }.execute()

    /** 按 ID 查询能力值 */
    fun loadViewById(id: Long): StatView? =
        sql
            .createQuery(Stat::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(StatView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
