package io.github.lishangbu.avalon.oauth2.authorizationserver.authentication

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority

/**
 * 短信验证码登录令牌
 *
 * 在认证前承载手机号与验证码，在认证后承载已登录用户与权限
 */
class SmsAuthenticationToken : UsernamePasswordAuthenticationToken {
    /** 使用手机号与验证码创建未认证令牌 */
    constructor(phoneNumber: String, smsCode: String) : super(phoneNumber, smsCode)

    /** 使用认证结果与权限创建已认证令牌 */
    constructor(
        principal: Any?,
        credentials: Any?,
        authorities: Collection<GrantedAuthority>,
    ) : super(principal ?: "", credentials ?: "", authorities)
}
