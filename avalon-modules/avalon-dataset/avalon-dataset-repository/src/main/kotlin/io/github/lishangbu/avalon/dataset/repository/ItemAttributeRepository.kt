package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

/** 道具属性仓储接口 */
@Repository
interface ItemAttributeRepository : KRepository<ItemAttribute, Long> {
    fun listViews(specification: ItemAttributeSpecification?): List<ItemAttributeView> =
        sql
            .createQuery(ItemAttribute::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(ItemAttributeView::class))
            }.execute()

    fun loadViewById(id: Long): ItemAttributeView? =
        sql
            .createQuery(ItemAttribute::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(ItemAttributeView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
