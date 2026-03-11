package io.github.lishangbu.avalon.oauth2.common.log;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuthenticationLogTests {

    @Test
    void recordStoresProvidedValues() {
        Instant now = Instant.now();
        AuthenticationLogRecord record =
                new AuthenticationLogRecord("alice", "client", "password", "127.0.0.1", "UA", true, "none", now);

        assertEquals("alice", record.username());
        assertEquals("client", record.clientId());
        assertEquals("password", record.grantType());
        assertEquals("127.0.0.1", record.ip());
        assertEquals("UA", record.userAgent());
        assertEquals(true, record.success());
        assertEquals("none", record.errorMessage());
        assertEquals(now, record.timestamp());
    }

    @Test
    void recordSupportsNullsAndFalseFlag() {
        AuthenticationLogRecord record =
                new AuthenticationLogRecord(null, null, null, null, null, false, null, null);

        assertNull(record.username());
        assertNull(record.clientId());
        assertNull(record.grantType());
        assertNull(record.ip());
        assertNull(record.userAgent());
        assertFalse(record.success());
        assertNull(record.errorMessage());
        assertNull(record.timestamp());
    }

    @Test
    void noopRecorderAcceptsRecords() {
        AuthenticationLogRecorder recorder = AuthenticationLogRecorder.noop();

        assertDoesNotThrow(() -> recorder.record(new AuthenticationLogRecord(null, null, null, null, null, false, null, null)));
    }
}
