package io.github.lishangbu.avalon.oauth2.common.log

import org.junit.jupiter.api.Test
import java.time.Instant

class AuthenticationLogRecorderTest {
    @Test
    fun noopRecorderAcceptsRecords() {
        val recorder = AuthenticationLogRecorder.noop()

        recorder.record(
            AuthenticationLogRecord(
                username = "alice",
                clientId = "client",
                grantType = "password",
                ip = "127.0.0.1",
                userAgent = "JUnit",
                success = true,
                errorMessage = null,
                timestamp = Instant.parse("2026-03-25T00:00:00Z"),
            ),
        )
    }
}
