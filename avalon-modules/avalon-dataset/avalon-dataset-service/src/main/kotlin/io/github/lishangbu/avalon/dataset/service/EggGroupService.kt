package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveEggGroupInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateEggGroupInput

/** 蛋组服务 */
interface EggGroupService {
    /** 创建蛋组 */
    fun save(command: SaveEggGroupInput): EggGroupView

    /** 更新蛋组 */
    fun update(command: UpdateEggGroupInput): EggGroupView

    /** 删除指定 ID 的蛋组 */
    fun removeById(id: Long)

    /** 按筛选条件查询蛋组列表 */
    fun listByCondition(specification: EggGroupSpecification): List<EggGroupView>
}
