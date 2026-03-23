package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.User
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable

/**
 * 用户仓储接口
 *
 * 定义用户数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/08/19
 */
interface UserRepository {
    /** 按条件查询用户列表 */
    fun findAll(example: Example<User>?): List<User>

    /** 按条件分页查询用户 */
    fun findAll(
        example: Example<User>?,
        pageable: Pageable,
    ): Page<User>

    /** 按 ID 查询用户 */
    fun findById(id: Long): User?

    /** 保存用户 */
    fun save(user: User): User

    /** 保存用户并立即刷新 */
    fun saveAndFlush(user: User): User

    /** 按 ID 删除用户 */
    fun deleteById(id: Long)

    /** 刷新持久化上下文 */
    fun flush()

    /** 根据账号查找用户及角色列表 */
    fun findUserWithRolesByAccount(account: String): User?
}
