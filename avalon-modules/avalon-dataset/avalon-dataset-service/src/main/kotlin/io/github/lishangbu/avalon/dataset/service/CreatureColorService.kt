package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureColorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureColorView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureColorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureColorInput

/** 生物颜色服务 */
interface CreatureColorService {
    /** 创建生物颜色 */
    fun save(command: SaveCreatureColorInput): CreatureColorView

    /** 更新生物颜色 */
    fun update(command: UpdateCreatureColorInput): CreatureColorView

    /** 删除指定 ID 的生物颜色 */
    fun removeById(id: Long)

    /** 按筛选条件查询生物颜色列表 */
    fun listByCondition(specification: CreatureColorSpecification): List<CreatureColorView>
}
