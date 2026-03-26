package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.SaveTypeInput
import io.github.lishangbu.avalon.dataset.entity.dto.TypeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.TypeView
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateTypeInput

/** 属性应用服务 */
interface TypeService {
    /** 创建属性 */
    fun save(command: SaveTypeInput): TypeView

    /** 更新属性 */
    fun update(command: UpdateTypeInput): TypeView

    /** 删除指定 ID 的属性 */
    fun removeById(id: Long)

    /** 按筛选条件查询属性列表 */
    fun listByCondition(specification: TypeSpecification): List<TypeView>
}
