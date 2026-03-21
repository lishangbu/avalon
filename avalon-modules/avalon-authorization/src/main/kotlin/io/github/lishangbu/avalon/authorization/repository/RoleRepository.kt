package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.Role
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable
import java.util.Optional

/**
 * 角色信息(role)表数据库访问层
 *
 * 提供基础的 CRUD 操作
 *
 * @author lishangbu
 * @since 2025/08/20
 */
interface RoleRepository {
    fun findAll(example: Example<Role>?): List<Role>

    fun findAll(
        example: Example<Role>?,
        pageable: Pageable,
    ): Page<Role>

    fun findById(id: Long): Optional<Role>

    fun findAllById(ids: Iterable<Long>): List<Role>

    fun save(role: Role): Role

    fun saveAndFlush(role: Role): Role

    fun deleteById(id: Long)

    fun flush()
}
