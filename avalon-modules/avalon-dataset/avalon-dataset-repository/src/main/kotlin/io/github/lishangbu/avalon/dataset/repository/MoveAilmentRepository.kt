package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.MoveAilmentSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveAilmentView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

/** 招式异常仓储接口 */
@Repository
interface MoveAilmentRepository : KRepository<MoveAilment, Long> {
    fun listViews(specification: MoveAilmentSpecification?): List<MoveAilmentView> =
        sql
            .createQuery(MoveAilment::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(MoveAilmentView::class))
            }.execute()

    fun loadViewById(id: Long): MoveAilmentView? =
        sql
            .createQuery(MoveAilment::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(MoveAilmentView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
