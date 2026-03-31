package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureShapeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureShapeView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

/** 生物形状仓储接口 */
@Repository
interface CreatureShapeRepository : KRepository<CreatureShape, Long> {
    /** 按条件查询生物形状视图 */
    fun listViews(specification: CreatureShapeSpecification?): List<CreatureShapeView> =
        sql
            .createQuery(CreatureShape::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(CreatureShapeView::class))
            }.execute()

    /** 按 ID 查询生物形状视图 */
    fun loadViewById(id: Long): CreatureShapeView? =
        sql
            .createQuery(CreatureShape::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(CreatureShapeView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
