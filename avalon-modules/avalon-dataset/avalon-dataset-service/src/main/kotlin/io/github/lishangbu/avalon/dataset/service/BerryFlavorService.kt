package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 树果风味服务。 */
interface BerryFlavorService {
    /** 根据条件分页查询树果风味。 */
    fun getPageByCondition(
        berryFlavor: BerryFlavor,
        pageable: Pageable,
    ): Page<BerryFlavor>

    /** 新增树果风味。 */
    fun save(berryFlavor: BerryFlavor): BerryFlavor

    /** 更新树果风味。 */
    fun update(berryFlavor: BerryFlavor): BerryFlavor

    /** 根据主键删除树果风味。 */
    fun removeById(id: Long)
}
