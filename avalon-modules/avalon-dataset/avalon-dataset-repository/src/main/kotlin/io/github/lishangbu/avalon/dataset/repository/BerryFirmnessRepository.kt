package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessSpecification
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.data.domain.Pageable

/**
 * 树果硬度仓储接口
 *
 * 定义树果硬度数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface BerryFirmnessRepository :
    KRepository<BerryFirmness, Long>,
    BerryFirmnessRepositoryExt

/** 树果硬度仓储扩展接口 */
interface BerryFirmnessRepositoryExt {
    /** 按条件查询树果硬度列表 */
    fun findAll(specification: BerryFirmnessSpecification?): List<BerryFirmness>

    /** 按条件分页查询树果硬度 */
    fun findAll(
        specification: BerryFirmnessSpecification?,
        pageable: Pageable,
    ): Page<BerryFirmness>

    /** 按 ID 删除树果硬度 */
    fun removeById(id: Long)
}
