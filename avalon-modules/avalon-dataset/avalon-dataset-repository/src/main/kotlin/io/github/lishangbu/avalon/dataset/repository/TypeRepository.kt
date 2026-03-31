package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.TypeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.TypeView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort

/**
 * 属性仓储接口
 *
 * 定义属性数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface TypeRepository : KRepository<Type, Long> {
    fun listAll(): List<Type> =
        sql
            .createQuery(Type::class) {
                orderBy(DEFAULT_SORT)
                select(table)
            }.execute()

    /** 按条件查询属性视图列表 */
    fun listViews(specification: TypeSpecification?): List<TypeView> =
        sql
            .createQuery(Type::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(TypeView::class))
            }.execute()

    /** 按 ID 查询属性视图 */
    fun loadViewById(id: Long): TypeView? =
        sql
            .createQuery(Type::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(TypeView::class))
            }.execute()
            .firstOrNull()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
