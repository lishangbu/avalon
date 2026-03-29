package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.GenderSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.GenderView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq

/**
 * 性别仓储接口
 *
 * 定义性别数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/3/24
 */
interface GenderRepository : KRepository<Gender, Long> {
    /** 按条件查询性别视图列表 */
    fun listViews(specification: GenderSpecification?): List<GenderView> =
        sql
            .createQuery(Gender::class) {
                specification?.let(::where)
                select(table.fetch(GenderView::class))
            }.execute()

    /** 按 ID 查询性别视图 */
    fun loadViewById(id: Long): GenderView? =
        sql
            .createQuery(Gender::class) {
                where(table.id eq id)
                select(table.fetch(GenderView::class))
            }.execute()
            .firstOrNull()
}
