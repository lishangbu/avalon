package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.Role
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable

/**
 * 角色仓储接口
 *
 * 定义角色数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/08/20
 */
interface RoleRepository {
    /** 按条件查询角色列表 */
    fun findAll(example: Example<Role>?): List<Role>

    /** 按条件分页查询角色 */
    fun findAll(
        example: Example<Role>?,
        pageable: Pageable,
    ): Page<Role>

    /** 按 ID 查询角色 */
    fun findById(id: Long): Role?

    /** 按 ID 列表查询角色 */
    fun findAllById(ids: Iterable<Long>): List<Role>

    /** 保存角色 */
    fun save(role: Role): Role

    /** 保存角色并立即刷新 */
    fun saveAndFlush(role: Role): Role

    /** 按 ID 删除角色 */
    fun deleteById(id: Long)

    /** 刷新持久化上下文 */
    fun flush()
}
