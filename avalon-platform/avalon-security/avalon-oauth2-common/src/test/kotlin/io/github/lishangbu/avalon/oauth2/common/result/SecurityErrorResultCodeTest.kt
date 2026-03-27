package io.github.lishangbu.avalon.oauth2.common.result

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SecurityErrorResultCodeTest {
    @Test
    fun unauthorizedCodeAndMessageMatchDefinition() {
        assertEquals(401, SecurityErrorResultCode.UNAUTHORIZED.code())
        assertEquals("Unauthorized", SecurityErrorResultCode.UNAUTHORIZED.errorMessage())
    }

    @Test
    fun forbiddenCodeAndMessageMatchDefinition() {
        assertEquals(403, SecurityErrorResultCode.FORBIDDEN.code())
        assertEquals("Forbidden", SecurityErrorResultCode.FORBIDDEN.errorMessage())
    }
}
