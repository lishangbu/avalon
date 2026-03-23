package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Berry
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable

/**
 * 树果仓储接口
 *
 * 定义树果的查询、保存与删除操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface BerryRepository {
    /** 按示例条件查询树果列表 */
    fun findAll(example: Example<Berry>?): List<Berry>

    /** 按示例条件分页查询树果 */
    fun findAll(
        example: Example<Berry>?,
        pageable: Pageable,
    ): Page<Berry>

    /** 按 ID 查询单个树果 */
    fun findById(id: Long): Berry?

    /** 保存树果 */
    fun save(berry: Berry): Berry

    /** 保存树果并立即刷新 */
    fun saveAndFlush(berry: Berry): Berry

    /** 删除指定 ID 的树果 */
    fun deleteById(id: Long)

    /** 刷新当前持久化上下文 */
    fun flush()
}
