package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import org.springframework.data.domain.Example

/**
 * 成长速率仓储接口
 *
 * 定义成长速率数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/2/10
 */
interface GrowthRateRepository {
    /** 查询全部成长速率列表 */
    fun findAll(): List<GrowthRate>

    /** 按条件查询成长速率列表 */
    fun findAll(example: Example<GrowthRate>?): List<GrowthRate>

    /** 按 ID 查询成长速率 */
    fun findById(id: Long): GrowthRate?

    /** 保存成长速率 */
    fun save(growthRate: GrowthRate): GrowthRate

    /** 保存成长速率并立即刷新 */
    fun saveAndFlush(growthRate: GrowthRate): GrowthRate

    /** 按 ID 删除成长速率 */
    fun deleteById(id: Long)

    /** 刷新持久化上下文 */
    fun flush()
}
