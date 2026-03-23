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
    private val verificationCodeService: VerificationCodeService,
    private val userDetailsService: UserDetailsService,
) : AuthenticationProvider {
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

    override fun supports(authentication: Class<*>): Boolean = EmailAuthenticationToken::class.java.isAssignableFrom(authentication)

    private fun resolveRequiredText(value: Any?): String =
        value
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: throw InvalidCaptchaException("邮箱验证码不能为空")

    private fun normalizeEmail(email: Any?): String = resolveRequiredText(email).lowercase(Locale.ROOT)
}
