package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.RegionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.RegionView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
interface RegionRepository : KRepository<Region, Long> {
    fun listViews(specification: RegionSpecification?): List<RegionView> =
        sql
            .createQuery(Region::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(RegionView::class))
            }.execute()

    fun loadViewById(id: Long): RegionView? =
        sql
            .createQuery(Region::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(RegionView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
