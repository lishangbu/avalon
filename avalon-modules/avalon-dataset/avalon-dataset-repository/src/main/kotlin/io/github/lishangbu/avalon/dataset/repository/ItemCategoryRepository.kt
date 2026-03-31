package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.ItemCategorySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemCategoryView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

/** 道具类别仓储接口 */
@Repository
interface ItemCategoryRepository : KRepository<ItemCategory, Long> {
    fun listViews(specification: ItemCategorySpecification?): List<ItemCategoryView> =
        sql
            .createQuery(ItemCategory::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(ItemCategoryView::class))
            }.execute()

    fun loadViewById(id: Long): ItemCategoryView? =
        sql
            .createQuery(ItemCategory::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(ItemCategoryView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
