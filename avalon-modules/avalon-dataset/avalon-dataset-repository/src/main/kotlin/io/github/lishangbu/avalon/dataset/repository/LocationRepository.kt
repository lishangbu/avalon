package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.LocationSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.LocationView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
interface LocationRepository : KRepository<Location, Long> {
    fun listViews(specification: LocationSpecification?): List<LocationView> =
        sql
            .createQuery(Location::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(LocationView::class))
            }.execute()

    fun pageViews(
        specification: LocationSpecification?,
        pageable: Pageable,
    ): Page<LocationView> =
        sql
            .createQuery(Location::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(LocationView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    fun loadViewById(id: Long): LocationView? =
        sql
            .createQuery(Location::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(LocationView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
