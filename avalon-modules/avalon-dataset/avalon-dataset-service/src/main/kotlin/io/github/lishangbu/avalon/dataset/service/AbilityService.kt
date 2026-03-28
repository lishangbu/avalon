package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.AbilitySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.AbilityView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveAbilityInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateAbilityInput

/** 特性服务 */
interface AbilityService {
    /** 创建特性 */
    fun save(command: SaveAbilityInput): AbilityView

    /** 更新特性 */
    fun update(command: UpdateAbilityInput): AbilityView

    /** 删除指定 ID 的特性 */
    fun removeById(id: Long)

    /** 按筛选条件查询特性列表 */
    fun listByCondition(specification: AbilitySpecification): List<AbilityView>
}
