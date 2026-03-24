package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Gender
import org.springframework.data.domain.Example

/**
 * 性别仓储接口
 *
 * 定义性别数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/3/24
 */
interface GenderRepository {
    /** 查询全部性别列表 */
    fun findAll(): List<Gender>

    /** 按条件查询性别列表 */
    fun findAll(example: Example<Gender>?): List<Gender>

    /** 按 ID 查询性别 */
    fun findById(id: Long): Gender?

    /** 保存性别 */
    fun save(gender: Gender): Gender

    /** 保存性别并立即刷新 */
    fun saveAndFlush(gender: Gender): Gender

    /** 按 ID 删除性别 */
    fun deleteById(id: Long)

    /** 刷新持久化上下文 */
    fun flush()
}
