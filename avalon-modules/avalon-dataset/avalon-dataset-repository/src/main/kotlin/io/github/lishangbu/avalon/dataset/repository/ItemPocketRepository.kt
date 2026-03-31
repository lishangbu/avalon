package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.ItemPocketSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemPocketView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

/** 道具口袋仓储接口 */
@Repository
interface ItemPocketRepository : KRepository<ItemPocket, Long> {
    fun listViews(specification: ItemPocketSpecification?): List<ItemPocketView> =
        sql
            .createQuery(ItemPocket::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(ItemPocketView::class))
            }.execute()

    fun loadViewById(id: Long): ItemPocketView? =
        sql
            .createQuery(ItemPocket::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(ItemPocketView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
