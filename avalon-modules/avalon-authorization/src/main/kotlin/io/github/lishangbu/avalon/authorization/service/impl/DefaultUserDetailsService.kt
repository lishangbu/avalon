package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.repository.UserRepository
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * 用户服务
 *
 * @author lishangbu
 * @since 2025/8/17
 */
@Service
class DefaultUserDetailsService(
    /** 用户仓储 */
    private val userRepository: UserRepository,
) : UserDetailsService {
    /**
     * 根据用户名/手机号/邮箱加载用户详情
     *
     * @param username 登录账号
     * @return 用户详情
     * @throws UsernameNotFoundException 用户未找到时抛出
     */
    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): UserDetails =
        userRepository
            .findUserWithRolesByAccount(username)
            ?.let { user ->
                UserInfo(
                    user.username ?: "",
                    user.hashedPassword ?: "",
                    if (user.roles.isEmpty()) {
                        AuthorityUtils.NO_AUTHORITIES
                    } else {
                        AuthorityUtils.createAuthorityList(
                            *user.roles.mapNotNull { it.code }.toTypedArray(),
                        )
                    },
                )
            } ?: throw UsernameNotFoundException("账号或密码错误")
}
