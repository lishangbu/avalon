package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemAttributeView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

/** 道具属性仓储接口 */
@Repository
interface ItemAttributeRepository : KRepository<ItemAttribute, Long> {
    fun listViews(specification: ItemAttributeSpecification?): List<ItemAttributeView> =
        sql
            .createQuery(ItemAttribute::class) {
                specification?.let(::where)
                select(table.fetch(ItemAttributeView::class))
            }.execute()

    fun loadViewById(id: Long): ItemAttributeView? =
        sql
            .createQuery(ItemAttribute::class) {
                where(table.id eq id)
                select(table.fetch(ItemAttributeView::class))
            }.execute()
            .firstOrNull()
}
