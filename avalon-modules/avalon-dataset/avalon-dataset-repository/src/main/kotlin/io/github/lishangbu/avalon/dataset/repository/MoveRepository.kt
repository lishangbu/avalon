package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.MoveSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveView
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * 招式仓储接口
 *
 * 定义招式数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
interface MoveRepository : KRepository<Move, Long> {
    fun listAll(): List<Move> =
        sql
            .createQuery(Move::class) {
                select(table)
            }.execute()

    /** 按条件查询招式视图 */
    fun listViews(specification: MoveSpecification?): List<MoveView> =
        sql
            .createQuery(Move::class) {
                specification?.let(::where)
                select(table.fetch(MoveView::class))
            }.execute()

    /** 按条件分页查询招式视图 */
    fun pageViews(
        specification: MoveSpecification?,
        pageable: Pageable,
    ): Page<MoveView> =
        sql
            .createQuery(Move::class) {
                specification?.let(::where)
                select(table.fetch(MoveView::class))
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 查询单个招式视图 */
    fun loadViewById(id: Long): MoveView? =
        sql
            .createQuery(Move::class) {
                where(table.id eq id)
                select(table.fetch(MoveView::class))
            }.execute()
            .firstOrNull()
}
