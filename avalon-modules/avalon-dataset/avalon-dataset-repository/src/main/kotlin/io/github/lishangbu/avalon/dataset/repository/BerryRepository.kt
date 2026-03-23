package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Berry
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable

/**
 * 树果(Berry)数据访问层
 *
 * 由 Jimmer 实现的仓储契约，保持原有调用方式兼容
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface BerryRepository {
    fun findAll(example: Example<Berry>?): List<Berry>

    fun findAll(
        example: Example<Berry>?,
        pageable: Pageable,
    ): Page<Berry>

    fun findById(id: Long): Berry?

    fun save(berry: Berry): Berry

    fun saveAndFlush(berry: Berry): Berry

    fun deleteById(id: Long)

    fun flush()
}
