package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

/** 遭遇条件仓储接口 */
@Repository
interface EncounterConditionRepository : KRepository<EncounterCondition, Long> {
    /** 按条件查询遭遇条件视图 */
    fun listViews(specification: EncounterConditionSpecification?): List<EncounterConditionView> =
        sql
            .createQuery(EncounterCondition::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(EncounterConditionView::class))
            }.execute()

    /** 按 ID 查询遭遇条件视图 */
    fun loadViewById(id: Long): EncounterConditionView? =
        sql
            .createQuery(EncounterCondition::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(EncounterConditionView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
