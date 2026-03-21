package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.User
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable
import java.util.*

/**
 * 用户信息(user)表数据库访问层
 *
 * 提供对用户信息的增删改查等操作
 *
 * @author lishangbu
 * @since 2025/08/19
 */
interface UserRepository {
    fun findAll(example: Example<User>?): List<User>

    fun findAll(
        example: Example<User>?,
        pageable: Pageable,
    ): Page<User>

    fun findById(id: Long): Optional<User>

    fun save(user: User): User

    fun saveAndFlush(user: User): User

    fun deleteById(id: Long)

    fun flush()

    fun findUserWithRolesByAccount(account: String): Optional<User>
}
