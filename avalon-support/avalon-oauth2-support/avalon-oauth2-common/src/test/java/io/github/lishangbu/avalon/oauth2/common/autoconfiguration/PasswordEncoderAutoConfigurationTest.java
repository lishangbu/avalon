package io.github.lishangbu.avalon.oauth2.common.autoconfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderAutoConfigurationTest {

    @Test
    void createsDelegatingPasswordEncoder() {
        PasswordEncoderAutoConfiguration configuration = new PasswordEncoderAutoConfiguration();

        PasswordEncoder encoder = configuration.passwordEncoder();

        assertNotNull(encoder);
        String encoded = encoder.encode("secret");
        assertTrue(encoder.matches("secret", encoded));
    }
}
