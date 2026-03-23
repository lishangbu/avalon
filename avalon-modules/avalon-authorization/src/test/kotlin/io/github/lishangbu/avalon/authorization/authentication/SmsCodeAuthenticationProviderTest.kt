package io.github.lishangbu.avalon.authorization.authentication

import io.github.lishangbu.avalon.authorization.service.VerificationCodeService
import io.github.lishangbu.avalon.oauth2.authorizationserver.authentication.SmsAuthenticationToken
import io.github.lishangbu.avalon.oauth2.authorizationserver.exception.InvalidCaptchaException
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService

class SmsCodeAuthenticationProviderTest {
    @Test
    fun trimsPhoneAndKeepsAuthenticationDetails() {
        val verificationCodeService = Mockito.mock(VerificationCodeService::class.java)
        val userDetailsService = Mockito.mock(UserDetailsService::class.java)
        val userDetails =
            User
                .withUsername("13800000000")
                .password("{noop}pwd")
                .authorities("ROLE_USER")
                .build()
        Mockito.`when`(userDetailsService.loadUserByUsername("13800000000")).thenReturn(userDetails)

        val provider = SmsCodeAuthenticationProvider(verificationCodeService, userDetailsService)
        val authentication = SmsAuthenticationToken(" 13800000000 ", " 123456 ")
        authentication.details = "request-details"

        val authenticated = provider.authenticate(authentication)

        Mockito.verify(verificationCodeService).verifyCode(
            "13800000000",
            "123456",
            AuthorizationGrantTypeSupport.SMS.value,
        )
        Mockito.verify(userDetailsService).loadUserByUsername("13800000000")
        assertSame("request-details", authenticated.details)
        assertSame(userDetails, authenticated.principal)
    }

    @Test
    fun rejectsBlankPhoneOrCode() {
        val verificationCodeService = Mockito.mock(VerificationCodeService::class.java)
        val userDetailsService = Mockito.mock(UserDetailsService::class.java)
        val provider = SmsCodeAuthenticationProvider(verificationCodeService, userDetailsService)

        val exception =
            assertThrows(InvalidCaptchaException::class.java) {
                provider.authenticate(SmsAuthenticationToken("13800000000", " "))
            }

        assertEquals("短信验证码不能为空", exception.message)
        Mockito.verifyNoInteractions(verificationCodeService, userDetailsService)
    }
}
