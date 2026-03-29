package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.EggGroup
import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.spring.repository.KRepository

/** 蛋组仓储接口 */
interface EggGroupRepository : KRepository<EggGroup, Long> {
    /** 按条件查询蛋组列表 */
    fun findAll(specification: Specification<EggGroup>?): List<EggGroup> =
        sql
            .createQuery(EggGroup::class) {
                specification?.let(::where)
                select(table)
            }.execute()
}
