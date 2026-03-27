package io.github.lishangbu.avalon.oauth2.authorizationserver.keygen

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UuidKeyGeneratorTest {
    @Test
    fun generatesLowercaseUuid() {
        val generator = UuidKeyGenerator()

        val key = generator.generateKey()

        assertNotNull(key)
        assertEquals(key.lowercase(), key)
        assertTrue(key.contains("-"))
    }
}
