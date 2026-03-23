package io.github.lishangbu.avalon.authorization.authentication

import io.github.lishangbu.avalon.authorization.service.VerificationCodeService
import io.github.lishangbu.avalon.oauth2.authorizationserver.authentication.SmsAuthenticationToken
import io.github.lishangbu.avalon.oauth2.authorizationserver.exception.InvalidCaptchaException
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component

/**
 * 短信验证码认证提供者
 *
 * 使用 Redis 中的短信验证码进行认证
 *
 * @author lishangbu
 * @since 2026/3/13
 */
@Component
class SmsCodeAuthenticationProvider(
    /** 验证码服务 */
    private val verificationCodeService: VerificationCodeService,
    /** 用户详情服务 */
    private val userDetailsService: UserDetailsService,
) : AuthenticationProvider {
    /** 校验短信验证码并构造已认证令牌 */
    @Throws(AuthenticationException::class)
    override fun authenticate(authentication: Authentication): Authentication {
        val smsAuthenticationToken = authentication as SmsAuthenticationToken
        val normalizedPhone = normalizePhone(smsAuthenticationToken.principal)
        val normalizedCode = resolveRequiredText(smsAuthenticationToken.credentials)
        verificationCodeService.verifyCode(
            normalizedPhone,
            normalizedCode,
            AuthorizationGrantTypeSupport.SMS.value,
        )
        val userDetails: UserDetails = userDetailsService.loadUserByUsername(normalizedPhone)
        val authenticated =
            SmsAuthenticationToken(userDetails, null, userDetails.authorities).also {
                it.details = authentication.details
            }
        return authenticated
    }

    /** 判断当前提供者是否支持短信验证码令牌 */
    override fun supports(authentication: Class<*>): Boolean = SmsAuthenticationToken::class.java.isAssignableFrom(authentication)

    /** 解析必填文本 */
    private fun resolveRequiredText(value: Any?): String =
        value
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: throw InvalidCaptchaException("短信验证码不能为空")

    /** 规范化手机 */
    private fun normalizePhone(phone: Any?): String = resolveRequiredText(phone)
}
