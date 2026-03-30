package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.User
import io.github.lishangbu.avalon.authorization.entity.addBy
import io.github.lishangbu.avalon.authorization.entity.dto.CurrentUserView
import io.github.lishangbu.avalon.authorization.entity.dto.SaveUserInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateUserInput
import io.github.lishangbu.avalon.authorization.entity.dto.UserSpecification
import io.github.lishangbu.avalon.authorization.entity.dto.UserView
import io.github.lishangbu.avalon.authorization.repository.AuthorizationFetchers
import io.github.lishangbu.avalon.authorization.repository.RoleRepository
import io.github.lishangbu.avalon.authorization.repository.UserRepository
import io.github.lishangbu.avalon.authorization.service.UserService
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 用户服务实现
 *
 * @author lishangbu
 * @since 2025/8/30
 */
@Service
class UserServiceImpl(
    /** 用户仓储 */
    private val userRepository: UserRepository,
    /** 角色仓储 */
    private val roleRepository: RoleRepository,
) : UserService {
    /**
     * 根据用户名/手机号/邮箱查询用户详情，包含基本信息、角色信息及个人资料
     *
     * @param username 登录账号
     * @return 查询到的用户详情，未找到时返回 null
     */
    override fun getUserByUsername(username: String): CurrentUserView? = userRepository.loadByAccountWithRoles(username)?.let(::CurrentUserView)

    /** 按条件分页查询用户 */
    override fun getPageByCondition(
        specification: UserSpecification,
        pageable: Pageable,
    ): Page<UserView> = userRepository.pageViews(specification, pageable)

    /** 按条件查询用户列表 */
    override fun listByCondition(specification: UserSpecification): List<UserView> = userRepository.listViews(specification)

    /** 按 ID 查询用户 */
    override fun getById(id: Long): UserView? = userRepository.loadViewById(id)

    /** 保存用户 */
    @Transactional(rollbackFor = [Exception::class])
    override fun save(command: SaveUserInput): UserView {
        val prepared = bindRoles(command.toEntity(), false)
        return userRepository.save(prepared, SaveMode.INSERT_ONLY).let(::reloadView)
    }

    /** 更新用户 */
    @Transactional(rollbackFor = [Exception::class])
    override fun update(command: UpdateUserInput): UserView {
        val prepared = bindRoles(command.toEntity(), true)
        return userRepository.save(prepared).let(::reloadView)
    }

    /** 按 ID 删除用户 */
    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        userRepository.deleteById(id)
    }

    /** 绑定并补全角色信息 */
    private fun bindRoles(
        user: User,
        preserveWhenNull: Boolean,
    ): User {
        val existing =
            if (preserveWhenNull) {
                user.readOrNull { id }?.let { userId -> userRepository.findNullable(userId, AuthorizationFetchers.USER_WITH_ROLES) }
            } else {
                null
            }

        val currentRoles = user.readOrNull { roles }
        val roleIds = currentRoles?.mapNotNull { it.readOrNull { id } }?.toCollection(LinkedHashSet())
        val shouldLoadRoles = currentRoles != null
        val boundRoles =
            when {
                currentRoles != null && !roleIds.isNullOrEmpty() -> roleRepository.findAllById(roleIds)
                currentRoles != null -> emptyList()
                preserveWhenNull -> existing?.readOrNull { roles } ?: emptyList()
                else -> emptyList()
            }

        val hashedPassword =
            user.readOrNull { hashedPassword } ?: existing?.readOrNull { hashedPassword }

        return User {
            user.readOrNull { id }?.let { id = it }
            username = user.readOrNull { username } ?: existing?.readOrNull { username }
            phone = user.readOrNull { phone } ?: existing?.readOrNull { phone }
            email = user.readOrNull { email } ?: existing?.readOrNull { email }
            avatar = user.readOrNull { avatar } ?: existing?.readOrNull { avatar }
            this.hashedPassword = hashedPassword
            if (shouldLoadRoles) {
                roles()
            }
            boundRoles.forEach { boundRole -> roles().addBy(boundRole) }
        }
    }

    private fun reloadView(user: User): UserView = requireNotNull(userRepository.loadViewById(user.id)) { "未找到 ID=${user.id} 对应的用户" }
}
