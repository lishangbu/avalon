package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.repository.AuthenticationLogRepository
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import java.time.Instant

class DefaultAuthenticationLogRecorderTest {
    private val repository = mock(AuthenticationLogRepository::class.java)
    private val recorder = DefaultAuthenticationLogRecorder(repository)

    @Test
    fun recordUsesProvidedTimestampWhenAvailable() {
        var recorded: io.github.lishangbu.avalon.authorization.entity.AuthenticationLog? = null
        Mockito.doAnswer {
            recorded = it.getArgument(0)
            recorded
        }.`when`(repository).save(any())
        val timestamp = Instant.parse("2026-03-25T01:02:03Z")
        recorder.record(
            AuthenticationLogRecord(
                username = "alice",
                clientId = "client",
                grantType = "password",
                ip = "127.0.0.1",
                userAgent = "JUnit",
                success = true,
                errorMessage = null,
                timestamp = timestamp,
            ),
        )

        val log = requireNotNull(recorded)
        assertEquals("alice", log.username)
        assertEquals("client", log.clientId)
        assertEquals("password", log.grantType)
        assertEquals("127.0.0.1", log.ip)
        assertEquals("JUnit", log.userAgent)
        assertEquals(true, log.success)
        assertEquals(timestamp, log.occurredAt)
    }

    @Test
    fun recordFallsBackToCurrentTimeWhenTimestampIsMissing() {
        var recorded: io.github.lishangbu.avalon.authorization.entity.AuthenticationLog? = null
        Mockito.doAnswer {
            recorded = it.getArgument(0)
            recorded
        }.`when`(repository).save(any())
        val before = Instant.now()

        recorder.record(
            AuthenticationLogRecord(
                username = "bob",
                clientId = null,
                grantType = null,
                ip = null,
                userAgent = null,
                success = false,
                errorMessage = "bad credentials",
                timestamp = null,
            ),
        )

        val after = Instant.now()
        val log = requireNotNull(recorded)
        assertEquals("bob", log.username)
        assertEquals(false, log.success)
        assertEquals("bad credentials", log.errorMessage)
        assertNotNull(log.occurredAt)
        assertTrue(!log.occurredAt!!.isBefore(before))
        assertTrue(!log.occurredAt!!.isAfter(after))
    }
}
