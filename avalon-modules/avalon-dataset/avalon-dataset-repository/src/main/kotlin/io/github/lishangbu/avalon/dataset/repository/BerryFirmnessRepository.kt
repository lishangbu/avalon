package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable

/**
 * 树果硬度仓储接口
 *
 * 定义树果硬度数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface BerryFirmnessRepository {
    /** 查询全部树果硬度列表 */
    fun findAll(): List<BerryFirmness>

    /** 按条件查询树果硬度列表 */
    fun findAll(example: Example<BerryFirmness>?): List<BerryFirmness>

    /** 按条件分页查询树果硬度 */
    fun findAll(
        example: Example<BerryFirmness>?,
        pageable: Pageable,
    ): Page<BerryFirmness>

    /** 按 ID 查询树果硬度 */
    fun findById(id: Long): BerryFirmness?

    /** 保存树果硬度 */
    fun save(berryFirmness: BerryFirmness): BerryFirmness

    /** 保存树果硬度并立即刷新 */
    fun saveAndFlush(berryFirmness: BerryFirmness): BerryFirmness

    /** 按 ID 删除树果硬度 */
    fun deleteById(id: Long)

    /** 刷新持久化上下文 */
    fun flush()
}
