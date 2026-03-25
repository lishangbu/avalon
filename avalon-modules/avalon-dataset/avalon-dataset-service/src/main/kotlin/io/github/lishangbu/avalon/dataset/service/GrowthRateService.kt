package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateSpecification

/** 成长速率服务 */
interface GrowthRateService {
    /** 创建成长速率 */
    fun save(growthRate: GrowthRate): GrowthRate

    /** 更新成长速率 */
    fun update(growthRate: GrowthRate): GrowthRate

    /** 删除指定 ID 的成长速率 */
    fun removeById(id: Long)

    /** 按筛选条件查询成长速率列表 */
    fun listByCondition(specification: GrowthRateSpecification): List<GrowthRate>
}
