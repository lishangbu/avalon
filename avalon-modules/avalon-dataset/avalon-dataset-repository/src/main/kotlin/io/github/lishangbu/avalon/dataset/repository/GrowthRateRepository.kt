package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import org.babyfish.jimmer.Specification
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 成长速率仓储接口
 *
 * 定义成长速率数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/2/10
 */
interface GrowthRateRepository : KRepository<GrowthRate, Long> {
    /** 按条件查询成长速率列表 */
    fun findAll(specification: Specification<GrowthRate>?): List<GrowthRate> =
        sql
            .createQuery(GrowthRate::class) {
                specification?.let(::where)
                select(table)
            }.execute()
}
