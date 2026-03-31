package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.CreatureHabitatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.CreatureHabitatView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveCreatureHabitatInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateCreatureHabitatInput

/** 生物栖息地服务 */
interface CreatureHabitatService {
    /** 创建生物栖息地 */
    fun save(command: SaveCreatureHabitatInput): CreatureHabitatView

    /** 更新生物栖息地 */
    fun update(command: UpdateCreatureHabitatInput): CreatureHabitatView

    /** 删除指定 ID 的生物栖息地 */
    fun removeById(id: Long)

    /** 按筛选条件查询生物栖息地列表 */
    fun listByCondition(specification: CreatureHabitatSpecification): List<CreatureHabitatView>
}
