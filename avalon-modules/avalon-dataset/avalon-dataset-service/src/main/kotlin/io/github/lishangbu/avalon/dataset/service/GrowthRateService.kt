package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveGrowthRateInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateGrowthRateInput

/** 成长速率服务 */
interface GrowthRateService {
    /** 创建成长速率 */
    fun save(command: SaveGrowthRateInput): GrowthRateView

    /** 更新成长速率 */
    fun update(command: UpdateGrowthRateInput): GrowthRateView

    /** 删除指定 ID 的成长速率 */
    fun removeById(id: Long)

    /** 按筛选条件查询成长速率列表 */
    fun listByCondition(specification: GrowthRateSpecification): List<GrowthRateView>
}
