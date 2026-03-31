package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureShapeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureShapeView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureShapeInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureShapeInput

/** 生物形状服务 */
interface CreatureShapeService {
    /** 创建生物形状 */
    fun save(command: SaveCreatureShapeInput): CreatureShapeView

    /** 更新生物形状 */
    fun update(command: UpdateCreatureShapeInput): CreatureShapeView

    /** 删除指定 ID 的生物形状 */
    fun removeById(id: Long)

    /** 按筛选条件查询生物形状列表 */
    fun listByCondition(specification: CreatureShapeSpecification): List<CreatureShapeView>
}
