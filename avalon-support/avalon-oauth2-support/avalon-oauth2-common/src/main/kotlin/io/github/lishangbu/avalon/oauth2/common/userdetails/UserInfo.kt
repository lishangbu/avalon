package io.github.lishangbu.avalon.oauth2.common.userdetails

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.SpringSecurityCoreVersion
import org.springframework.security.core.userdetails.User
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal

/**
 * 用户信息 封装 Spring Security User 并实现 OAuth2AuthenticatedPrincipal，支持附加参数
 *
 * @author lishangbu
 * @since 2025/8/9 附加参数：用于在获取 Token 接口返回
 */
@Suppress("removal")
class UserInfo :
    User,
    OAuth2AuthenticatedPrincipal {
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

    override fun getAttributes(): MutableMap<String, Any> = additionalParameters

    override fun getName(): String = username

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
        private const val serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID
    }
}
