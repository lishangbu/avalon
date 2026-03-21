package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable
import java.util.*

/**
 * 树果风味(BerryFlavor)数据访问层
 *
 * 由 Jimmer 实现的仓储契约，保持原有调用方式兼容
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface BerryFlavorRepository {
    fun findAll(): List<BerryFlavor>

    fun findAll(example: Example<BerryFlavor>?): List<BerryFlavor>

    fun findAll(
        example: Example<BerryFlavor>?,
        pageable: Pageable,
    ): Page<BerryFlavor>

    fun findById(id: Long): Optional<BerryFlavor>

    fun save(berryFlavor: BerryFlavor): BerryFlavor

    fun saveAndFlush(berryFlavor: BerryFlavor): BerryFlavor

    fun deleteById(id: Long)

    fun flush()
}
