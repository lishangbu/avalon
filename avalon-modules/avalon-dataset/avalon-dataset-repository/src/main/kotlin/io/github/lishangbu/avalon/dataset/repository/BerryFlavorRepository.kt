package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort

/**
 * 树果风味仓储接口
 *
 * 定义树果风味数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface BerryFlavorRepository : KRepository<BerryFlavor, Long> {
    /** 按条件查询树果风味视图列表 */
    fun listViews(specification: BerryFlavorSpecification?): List<BerryFlavorView> =
        sql
            .createQuery(BerryFlavor::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(BerryFlavorView::class))
            }.execute()

    /** 按 ID 查询树果风味视图 */
    fun loadViewById(id: Long): BerryFlavorView? =
        sql
            .createQuery(BerryFlavor::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(BerryFlavorView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
