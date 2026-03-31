package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureVariantSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureVariantView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
interface CreatureVariantRepository : KRepository<CreatureVariant, Long> {
    fun listAll(): List<CreatureVariant> =
        sql
            .createQuery(CreatureVariant::class) {
                orderBy(DEFAULT_SORT)
                select(table)
            }.execute()

    fun pageViews(
        specification: CreatureVariantSpecification?,
        pageable: Pageable,
    ): Page<CreatureVariantView> =
        sql
            .createQuery(CreatureVariant::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(CreatureVariantView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    fun loadViewById(id: Long): CreatureVariantView? =
        sql
            .createQuery(CreatureVariant::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(CreatureVariantView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
