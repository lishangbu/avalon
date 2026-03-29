package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionTriggerSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EvolutionTriggerView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEvolutionTriggerInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEvolutionTriggerInput

/** 进化触发方式服务 */
interface EvolutionTriggerService {
    /** 创建进化触发方式 */
    fun save(command: SaveEvolutionTriggerInput): EvolutionTriggerView

    /** 更新进化触发方式 */
    fun update(command: UpdateEvolutionTriggerInput): EvolutionTriggerView

    /** 删除指定 ID 的进化触发方式 */
    fun removeById(id: Long)

    /** 按筛选条件查询进化触发方式列表 */
    fun listByCondition(specification: EvolutionTriggerSpecification): List<EvolutionTriggerView>
}
