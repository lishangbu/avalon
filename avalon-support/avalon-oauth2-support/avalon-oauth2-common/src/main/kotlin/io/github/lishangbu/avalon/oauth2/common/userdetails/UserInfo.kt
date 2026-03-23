package io.github.lishangbu.avalon.oauth2.common.userdetails

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal

/**
 * OAuth2 用户信息对象
 *
 * 基于 Spring Security 的 [User] 扩展附加属性，便于在令牌相关接口中返回更多用户信息
 *
 * @author lishangbu
 * @since 2025/8/9
 */
@Suppress("removal")
class UserInfo :
    User,
    OAuth2AuthenticatedPrincipal {
    /** 附加属性 */
    val additionalParameters: MutableMap<String, Any> = HashMap()

    constructor(
        username: String,
        password: String?,
        authorities: Collection<out GrantedAuthority>,
    ) : super(username, password, authorities)

    constructor(
        username: String,
        password: String?,
        enabled: Boolean,
        accountNonExpired: Boolean,
        credentialsNonExpired: Boolean,
        accountNonLocked: Boolean,
        authorities: Collection<out GrantedAuthority>,
    ) : super(
        username,
        password,
        enabled,
        accountNonExpired,
        credentialsNonExpired,
        accountNonLocked,
        authorities,
    )

    /** 返回附加属性映射 */
    override fun getAttributes(): MutableMap<String, Any> = additionalParameters

    /** 返回主体名称 */
    override fun getName(): String = username

    /** 返回对象的字符串表示 */
    override fun toString(): String =
        javaClass.name +
            " [" +
            "Username=" +
            username +
            ", " +
            "Enabled=" +
            isEnabled +
            ", " +
            "AdditionalParameters" +
            additionalParameters +
            "AccountNonExpired=" +
            isAccountNonExpired +
            ", " +
            "CredentialsNonExpired=" +
            isCredentialsNonExpired +
            ", " +
            "AccountNonLocked=" +
            isAccountNonLocked +
            ", " +
            "Granted Authorities=" +
            authorities +
            "]"

    companion object {
        /** 序列化版本号 */
        private const val serialVersionUID = 1L
    }
}
