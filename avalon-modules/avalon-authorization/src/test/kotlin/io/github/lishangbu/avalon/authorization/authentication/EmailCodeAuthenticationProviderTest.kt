package io.github.lishangbu.avalon.authorization.authentication

import io.github.lishangbu.avalon.authorization.service.VerificationCodeService
import io.github.lishangbu.avalon.oauth2.authorizationserver.authentication.EmailAuthenticationToken
import io.github.lishangbu.avalon.oauth2.authorizationserver.exception.InvalidCaptchaException
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService

class EmailCodeAuthenticationProviderTest {
    @Test
    fun normalizesEmailAndKeepsAuthenticationDetails() {
        val verificationCodeService = Mockito.mock(VerificationCodeService::class.java)
        val userDetailsService = Mockito.mock(UserDetailsService::class.java)
        val userDetails =
            User
                .withUsername("user@example.com")
                .password("{noop}pwd")
                .authorities("ROLE_USER")
                .build()
        Mockito.`when`(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails)

        val provider = EmailCodeAuthenticationProvider(verificationCodeService, userDetailsService)
        val authentication = EmailAuthenticationToken("  User@Example.com  ", " 123456 ")
        authentication.details = "request-details"

        val authenticated = provider.authenticate(authentication)

        Mockito.verify(verificationCodeService).verifyCode(
            "user@example.com",
            "123456",
            AuthorizationGrantTypeSupport.EMAIL.value,
        )
        Mockito.verify(userDetailsService).loadUserByUsername("user@example.com")
        assertSame("request-details", authenticated.details)
        assertSame(userDetails, authenticated.principal)
    }

    @Test
    fun rejectsBlankEmailOrCode() {
        val verificationCodeService = Mockito.mock(VerificationCodeService::class.java)
        val userDetailsService = Mockito.mock(UserDetailsService::class.java)
        val provider = EmailCodeAuthenticationProvider(verificationCodeService, userDetailsService)

        val exception =
            assertThrows(InvalidCaptchaException::class.java) {
                provider.authenticate(EmailAuthenticationToken(" ", "123456"))
            }

        assertEquals("邮箱验证码不能为空", exception.message)
        Mockito.verifyNoInteractions(verificationCodeService, userDetailsService)
    }
}
