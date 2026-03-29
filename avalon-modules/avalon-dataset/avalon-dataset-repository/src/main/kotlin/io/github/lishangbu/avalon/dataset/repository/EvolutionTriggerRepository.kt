package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionTriggerSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionTriggerView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

/** 进化触发方式仓储接口 */
@Repository
interface EvolutionTriggerRepository : KRepository<EvolutionTrigger, Long> {
    /** 按条件查询进化触发方式视图 */
    fun listViews(specification: EvolutionTriggerSpecification?): List<EvolutionTriggerView> =
        sql
            .createQuery(EvolutionTrigger::class) {
                specification?.let(::where)
                select(table.fetch(EvolutionTriggerView::class))
            }.execute()

    /** 按 ID 查询进化触发方式视图 */
    fun loadViewById(id: Long): EvolutionTriggerView? =
        sql
            .createQuery(EvolutionTrigger::class) {
                where(table.id eq id)
                select(table.fetch(EvolutionTriggerView::class))
            }.execute()
            .firstOrNull()
}
