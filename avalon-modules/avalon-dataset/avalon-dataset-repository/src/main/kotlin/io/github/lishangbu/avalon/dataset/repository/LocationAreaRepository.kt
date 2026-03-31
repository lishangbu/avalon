package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.LocationAreaSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.LocationAreaView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
interface LocationAreaRepository : KRepository<LocationArea, Long> {
    fun pageViews(
        specification: LocationAreaSpecification?,
        pageable: Pageable,
    ): Page<LocationAreaView> =
        sql
            .createQuery(LocationArea::class) {
                specification?.let(::where)
                select(table.fetch(LocationAreaView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    fun loadViewById(id: Long): LocationAreaView? =
        sql
            .createQuery(LocationArea::class) {
                where(table.id eq id)
                select(table.fetch(LocationAreaView::class))
            }.execute()
            .firstOrNull()
}
