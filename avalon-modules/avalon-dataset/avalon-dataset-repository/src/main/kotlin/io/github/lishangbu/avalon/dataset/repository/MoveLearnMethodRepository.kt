package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.MoveLearnMethodSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveLearnMethodView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

/** 招式学习方式仓储接口 */
@Repository
interface MoveLearnMethodRepository : KRepository<MoveLearnMethod, Long> {
    fun listViews(specification: MoveLearnMethodSpecification?): List<MoveLearnMethodView> =
        sql
            .createQuery(MoveLearnMethod::class) {
                specification?.let(::where)
                select(table.fetch(MoveLearnMethodView::class))
            }.execute()

    fun loadViewById(id: Long): MoveLearnMethodView? =
        sql
            .createQuery(MoveLearnMethod::class) {
                where(table.id eq id)
                select(table.fetch(MoveLearnMethodView::class))
            }.execute()
            .firstOrNull()
}
