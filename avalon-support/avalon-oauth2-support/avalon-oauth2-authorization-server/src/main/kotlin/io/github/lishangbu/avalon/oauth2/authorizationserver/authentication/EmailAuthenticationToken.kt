package io.github.lishangbu.avalon.oauth2.authorizationserver.authentication

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority

/**
 * 邮箱验证码登录令牌
 *
 * 在认证前承载邮箱与验证码，在认证后承载已登录用户与权限
 */
class EmailAuthenticationToken : UsernamePasswordAuthenticationToken {
    /** 使用邮箱与验证码创建未认证令牌 */
    constructor(email: String, emailCode: String) : super(email, emailCode)

    /** 使用认证结果与权限创建已认证令牌 */
    constructor(
        principal: Any?,
        credentials: Any?,
        authorities: Collection<GrantedAuthority>,
    ) : super(principal ?: "", credentials ?: "", authorities)
}
