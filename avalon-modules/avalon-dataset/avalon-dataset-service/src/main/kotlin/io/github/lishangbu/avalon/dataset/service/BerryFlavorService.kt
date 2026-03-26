package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryFlavorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryFlavorInput

/** 树果风味服务 */
interface BerryFlavorService {
    /** 保存树果风味 */
    fun save(command: SaveBerryFlavorInput): BerryFlavorView

    /** 更新树果风味 */
    fun update(command: UpdateBerryFlavorInput): BerryFlavorView

    /** 按 ID 删除树果风味 */
    fun removeById(id: Long)

    /** 按筛选条件查询树果风味列表 */
    fun listByCondition(specification: BerryFlavorSpecification): List<BerryFlavorView>
}
