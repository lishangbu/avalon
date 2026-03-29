package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.ItemSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * 道具仓储接口
 *
 * 定义道具数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
interface ItemRepository : KRepository<Item, Long> {
    /** 按条件查询道具视图 */
    fun listViews(specification: ItemSpecification?): List<ItemView> =
        sql
            .createQuery(Item::class) {
                specification?.let(::where)
                select(table.fetch(ItemView::class))
            }.execute()

    /** 按条件分页查询道具视图 */
    fun pageViews(
        specification: ItemSpecification?,
        pageable: Pageable,
    ): Page<ItemView> =
        sql
            .createQuery(Item::class) {
                specification?.let(::where)
                select(table.fetch(ItemView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 查询单个道具视图 */
    fun loadViewById(id: Long): ItemView? =
        sql
            .createQuery(Item::class) {
                where(table.id eq id)
                select(table.fetch(ItemView::class))
            }.execute()
            .firstOrNull()
}
