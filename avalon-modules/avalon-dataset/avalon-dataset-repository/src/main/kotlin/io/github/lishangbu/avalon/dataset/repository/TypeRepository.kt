package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.TypeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.TypeView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.sql.kt.ast.expression.eq

/**
 * 属性仓储接口
 *
 * 定义属性数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface TypeRepository : KRepository<Type, Long> {
    /** 按条件查询属性视图列表 */
    fun listViews(specification: TypeSpecification?): List<TypeView> =
        sql
            .createQuery(Type::class) {
                specification?.let(::where)
                select(table.fetch(TypeView::class))
            }.execute()

    /** 按 ID 查询属性视图 */
    fun loadViewById(id: Long): TypeView? =
        sql
            .createQuery(Type::class) {
                where(table.id eq id)
                select(table.fetch(TypeView::class))
            }.execute()
            .firstOrNull()
}
