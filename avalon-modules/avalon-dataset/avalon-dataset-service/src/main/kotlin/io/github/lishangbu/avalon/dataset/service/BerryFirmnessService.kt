package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 树果坚硬度服务。 */
interface BerryFirmnessService {
    /** 根据条件分页查询树果坚硬度。 */
    fun getPageByCondition(
        berryFirmness: BerryFirmness,
        pageable: Pageable,
    ): Page<BerryFirmness>

    /** 新增树果坚硬度。 */
    fun save(berryFirmness: BerryFirmness): BerryFirmness

    /** 更新树果坚硬度。 */
    fun update(berryFirmness: BerryFirmness): BerryFirmness

    /** 根据主键删除树果坚硬度。 */
    fun removeById(id: Long)

    /** 根据条件查询树果坚硬度列表。 */
    fun listByCondition(berryFirmness: BerryFirmness): List<BerryFirmness>
}
