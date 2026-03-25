package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessSpecification
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 树果硬度服务 */
interface BerryFirmnessService {
    /** 按条件分页查询树果硬度*/
    fun getPageByCondition(
        specification: BerryFirmnessSpecification,
        pageable: Pageable,
    ): Page<BerryFirmness>

    /** 保存树果硬度 */
    fun save(berryFirmness: BerryFirmness): BerryFirmness

    /** 更新树果硬度 */
    fun update(berryFirmness: BerryFirmness): BerryFirmness

    /** 按 ID 删除树果硬度 */
    fun removeById(id: Long)

    /** 根据条件查询树果硬度列表 */
    fun listByCondition(specification: BerryFirmnessSpecification): List<BerryFirmness>
}
