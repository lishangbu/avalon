package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.User
import io.github.lishangbu.avalon.authorization.entity.dto.UserSpecification
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.data.domain.Pageable

/**
 * 用户仓储接口
 *
 * 定义用户数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/08/19
 */
interface UserRepository :
    KRepository<User, Long>,
    UserRepositoryExt

interface UserRepositoryExt {
    /** 按条件查询用户列表 */
    fun findAll(specification: UserSpecification?): List<User>

    /** 按条件分页查询用户 */
    fun findAll(
        specification: UserSpecification?,
        pageable: Pageable,
    ): Page<User>

    /** 按条件查询用户列表，并抓取角色 */
    fun findAllWithRoles(specification: UserSpecification?): List<User>

    /** 按条件分页查询用户，并抓取角色 */
    fun findAllWithRoles(
        specification: UserSpecification?,
        pageable: Pageable,
    ): Page<User>

    /** 按 ID 删除用户 */
    fun removeById(id: Long)

    /** 根据账号查找用户及角色列表 */
    fun findByAccountWithRoles(account: String): User?
}
