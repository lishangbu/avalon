package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq

/**
 * 成长速率仓储接口
 *
 * 定义成长速率数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/2/10
 */
interface GrowthRateRepository : KRepository<GrowthRate, Long> {
    /** 按条件查询成长速率视图列表 */
    fun listViews(specification: GrowthRateSpecification?): List<GrowthRateView> =
        sql
            .createQuery(GrowthRate::class) {
                specification?.let(::where)
                select(table.fetch(GrowthRateView::class))
            }.execute()

    /** 按 ID 查询成长速率视图 */
    fun loadViewById(id: Long): GrowthRateView? =
        sql
            .createQuery(GrowthRate::class) {
                where(table.id eq id)
                select(table.fetch(GrowthRateView::class))
            }.execute()
            .firstOrNull()
}
