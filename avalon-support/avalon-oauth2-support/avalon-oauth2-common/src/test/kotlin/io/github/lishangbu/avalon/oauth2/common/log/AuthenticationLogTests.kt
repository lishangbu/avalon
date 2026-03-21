package io.github.lishangbu.avalon.oauth2.common.log

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class AuthenticationLogTests {
    @Test
    fun recordStoresProvidedValues() {
        val now = Instant.now()
        val record =
            AuthenticationLogRecord(
                "alice",
                "client",
                "password",
                "127.0.0.1",
                "UA",
                true,
                "none",
                now,
            )

        assertEquals("alice", record.username)
        assertEquals("client", record.clientId)
        assertEquals("password", record.grantType)
        assertEquals("127.0.0.1", record.ip)
        assertEquals("UA", record.userAgent)
        assertEquals(true, record.success)
        assertEquals("none", record.errorMessage)
        assertEquals(now, record.timestamp)
    }

    @Test
    fun recordSupportsNullsAndFalseFlag() {
        val record = AuthenticationLogRecord(null, null, null, null, null, false, null, null)

        assertNull(record.username)
        assertNull(record.clientId)
        assertNull(record.grantType)
        assertNull(record.ip)
        assertNull(record.userAgent)
        assertFalse(record.success)
        assertNull(record.errorMessage)
        assertNull(record.timestamp)
    }

    @Test
    fun noopRecorderAcceptsRecords() {
        val recorder = AuthenticationLogRecorder.noop()

        assertDoesNotThrow {
            recorder.record(
                AuthenticationLogRecord(null, null, null, null, null, false, null, null),
            )
        }
    }
}
