package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.MoveCategorySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveCategoryView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

/** 招式类别仓储接口 */
@Repository
interface MoveCategoryRepository : KRepository<MoveCategory, Long> {
    fun listViews(specification: MoveCategorySpecification?): List<MoveCategoryView> =
        sql
            .createQuery(MoveCategory::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(MoveCategoryView::class))
            }.execute()

    fun loadViewById(id: Long): MoveCategoryView? =
        sql
            .createQuery(MoveCategory::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(MoveCategoryView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
