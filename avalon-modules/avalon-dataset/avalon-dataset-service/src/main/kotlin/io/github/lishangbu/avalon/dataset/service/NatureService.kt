package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.NatureSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.NatureView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveNatureInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateNatureInput

/** 性格服务 */
interface NatureService {
    /** 创建性格 */
    fun save(command: SaveNatureInput): NatureView

    /** 更新性格 */
    fun update(command: UpdateNatureInput): NatureView

    /** 删除指定 ID 的性格 */
    fun removeById(id: Long)

    /** 按筛选条件查询性格列表 */
    fun listByCondition(specification: NatureSpecification): List<NatureView>
}
