package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.BerrySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

/**
 * 树果仓储接口
 *
 * 定义树果的查询、保存与删除操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface BerryRepository : KRepository<Berry, Long> {
    /** 按条件查询树果视图 */
    fun listViews(specification: BerrySpecification?): List<BerryView> =
        sql
            .createQuery(Berry::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(BerryView::class))
            }.execute()

    /** 按条件分页查询树果视图 */
    fun pageViews(
        specification: BerrySpecification?,
        pageable: Pageable,
    ): Page<BerryView> =
        sql
            .createQuery(Berry::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(BerryView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 查询单个树果视图 */
    fun loadViewById(id: Long): BerryView? =
        sql
            .createQuery(Berry::class) {
                where(table.id eq id)
                select(table.fetch(BerryView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
