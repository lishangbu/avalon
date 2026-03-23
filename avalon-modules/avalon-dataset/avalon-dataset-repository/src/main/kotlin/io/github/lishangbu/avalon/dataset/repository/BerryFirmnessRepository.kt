package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable

/**
 * 树果硬度(BerryFirmness)数据访问层
 *
 * 由 Jimmer 实现的仓储契约，保持原有调用方式兼容
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface BerryFirmnessRepository {
    fun findAll(): List<BerryFirmness>

    fun findAll(example: Example<BerryFirmness>?): List<BerryFirmness>

    fun findAll(
        example: Example<BerryFirmness>?,
        pageable: Pageable,
    ): Page<BerryFirmness>

    fun findById(id: Long): BerryFirmness?

    fun save(berryFirmness: BerryFirmness): BerryFirmness

    fun saveAndFlush(berryFirmness: BerryFirmness): BerryFirmness

    fun deleteById(id: Long)

    fun flush()
}
