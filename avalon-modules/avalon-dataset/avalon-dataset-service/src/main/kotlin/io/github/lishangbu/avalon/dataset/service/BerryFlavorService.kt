package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor

/** 树果风味服务 */
interface BerryFlavorService {
    /** 保存树果风味 */
    fun save(berryFlavor: BerryFlavor): BerryFlavor

    /** 更新树果风味 */
    fun update(berryFlavor: BerryFlavor): BerryFlavor

    /** 按 ID 删除树果风味 */
    fun removeById(id: Long)

    /** 按筛选条件查询树果风味列表 */
    fun listByCondition(berryFlavor: BerryFlavor): List<BerryFlavor>
}
