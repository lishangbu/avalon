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
import org.springframework.util.StringUtils

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
    private val verificationCodeService: VerificationCodeService,
    private val userDetailsService: UserDetailsService,
) : AuthenticationProvider {
    @Throws(AuthenticationException::class)
    override fun authenticate(authentication: Authentication): Authentication {
        val smsAuthenticationToken = authentication as SmsAuthenticationToken
        val phone = normalizePhone(resolveText(smsAuthenticationToken.principal))
        val code = resolveText(smsAuthenticationToken.credentials)
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(code)) {
            throw InvalidCaptchaException("短信验证码不能为空")
        }
        val normalizedPhone = phone!!
        val normalizedCode = code!!
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

    override fun supports(authentication: Class<*>): Boolean = SmsAuthenticationToken::class.java.isAssignableFrom(authentication)

    private fun resolveText(value: Any?): String? = value?.toString()

    private fun normalizePhone(phone: String?): String? {
        if (!StringUtils.hasText(phone)) {
            return phone
        }
        return phone!!.trim { it <= ' ' }
    }
}
