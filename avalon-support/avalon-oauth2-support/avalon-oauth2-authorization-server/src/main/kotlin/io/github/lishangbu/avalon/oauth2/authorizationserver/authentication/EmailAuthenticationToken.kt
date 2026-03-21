package io.github.lishangbu.avalon.oauth2.authorizationserver.authentication

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority

/**
 * 邮箱验证码登录的认证令牌 用于承载邮箱与验证码，并与 AuthenticationManager 协作完成认证
 *
 * @author lishangbu
 * @since 2026/3/13
 */
class EmailAuthenticationToken : UsernamePasswordAuthenticationToken {
    constructor(email: String, emailCode: String) : super(email, emailCode)

    constructor(
        principal: Any?,
        credentials: Any?,
        authorities: Collection<GrantedAuthority>,
    ) : super(principal ?: "", credentials ?: "", authorities)
}
