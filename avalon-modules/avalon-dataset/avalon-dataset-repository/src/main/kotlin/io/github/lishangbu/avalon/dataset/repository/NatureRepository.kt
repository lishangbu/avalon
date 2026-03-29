package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.NatureSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.NatureView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq

/** 性格仓储接口 */
interface NatureRepository : KRepository<Nature, Long> {
    /** 按条件查询性格视图 */
    fun listViews(specification: NatureSpecification?): List<NatureView> =
        sql
            .createQuery(Nature::class) {
                specification?.let(::where)
                select(table.fetch(NatureView::class))
            }.execute()

    /** 按 ID 查询性格视图 */
    fun loadViewById(id: Long): NatureView? =
        sql
            .createQuery(Nature::class) {
                where(table.id eq id)
                select(table.fetch(NatureView::class))
            }.execute()
            .firstOrNull()
}
