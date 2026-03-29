package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Type
import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 属性仓储接口
 *
 * 定义属性数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface TypeRepository : KRepository<Type, Long> {
    /** 按条件查询属性列表 */
    fun findAll(specification: Specification<Type>?): List<Type> =
        sql
            .createQuery(Type::class) {
                specification?.let(::where)
                select(table)
            }.execute()
}
