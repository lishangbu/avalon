package io.github.lishangbu.avalon.oauth2.authorizationserver.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InvalidCaptchaExceptionTest {
    @Test
    fun exposesMessage() {
        val exception = InvalidCaptchaException("captcha")

        assertEquals("captcha", exception.message)
    }
}
