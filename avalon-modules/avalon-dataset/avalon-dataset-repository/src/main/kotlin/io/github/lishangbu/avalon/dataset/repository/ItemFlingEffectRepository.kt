package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.ItemFlingEffectSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemFlingEffectView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

/** 道具投掷效果仓储接口 */
@Repository
interface ItemFlingEffectRepository : KRepository<ItemFlingEffect, Long> {
    fun listViews(specification: ItemFlingEffectSpecification?): List<ItemFlingEffectView> =
        sql
            .createQuery(ItemFlingEffect::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(ItemFlingEffectView::class))
            }.execute()

    fun loadViewById(id: Long): ItemFlingEffectView? =
        sql
            .createQuery(ItemFlingEffect::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(ItemFlingEffectView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
