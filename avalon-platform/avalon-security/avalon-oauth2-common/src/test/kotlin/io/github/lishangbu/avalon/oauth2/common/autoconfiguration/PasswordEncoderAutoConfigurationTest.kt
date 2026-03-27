package io.github.lishangbu.avalon.oauth2.common.autoconfiguration

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PasswordEncoderAutoConfigurationTest {
    @Test
    fun createsDelegatingPasswordEncoder() {
        val configuration = PasswordEncoderAutoConfiguration()

        val encoder = configuration.passwordEncoder()

        assertNotNull(encoder)
        val encoded = encoder.encode("secret")
        assertTrue(encoder.matches("secret", encoded))
    }
}
