package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Stat
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable

/**
 * 能力(Stat)数据访问层
 *
 * 由 Jimmer 实现的仓储契约，保持原有调用方式兼容
 *
 * @author lishangbu
 * @since 2026/2/11
 */
interface StatRepository {
    fun findAll(example: Example<Stat>?): List<Stat>

    fun findAll(
        example: Example<Stat>?,
        pageable: Pageable,
    ): Page<Stat>

    fun save(stat: Stat): Stat

    fun deleteById(id: Long)
}
