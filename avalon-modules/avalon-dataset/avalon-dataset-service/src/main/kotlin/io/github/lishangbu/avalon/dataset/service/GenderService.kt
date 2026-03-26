package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.GenderSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.GenderView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveGenderInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateGenderInput

/** 性别服务 */
interface GenderService {
    /** 创建性别 */
    fun save(command: SaveGenderInput): GenderView

    /** 更新性别 */
    fun update(command: UpdateGenderInput): GenderView

    /** 删除指定 ID 的性别 */
    fun removeById(id: Long)

    /** 按筛选条件查询性别列表 */
    fun listByCondition(specification: GenderSpecification): List<GenderView>
}
