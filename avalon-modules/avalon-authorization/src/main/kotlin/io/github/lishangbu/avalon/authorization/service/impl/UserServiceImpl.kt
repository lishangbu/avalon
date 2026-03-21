package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.entity.*
import io.github.lishangbu.avalon.authorization.model.UserWithRoles
import io.github.lishangbu.avalon.authorization.repository.RoleRepository
import io.github.lishangbu.avalon.authorization.repository.UserRepository
import io.github.lishangbu.avalon.authorization.service.UserService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * 用户服务实现
 *
 * @author lishangbu
 * @since 2025/8/30
 */
@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
) : UserService {
    /**
     * 根据用户名/手机号/邮箱查询用户详情，包含基本信息、角色信息及个人资料
     *
     * @param username 登录账号
     * @return 查询到的用户详情，未找到时返回Optional.empty()
     */
    override fun getUserByUsername(username: String): Optional<UserWithRoles> = userRepository.findUserWithRolesByAccount(username).map(::UserWithRoles)

    override fun getPageByCondition(
        user: User,
        pageable: Pageable,
    ): Page<User> =
        userRepository.findAll(
            Example.of(
                user,
                ExampleMatcher
                    .matching()
                    .withIgnoreNullValues()
                    .withMatcher("username", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("phone", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("email", ExampleMatcher.GenericPropertyMatchers.contains()),
            ),
            pageable,
        )

    override fun listByCondition(user: User): List<User> =
        userRepository.findAll(
            Example.of(
                user,
                ExampleMatcher
                    .matching()
                    .withIgnoreNullValues()
                    .withMatcher("username", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("phone", ExampleMatcher.GenericPropertyMatchers.contains())
                    .withMatcher("email", ExampleMatcher.GenericPropertyMatchers.contains()),
            ),
        )

    override fun getById(id: Long): Optional<User> = userRepository.findById(id)

    @Transactional(rollbackFor = [Exception::class])
    override fun save(user: User): User {
        val prepared = bindRoles(user, false)
        return userRepository.save(prepared)
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun update(user: User): User {
        val prepared = bindRoles(user, true)
        return userRepository.save(prepared)
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun removeById(id: Long) {
        userRepository.deleteById(id)
    }

    private fun bindRoles(
        user: User,
        preserveWhenNull: Boolean,
    ): User {
        val existing =
            if (preserveWhenNull) {
                user.readOrNull { id }?.let { userRepository.findById(it).orElse(null) }
            } else {
                null
            }

        val currentRoles = user.readOrNull { roles } ?: emptyList()
        val roleIds = currentRoles.mapNotNull { it.readOrNull { id } }.toCollection(LinkedHashSet())
        val boundRoles =
            when {
                roleIds.isNotEmpty() -> roleRepository.findAllById(roleIds)
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
            boundRoles.forEach { boundRole -> roles().addBy(boundRole) }
        }
    }

    private inline fun <T, R> T?.readOrNull(block: T.() -> R): R? = this?.let { runCatching { it.block() }.getOrNull() }
}
