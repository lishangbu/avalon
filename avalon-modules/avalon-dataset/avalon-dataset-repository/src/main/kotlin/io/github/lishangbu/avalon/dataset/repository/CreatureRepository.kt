package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

/**
 * 生物仓储接口
 *
 * 定义生物数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
interface CreatureRepository : KRepository<Creature, Long> {
    fun listAll(): List<Creature> =
        sql
            .createQuery(Creature::class) {
                orderBy(DEFAULT_SORT)
                select(table)
            }.execute()

    /** 按条件查询生物视图 */
    fun listViews(specification: CreatureSpecification?): List<CreatureView> =
        sql
            .createQuery(Creature::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(CreatureView::class))
            }.execute()

    /** 按条件分页查询生物视图 */
    fun pageViews(
        specification: CreatureSpecification?,
        pageable: Pageable,
    ): Page<CreatureView> =
        sql
            .createQuery(Creature::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(CreatureView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 查询单个生物视图 */
    fun loadViewById(id: Long): CreatureView? =
        sql
            .createQuery(Creature::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(CreatureView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
