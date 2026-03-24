package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import org.springframework.data.domain.Example

/**
 * 树果风味仓储接口
 *
 * 定义树果风味数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface BerryFlavorRepository {
    /** 查询全部树果风味列表 */
    fun findAll(): List<BerryFlavor>

    /** 按条件查询树果风味列表 */
    fun findAll(example: Example<BerryFlavor>?): List<BerryFlavor>

    /** 按 ID 查询树果风味 */
    fun findById(id: Long): BerryFlavor?

    /** 保存树果风味 */
    fun save(berryFlavor: BerryFlavor): BerryFlavor

    /** 保存树果风味并立即刷新 */
    fun saveAndFlush(berryFlavor: BerryFlavor): BerryFlavor

    /** 按 ID 删除树果风味 */
    fun deleteById(id: Long)

    /** 刷新持久化上下文 */
    fun flush()
}
