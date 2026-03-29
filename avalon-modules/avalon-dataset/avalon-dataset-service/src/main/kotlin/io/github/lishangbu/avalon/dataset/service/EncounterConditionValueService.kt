package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionValueSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionValueView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEncounterConditionValueInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEncounterConditionValueInput

/** 遭遇条件值服务 */
interface EncounterConditionValueService {
    /** 创建遭遇条件值 */
    fun save(command: SaveEncounterConditionValueInput): EncounterConditionValueView

    /** 更新遭遇条件值 */
    fun update(command: UpdateEncounterConditionValueInput): EncounterConditionValueView

    /** 删除指定 ID 的遭遇条件值 */
    fun removeById(id: Long)

    /** 按筛选条件查询遭遇条件值列表 */
    fun listByCondition(specification: EncounterConditionValueSpecification): List<EncounterConditionValueView>
}
