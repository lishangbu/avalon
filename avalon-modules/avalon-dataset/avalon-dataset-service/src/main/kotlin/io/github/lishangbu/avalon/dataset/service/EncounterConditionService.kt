package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterConditionInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterConditionInput

/** 遭遇条件服务 */
interface EncounterConditionService {
    /** 创建遭遇条件 */
    fun save(command: SaveEncounterConditionInput): EncounterConditionView

    /** 更新遭遇条件 */
    fun update(command: UpdateEncounterConditionInput): EncounterConditionView

    /** 删除指定 ID 的遭遇条件 */
    fun removeById(id: Long)

    /** 按筛选条件查询遭遇条件列表 */
    fun listByCondition(specification: EncounterConditionSpecification): List<EncounterConditionView>
}
