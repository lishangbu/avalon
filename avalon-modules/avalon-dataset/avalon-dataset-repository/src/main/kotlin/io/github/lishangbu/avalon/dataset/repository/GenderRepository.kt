package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Gender
import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 性别仓储接口
 *
 * 定义性别数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/3/24
 */
interface GenderRepository : KRepository<Gender, Long> {
    /** 按条件查询性别列表 */
    fun findAll(specification: Specification<Gender>?): List<Gender> =
        sql
            .createQuery(Gender::class) {
                specification?.let(::where)
                select(table)
            }.execute()
}
