package io.github.lishangbu.avalon.oauth2.authorizationserver.keygen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UuidKeyGeneratorTest {

    @Test
    void generatesLowercaseUuid() {
        UuidKeyGenerator generator = new UuidKeyGenerator();

        String key = generator.generateKey();

        assertNotNull(key);
        assertEquals(key.toLowerCase(), key);
        assertTrue(key.contains("-"));
    }
}
