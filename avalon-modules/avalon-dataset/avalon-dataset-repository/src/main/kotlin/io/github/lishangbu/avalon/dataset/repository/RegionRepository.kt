package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.RegionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.RegionView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

@Repository
interface RegionRepository : KRepository<Region, Long> {
    fun listViews(specification: RegionSpecification?): List<RegionView> =
        sql
            .createQuery(Region::class) {
                specification?.let(::where)
                select(table.fetch(RegionView::class))
            }.execute()

    fun loadViewById(id: Long): RegionView? =
        sql
            .createQuery(Region::class) {
                where(table.id eq id)
                select(table.fetch(RegionView::class))
            }.execute()
            .firstOrNull()
}
