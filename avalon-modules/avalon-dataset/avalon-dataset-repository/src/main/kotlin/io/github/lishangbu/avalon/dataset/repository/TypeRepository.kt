package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Type
import org.springframework.data.domain.Example

/**
 * 属性仓储接口
 *
 * 定义属性数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface TypeRepository {
    /** 查询全部属性列表 */
    fun findAll(): List<Type>

    /** 按条件查询属性列表 */
    fun findAll(example: Example<Type>?): List<Type>

    /** 按 ID 查询属性 */
    fun findById(id: Long): Type?

    /** 保存属性 */
    fun save(type: Type): Type

    /** 保存属性并立即刷新 */
    fun saveAndFlush(type: Type): Type

    /** 按 ID 删除属性 */
    fun deleteById(id: Long)

    /** 刷新持久化上下文 */
    fun flush()
}
