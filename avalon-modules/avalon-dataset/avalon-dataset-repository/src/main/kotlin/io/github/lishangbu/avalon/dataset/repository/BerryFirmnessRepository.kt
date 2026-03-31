package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

/**
 * 树果硬度仓储接口
 *
 * 定义树果硬度数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface BerryFirmnessRepository : KRepository<BerryFirmness, Long> {
    /** 按条件查询树果硬度视图列表 */
    fun listViews(specification: BerryFirmnessSpecification?): List<BerryFirmnessView> =
        sql
            .createQuery(BerryFirmness::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(BerryFirmnessView::class))
            }.execute()

    /** 按条件分页查询树果硬度视图 */
    fun pageViews(
        specification: BerryFirmnessSpecification?,
        pageable: Pageable,
    ): Page<BerryFirmnessView> =
        sql
            .createQuery(BerryFirmness::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(BerryFirmnessView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 查询树果硬度视图 */
    fun loadViewById(id: Long): BerryFirmnessView? =
        sql
            .createQuery(BerryFirmness::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(BerryFirmnessView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
