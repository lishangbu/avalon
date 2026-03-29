package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.MoveTargetSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveTargetView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

/** 招式目标仓储接口 */
@Repository
interface MoveTargetRepository : KRepository<MoveTarget, Long> {
    fun listViews(specification: MoveTargetSpecification?): List<MoveTargetView> =
        sql
            .createQuery(MoveTarget::class) {
                specification?.let(::where)
                select(table.fetch(MoveTargetView::class))
            }.execute()

    fun loadViewById(id: Long): MoveTargetView? =
        sql
            .createQuery(MoveTarget::class) {
                where(table.id eq id)
                select(table.fetch(MoveTargetView::class))
            }.execute()
            .firstOrNull()
}
