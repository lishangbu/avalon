package io.github.lishangbu.avalon.oauth2.common.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.core.AuthorizationGrantType

class AuthorizationGrantTypeSupportTest {
    @Test
    fun passwordGrantTypeMatchesStandardValue() {
        val expected: AuthorizationGrantType = AuthorizationGrantType("password")

        assertEquals(expected, AuthorizationGrantTypeSupport.PASSWORD)
        assertEquals("password", AuthorizationGrantTypeSupport.PASSWORD.getValue())
    }

    @Test
    fun smsGrantTypeMatchesCustomValue() {
        val expected: AuthorizationGrantType = AuthorizationGrantType("sms")

        assertEquals(expected, AuthorizationGrantTypeSupport.SMS)
        assertEquals("sms", AuthorizationGrantTypeSupport.SMS.getValue())
    }

    @Test
    fun emailGrantTypeMatchesCustomValue() {
        val expected: AuthorizationGrantType = AuthorizationGrantType("email")

        assertEquals(expected, AuthorizationGrantTypeSupport.EMAIL)
        assertEquals("email", AuthorizationGrantTypeSupport.EMAIL.getValue())
    }
}
