package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterMethodSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterMethodView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

/** 遭遇方式仓储接口 */
@Repository
interface EncounterMethodRepository : KRepository<EncounterMethod, Long> {
    /** 按条件查询遭遇方式视图 */
    fun listViews(specification: EncounterMethodSpecification?): List<EncounterMethodView> =
        sql
            .createQuery(EncounterMethod::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(EncounterMethodView::class))
            }.execute()

    /** 按 ID 查询遭遇方式视图 */
    fun loadViewById(id: Long): EncounterMethodView? =
        sql
            .createQuery(EncounterMethod::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(EncounterMethodView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
