package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateSpecification
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 成长速率仓储接口
 *
 * 定义成长速率数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/2/10
 */
interface GrowthRateRepository :
    KRepository<GrowthRate, Long>,
    GrowthRateRepositoryExt

/** 成长速率仓储扩展接口 */
interface GrowthRateRepositoryExt {
    /** 按条件查询成长速率列表 */
    fun findAll(specification: GrowthRateSpecification?): List<GrowthRate>

    /** 按 ID 删除成长速率 */
    fun removeById(id: Long)
}
