package io.github.lishangbu.avalon.authorization.authentication

import io.github.lishangbu.avalon.authorization.service.VerificationCodeService
import io.github.lishangbu.avalon.oauth2.authorizationserver.authentication.EmailAuthenticationToken
import io.github.lishangbu.avalon.oauth2.authorizationserver.exception.InvalidCaptchaException
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import java.util.*

/**
 * 邮箱验证码认证提供者
 *
 * 使用 Redis 中的邮箱验证码进行认证
 *
 * @author lishangbu
 * @since 2026/3/13
 */
@Component
class EmailCodeAuthenticationProvider(
    /** 验证码服务 */
    private val verificationCodeService: VerificationCodeService,
    /** 用户详情服务 */
    private val userDetailsService: UserDetailsService,
) : AuthenticationProvider {
    /** 校验邮箱验证码并构造已认证令牌 */
    @Throws(AuthenticationException::class)
    override fun authenticate(authentication: Authentication): Authentication {
        val emailAuthenticationToken = authentication as EmailAuthenticationToken
        val normalizedEmail = normalizeEmail(emailAuthenticationToken.principal)
        val normalizedCode = resolveRequiredText(emailAuthenticationToken.credentials)
        verificationCodeService.verifyCode(
            normalizedEmail,
            normalizedCode,
            AuthorizationGrantTypeSupport.EMAIL.value,
        )
        val userDetails: UserDetails = userDetailsService.loadUserByUsername(normalizedEmail)
        val authenticated =
            EmailAuthenticationToken(userDetails, null, userDetails.authorities).also {
                it.details = authentication.details
            }
        return authenticated
    }

    /** 判断当前提供者是否支持邮箱验证码令牌 */
    override fun supports(authentication: Class<*>): Boolean = EmailAuthenticationToken::class.java.isAssignableFrom(authentication)

    /** 解析必填文本 */
    private fun resolveRequiredText(value: Any?): String =
        value
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: throw InvalidCaptchaException("邮箱验证码不能为空")

    /** 规范化邮箱 */
    private fun normalizeEmail(email: Any?): String = resolveRequiredText(email).lowercase(Locale.ROOT)
}
