package io.github.lishangbu.avalon.oauth2.authorizationserver.authentication

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority

/**
 * 短信验证码登录的认证令牌 用于承载手机号与短信验证码，并与 AuthenticationManager 协作完成认证
 *
 * @author lishangbu
 * @since 2026/3/13
 */
class SmsAuthenticationToken : UsernamePasswordAuthenticationToken {
    constructor(phoneNumber: String, smsCode: String) : super(phoneNumber, smsCode)

    constructor(
        principal: Any?,
        credentials: Any?,
        authorities: Collection<GrantedAuthority>,
    ) : super(principal ?: "", credentials ?: "", authorities)
}
