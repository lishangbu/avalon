package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Ability
import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.spring.repository.KRepository

/** 特性仓储接口 */
interface AbilityRepository : KRepository<Ability, Long> {
    /** 按条件查询特性列表 */
    fun findAll(specification: Specification<Ability>?): List<Ability> =
        sql
            .createQuery(Ability::class) {
                specification?.let(::where)
                select(table)
            }.execute()
}
