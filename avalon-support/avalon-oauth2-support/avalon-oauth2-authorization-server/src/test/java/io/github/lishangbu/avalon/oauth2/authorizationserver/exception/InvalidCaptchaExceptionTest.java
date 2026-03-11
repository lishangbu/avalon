package io.github.lishangbu.avalon.oauth2.authorizationserver.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InvalidCaptchaExceptionTest {

    @Test
    void exposesMessage() {
        InvalidCaptchaException exception = new InvalidCaptchaException("captcha");

        assertEquals("captcha", exception.getMessage());
    }
}
