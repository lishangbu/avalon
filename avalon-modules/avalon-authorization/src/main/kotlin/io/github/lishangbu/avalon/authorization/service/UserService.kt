package io.github.lishangbu.avalon.authorization.service

import io.github.lishangbu.avalon.authorization.entity.User
import io.github.lishangbu.avalon.authorization.model.UserWithRoles
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/**
 * 用户服务
 *
 * 提供用户相关的查询与管理操作
 *
 * @author lishangbu
 * @since 2025/8/30
 */
interface UserService {
    /**
     * 根据用户名/手机号/邮箱查询用户详情，包含基本信息、角色信息及个人资料
     *
     * @param username 登录账号
     * @return 查询到的用户详情，未找到时返回 null
     */
    fun getUserByUsername(username: String): UserWithRoles?

    /** 按条件分页查询用户 */
    fun getPageByCondition(
        user: User,
        pageable: Pageable,
    ): Page<User>

    /** 根据条件查询用户列表 */
    fun listByCondition(user: User): List<User>

    /** 按 ID 查询用户 */
    fun getById(id: Long): User?

    /** 保存用户 */
    fun save(user: User): User

    /** 更新用户 */
    fun update(user: User): User

    /** 按 ID 删除用户 */
    fun removeById(id: Long)
}
